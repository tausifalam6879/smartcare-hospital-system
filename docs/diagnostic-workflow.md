# Diagnostic Workflow Design

## Delivered Phase 8 boundary

RaahMediQ Health coordinates clinician-ordered laboratory and imaging work. It does not let a patient self-prescribe a test, interpret values as a diagnosis, or release a draft result as final.

## State flow

```text
ORDERED -> SCHEDULED -> SAMPLE_COLLECTED -> IN_PROGRESS -> RESULT_VERIFIED
    |           |
    +-----------+---------------------------------------> CANCELLED
```

Imaging staff may move directly from `SCHEDULED` to `IN_PROGRESS`; laboratory workflows normally record collection first. Verification is allowed only after collection or processing has started. A verified result is immutable in this phase; production corrections require a signed amendment rather than an overwrite.

## Ordering and ownership

- A linked active doctor can create an order only for their assigned appointment.
- The appointment must be in consultation or completed.
- The procedure and appointment must belong to the same hospital.
- A patient query always starts from the authenticated patient's database ID.
- Scheduling and cancellation resolve the order with a pessimistic lock and then re-check patient ownership.
- Worklist and verification endpoints require `LAB_TECHNICIAN`, `HOSPITAL_ADMIN`, or `SUPER_ADMIN`. Production additionally needs hospital-scoped staff assignments and MFA.

## Capacity correctness

Each procedure/date has a locked day ledger with effective capacity, active count, and a monotonic next position. Scheduling locks the procedure row before first-ledger creation and locks an existing ledger before allocation. The database also rejects duplicate `(procedure_id, scheduled_date, queue_position)` values. A cancellable scheduled order releases one active capacity unit; issued position numbers are never silently reused.

## Result integrity

Verification stores one immutable result header per order plus ordered structured items. Each result includes the verifier, verification timestamp, summary, optional findings/impression, reference ranges, and explicit normal/abnormal/critical/indeterminate flags. Patient APIs return result content only from this verified table. Audit events identify the resource and action but never copy result values or narrative text.

## Deferred production work

- Hospital-scoped staff assignment and privileged-user MFA.
- Diagnostic payment, insurer authorization, refund, and cancellation policy integration.
- Instrument/LIS/RIS/PACS adapters, DICOM workflows, electronic signatures, and signed amendments.
- Verified report-file publication through managed private object storage.
- Critical-result acknowledgement/escalation workflow and jurisdiction-specific retention.
