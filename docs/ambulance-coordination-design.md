# Phase 10 Ambulance Coordination

## Safety boundary

SmartCare records and coordinates hospital transport requests. It is not an official emergency number, a clinical-triage tool, a guaranteed vehicle service, or a production computer-aided dispatch system. A patient submission remains `REQUESTED` until an authorized dispatcher explicitly assigns an available ambulance. The AI assistant does not select vehicles, prioritize dispatches, alter operational stages, or tell a patient that an emergency can wait.

The development fleet is marked `synthetic=true` in the database and UI. It has no callable crew contact and must never be presented as real availability.

## Request workflow

```text
REQUESTED
  -> ASSIGNED
  -> ACKNOWLEDGED
  -> EN_ROUTE_TO_PATIENT
  -> PATIENT_PICKED_UP
  -> EN_ROUTE_TO_HOSPITAL
  -> ARRIVED
  -> COMPLETED
```

Cancellation is allowed for the patient only while `REQUESTED`. Authorized dispatch staff may cancel before pickup. Once pickup is recorded, operational staff must finish the managed handoff rather than using the ordinary cancellation endpoint.

Vehicle status follows the active request but has no `COMPLETED` value. Completion or an allowed cancellation releases the vehicle back to `AVAILABLE`. Only an idle available vehicle may move to `OUT_OF_SERVICE`, and only an out-of-service vehicle may return to service.

## Consistency and authorization

- Creation is idempotent per requester and idempotency key.
- Assignment pessimistically locks both the request and ambulance rows in one transaction.
- The vehicle must be active, `AVAILABLE`, and owned by the request hospital.
- A second assignment cannot reserve the same ambulance.
- Every next stage is validated against one strict predecessor; stages cannot be skipped.
- Patient reads derive the patient from the JWT and query only that patient's rows.
- Fleet, crew contact, exact coordinates, worklists, assignment, acknowledgement, and stage updates require dispatcher/admin roles.
- Patient views expose vehicle identity, the latest coarse area label, and its timestamp but not coordinates or crew contact.
- Request, assignment, acknowledgement, stage, cancellation, fleet-availability, and location actions emit non-content audit events.

## Event timeline and notifications

`ambulance_request_events` is an append-only operational timeline with from/to state, actor, timestamp, and a short patient-visible note. Durable in-app notifications are deduplicated for request creation and every dispatch stage. Development external-channel adapters continue to report `SKIPPED_NOT_CONFIGURED`; they never claim that SMS, WhatsApp, push, or calls succeeded.

## Production work still required

Before real deployment, add hospital-scoped dispatcher grants, MFA, official emergency governance, accredited dispatch-provider integration, driver/crew identity and device enrollment, signed GPS telemetry, stale-location policy, map/ETA provider isolation, offline acknowledgement recovery, escalation SLAs, protected radio/contact routing, retention rules, regional consent/legal review, and disaster-recovery exercises. Exact GPS must remain restricted to authorized operations and must not appear in general patient APIs or logs.
