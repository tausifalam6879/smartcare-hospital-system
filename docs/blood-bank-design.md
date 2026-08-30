# Phase 9 Blood-Bank Workflow

## Safety boundary

RaahMediQ Health coordinates verified operational information; it does not decide transfusion compatibility, cross-match blood, approve donation, or administer a component. Search uses the exact clinician-recorded ABO/Rh group and component. Qualified blood-bank and clinical staff remain responsible for confirmation and release.

An emergency must not wait for a website lookup. The patient UI explicitly directs users to local emergency services or the hospital emergency desk and warns them to reconfirm availability before travelling.

## Inventory trust

Each inventory row represents a batch at one hospital-authorized internal or partner blood bank. It records total and reserved units, expiry, verification state, verification time, and verifier. A configurable `RAAHMEDIQ_BLOOD_VERIFICATION_MAX_AGE` defaults to six hours.

A batch is searchable only when all conditions are true:

1. blood bank is active and authorized;
2. group and component exactly match;
3. status is `VERIFIED`;
4. verification is inside the freshness window;
5. expiry is today or later; and
6. `total_units - reserved_units > 0`.

The patient search groups fresh batches by blood bank and returns `AVAILABLE`, `LIMITED`, `UNAVAILABLE`, or `STALE_OR_UNVERIFIED` with the last verification time. It omits batch references and staff identities.

## Request state machine

```text
SEARCHING
   | enough fresh stock       | some stock          | no stock
   v                          v                     v
RESERVED               PARTIALLY_RESERVED      UNAVAILABLE
   | staff fulfils            | repeat search       | repeat search
   v                          +-------> RESERVED <---+
FULFILLED

SEARCHING / PARTIALLY_RESERVED / RESERVED / UNAVAILABLE
   -> CANCELLED (authorized clinician or blood-bank staff)
```

Only a linked doctor, blood-bank staff member, or administrator can create a request. Doctor requests are restricted to the doctor’s hospital. An appointment is optional because an emergency workflow must not depend on a normal OPD booking; when supplied, patient/hospital/doctor relationships are validated.

The creator supplies an idempotency key. Repeating the same command returns the original request instead of allocating twice. Patients query only their own requests and cannot self-issue or fulfil one.

## Concurrency and allocation

Eligible inventory rows are selected with pessimistic write locks and ordered by bank distance then earliest expiry. The transaction reserves as many units as possible across batches, updates request matched units, writes allocation rows, and creates an audit event. Database checks reject oversubscription even if application validation regresses.

Fulfilment reduces both reserved and total batch units. Cancellation releases reserved units and marks allocations released. Both operations lock the request and every affected batch in the same transaction.

## Donor privacy

Donor participation is optional and explicit. Consent initially has `PENDING_VERIFICATION`; it contains no self-confirmed blood-group claim. Authorized staff separately record a verified group and `ELIGIBLE` or `TEMPORARILY_INELIGIBLE` decision. A user may withdraw consent at any time.

Private donor matches are available only to blood-bank/admin roles, only for exact verified group, only for `ELIGIBLE` active consent, and only after verified inventory is unavailable or partial. Phase 9 records and audits matching access; production donor-contact dispatch and medical eligibility screening remain external authorized workflows.

## Deferred production work

- hospital-scoped staff assignments and privileged-role MFA;
- accredited blood-bank/LIS integration adapters and signed source events;
- audited private donor-contact delivery and acknowledgement;
- cross-facility transfer acceptance and chain-of-custody;
- compatibility/cross-match systems managed by qualified professionals;
- real-time expiry alerts, recall/quarantine workflow, and regulatory reporting.
