# SmartCare Architecture

## 1. Architectural style

SmartCare starts as a **modular Spring Boot monolith**. A single deployable process owns one PostgreSQL database, while Java packages enforce domain boundaries. This keeps cross-module transactions reliable during the early hospital rollout and avoids distributed failure modes before operational scale requires them.

The application uses a ports-and-adapters shape inside each domain:

```text
HTTP / WebSocket / scheduled job
              |
       application service
              |
 domain model + policy + state machine
              |
 repository / gateway interfaces
              |
 PostgreSQL, Redis, object store, providers
```

Controllers validate transport concerns and map DTOs. Application services define transaction boundaries. Domain objects enforce invariants. Repositories persist state. Provider adapters isolate payment, SMS, WhatsApp, email, AI, object storage, maps, and blood-bank integrations.

PostgreSQL is the correctness boundary. Redis may accelerate caches, short-lived offers, rate limits, and distributed coordination, but a Redis outage must not permit oversubscription or corrupt the queue.

## 2. Modules and responsibilities

| Module | Responsibility | Owns |
|---|---|---|
| `auth` | Registration, login, JWT issuance, credentials, RBAC | user, roles, sessions/revocation later |
| `patient` | Demographics, consent, emergency contacts, language | patient profile |
| `hospital` | Hospital/building/floor/room/department directory | facility graph |
| `doctor` | Doctor profile, capacity, schedules, leave | doctor availability |
| `appointment` | Reservation lifecycle and state machine | appointment |
| `queue` | Capacity ledger, positions, waitlist, rollover, live queue | queue entry, waitlist entry |
| `payment` | Online/cash intent, webhook idempotency, refunds | payment, refund |
| `checkin` | QR/kiosk/reception check-in and no-show rules | check-in |
| `notification` | Event-driven channel adapters and delivery attempts | notification |
| `navigation` | Structured route graph and QR checkpoints | location/checkpoint/route |
| `medicalrecord` | Longitudinal clinical data and secured documents | visits, prescriptions, records |
| `diagnostic` | Lab/imaging orders, capacity, reports | diagnostic order, report |
| `bloodbank` | Verified inventory, urgent requests, opted-in donors | inventory, request, consent |
| `bloodgroupai` | Experimental slide-image review with human verification | image analysis, observation |
| `ambulance` | Authorized dispatch workflow | vehicle, dispatch |
| `operations` | Doctor-day state, disruption recovery, staff workboard | operation, recovery case |
| `ai` | Grounded hospital assistant and patient-isolated RAG | conversation, chunk metadata |
| `audit` | Append-only sensitive-operation trail | audit log |
| `admin` | Policy and operational controls | hospital-scoped settings |
| `common` | IDs, errors, correlation, time, outbox primitives | shared infrastructure only |

Modules communicate through explicit application interfaces and domain events. They do not reach into another module's repository. Events that cause external work are recorded to a transactional outbox in the same database transaction, then delivered asynchronously with retry and deduplication.

## 3. Runtime topology

```text
Mobile browser / kiosk / staff browser
                 |
          React + Tailwind SPA
                 |
        HTTPS REST + SSE/WebSocket
                 |
       Spring Boot modular monolith
       /        |        |       \
PostgreSQL    Redis   Object     Provider adapters
 (truth)     (assist) storage    payment/SMS/AI
```

The AI subsystem is never on the critical path for booking, payment, check-in, clinical records, emergency intake, or dispatch.

## 4. Security and tenancy

- JWT authentication uses short-lived access tokens. Refresh-token rotation and revocation are a later auth increment.
- Every staff action is authorized by both role and hospital scope. Role checks alone are insufficient.
- Patient record access requires subject ownership or an explicit staff care relationship and permission.
- Public identifiers are UUIDs; sequential database identifiers are never exposed.
- Sensitive reads and writes emit immutable audit records with actor, action, resource, hospital, timestamp, correlation ID, and outcome.
- The delivered development adapter keeps medical files outside the web root and streams them only through an authorized endpoint. Production replaces that port with managed private object storage and short-lived access; permanent public URLs are forbidden.
- Logs exclude passwords, tokens, report content, diagnoses, complete payment data, and uploaded documents.

## 5. Consistency patterns

- Doctor-day capacity is represented by a locked aggregate/ledger row.
- A unique constraint prevents two active queue entries from sharing `(doctor_id, service_date, position)`.
- Booking, cancellation, capacity release, and waitlist promotion occur in one database transaction.
- Payment webhooks have a provider-event uniqueness constraint and idempotent state transition.
- Commands accept idempotency keys for retry-prone clients.
- Optimistic versions detect concurrent administrative edits; pessimistic row locks protect the final slot and promotion sequence.
- Notification records and delivery attempts are committed durably; a future production transport can move dispatch to a transactional outbox worker.

## 6. Delivery phases

Phase 1 implements authentication, roles, hospital, department, doctor, schedules, and the responsive React/Tailwind shell. Phase 2 adds the transactional appointment/capacity ledger, pending-payment states, FIFO waitlist, cancellation/promotion, expiry worker, and patient booking history. Phase 3 adds payment/refund ledgers, signed and idempotent provider webhooks, cash receipts, cancellation cutoffs, and refund requests. Phase 4 adds check-in, privacy-safe live snapshots, staff-authorized queue advancement, durable in-app notifications, and explicit external-channel delivery status. Phase 5 adds hospital-scoped indoor graphs, public QR checkpoints, deterministic bilingual routing, step-free filtering, and ownership-protected appointment destinations. Phase 6 adds linked clinician identities, appointment-bound append-only visits, prescriptions, allergies, private report metadata/storage, object authorization, and audited patient record access. Phase 7 adds patient-owned assistant conversations, PDF extraction, citation-aware chunks, local embeddings, database-enforced patient filtering before ranking, grounded responses, citations, rate limiting, and medical-safety redirects. Phase 8 adds a diagnostic catalogue, appointment-bound clinician orders, locked procedure-day capacity ledgers, patient scheduling, role-protected processing states, and structured staff-verified results. Phase 9 adds recently verified batch inventory, exact group/component search, locked FEFO reservations, clinician/staff requests, patient-owned status, and consent-gated donor matching. Phase 10 adds idempotent transport requests, dispatcher-only locked vehicle assignment, crew acknowledgement, strict operational transitions, coarse patient location sharing, append-only timelines, and synthetic demo fleet boundaries. Phase 11 adds doctor-day operational state, delay-aware live estimates, role-aware hospital workboards, controlled no-show release, and patient-approved appointment recovery with locked capacity reallocation. Phase 12 adds private blood-slide image submission, an unavailable-by-default analyzer port, human reaction recording, and independent laboratory verification. Route calculation, transfusion compatibility, triage, automatic ambulance dispatch, silent appointment transfer, automatic refund success, and unvalidated blood-group prediction remain outside AI; the assistant never creates diagnoses, prescriptions, dispatch decisions, or operational approvals.

Every phase must retain: a compilable Java 17 build, valid migrations, passing tests, explicit unfinished-feature labeling, and an updated README.
