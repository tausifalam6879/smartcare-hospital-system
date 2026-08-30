# Appointment, Queue, Payment, and Recovery Design

## Appointment state machine

```text
RESERVED_PENDING_PAYMENT --verified online payment--> CONFIRMED
RESERVED_PENDING_PAYMENT --reservation timeout-----> EXPIRED
RESERVED_PENDING_PAYMENT --cash selected-----------> CASH_PENDING
CASH_PENDING ------------cashier confirms----------> CONFIRMED
CASH_PENDING ------------deadline------------------> EXPIRED
CONFIRMED ---------------check-in-------------------> CHECKED_IN
CONFIRMED ---------------cancel---------------------> CANCELLED
CONFIRMED ---------------reschedule-----------------> RESCHEDULED
CONFIRMED ---------------grace expired--------------> NO_SHOW
CHECKED_IN --------------staff makes ready----------> READY_FOR_CONSULTATION
READY_FOR_CONSULTATION --doctor starts--------------> IN_CONSULTATION
IN_CONSULTATION ---------doctor completes-----------> COMPLETED
eligible unfinished -----rollover policy------------> ROLLED_OVER
```

Only named domain commands can transition state. The entity rejects all other transitions, including backwards moves and direct controller updates. Each transition records actor, reason, previous/new state, correlation ID, and time.

## Booking the last position safely

1. Start a database transaction.
2. Lock the doctor-day capacity ledger (`SELECT ... FOR UPDATE`).
3. Re-evaluate schedule, leave, capacity, and active reservations.
4. If capacity exists, allocate the next position and insert an expiring reservation. The unique doctor/date/position constraint is the final guard.
5. Otherwise append a waitlist entry ordered by eligibility priority, then request time, then immutable tie-breaker.
6. Commit the reservation and an outbox notification atomically.

Two concurrent callers serialize on the ledger. Only one can observe and claim the final capacity unit.

## The 60-patient example

For capacity 60, positions 1–60 form the confirmed inventory for that doctor-day. A payment-pending hold counts against capacity only until its short expiry. Patient 60 and patient 61 arriving concurrently cannot both be confirmed: one gets the final position and the other is waitlisted. `completed = 52` does not mean positions 53–60 may overwrite tomorrow; those eight appointments remain Day-1 history and enter a separate rollover eligibility process.

## Fair waitlist promotion

Promotion runs inside the same transaction that releases capacity, or as an idempotent command triggered from its outbox event:

1. Lock the doctor-day ledger.
2. Select the first eligible waitlist row with `FOR UPDATE SKIP LOCKED` using hospital-configured priority and FIFO tie-breaking.
3. Create a time-limited offer or directly allocate according to hospital policy.
4. Mark the waitlist row promoted and link its resulting appointment.
5. Record an outbox notification.

No patient is silently removed. Expired offers resume promotion. Policy changes are versioned so later audits can explain the ordering.

## Next-day rollover

At day close, unfinished eligible appointments are placed into a rollover queue with the original appointment and policy version. For each future eligible service date:

1. Protect already confirmed future appointments.
2. Calculate only genuinely unallocated capacity.
3. Lock the future doctor-day ledger.
4. Merge eligible rollovers using hospital policy (for example, emergency-approved priority then oldest original service date), never by overwriting a position.
5. Mark the original appointment `ROLLED_OVER`, create/link the proposed future booking, and notify the patient.
6. If patient approval is required, reserve an expiring offer; rejection leaves the original history intact.
7. If full, retain the entry for the next eligible day and notify the patient of the delay.

## Online payment

The server creates a payment intent linked to exactly one reservation. A browser redirect is never proof of payment. The provider webhook is authenticated, stored under a unique provider event ID, and processed transactionally. Replays return the prior result. A successful amount/currency/appointment match transitions the reservation to `CONFIRMED`; mismatch enters manual review. Commit also writes `PaymentCompleted` and `AppointmentConfirmed` outbox events.

If the provider succeeds while the application is down, webhook retries or reconciliation recover the payment. If confirmation misses the hold deadline, reconciliation locks both records and follows policy: restore capacity if safe, otherwise refund/manual review. It never creates a duplicate appointment.

## Cash payment

Selecting cash creates `CASH_PENDING` with a configurable deadline. A cashier with hospital scope confirms receipt once using an idempotency key, producing a receipt and `CONFIRMED` state. A scheduled expiry worker claims due rows in small locked batches. Expiration releases capacity and promotes the waitlist in the same transaction. A late payment cannot revive an expired appointment; staff must create a new authorized booking/refund workflow.

## Phase 4 live queue and privacy

Check-in changes only a confirmed appointment to `CHECKED_IN` and records its source and verifier. Staff queue advancement serializes on the doctor row, completes any current consultation, and selects the lowest checked-in queue position. Patient snapshots count eligible positions ahead and multiply by the doctor's expected consultation duration; this value is always labelled approximate. Public snapshots expose only the OPD number or random check-in token and never patient names.

The React client refreshes every 15 seconds as a reliable fallback. A later SSE/WebSocket transport can reduce latency without changing the database-authoritative snapshot API.

## Failure recovery

| Failure | Recovery |
|---|---|
| Crash after DB commit | Outbox worker resumes notification/provider delivery |
| Duplicate payment webhook | Unique provider event + idempotent handler returns saved result |
| Notification provider down | Exponential retry, dead-letter state, admin alert; booking remains valid |
| Redis down | Database locking remains authoritative; cached UX degrades |
| AI down | Directory/queue/medical workflows continue; assistant reports unavailable |
| Doctor availability changes mid-booking | Locked ledger and version check reject stale reservation |
| Worker runs twice | Claim rows with locks and idempotent transition guards |
| Partial rollover batch | Per-entry transactions plus resumable batch cursor |

Reconciliation jobs compare reservations, payments, capacity ledgers, outbox delivery, and provider settlement without logging sensitive content.
