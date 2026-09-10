# Private Medical Record Design

## Delivered Phase 6 boundary

SmartCare keeps a patient-owned longitudinal view of clinician-finalized visits, structured prescriptions, medicines, allergies, follow-up guidance, and patient-uploaded reports. It stores and displays authored care facts; it does not infer a diagnosis, prescribe medicine, rewrite clinical notes, or run RAG over documents in this phase.

## Authorization model

| Action | Required relationship |
|---|---|
| View own longitudinal record | Authenticated patient owns the resolved patient profile |
| Upload or download own report | Authenticated patient owns the document patient ID |
| View a patient's record | User is linked to an active doctor and that doctor has an appointment with the patient |
| Finalize a visit | Linked doctor is assigned to that appointment, which is in consultation or completed |
| Link a doctor login | Hospital admin or super admin through the explicit account-link endpoint |

Controller roles are only the first gate. The service resolves the principal, patient, doctor, appointment, and document again before accessing data. Guessing a UUID never grants access. A doctor-role JWT obtained before account linking must be refreshed by signing in again.

## Clinical integrity

- One finalized clinical visit is allowed per appointment.
- A visit records who entered it and when it was finalized.
- Prescription medicines are structured child items rather than one unsearchable text blob.
- Allergy severity and active/resolved status are explicit patient-safety fields.
- No edit/delete API is exposed for finalized visits. A production amendment workflow should add a signed correction linked to the original instead of overwriting history.
- Diagnosis and document contents are excluded from application and audit log payloads.

## Private report storage

PostgreSQL stores metadata and an opaque storage key; it does not store report bytes. The delivered `PrivateDocumentStorage` port has a development filesystem adapter that writes outside the web root and persists on a private Docker volume. It performs:

1. 10 MB limit enforcement.
2. PDF/JPEG/PNG magic-byte detection rather than trusting the browser MIME value.
3. Filename sanitization and path containment checks.
4. SHA-256 calculation for integrity metadata.
5. Temporary write followed by atomic move where supported.
6. Authorized streaming through an API response with `Cache-Control: no-store` and no public storage URL.

Before real patient use, replace this adapter with managed private object storage, customer-controlled encryption/key policy as required, malware scanning/quarantine, backup and restore testing, retention/legal-hold workflows, and short-lived access. File-signature detection is not malware scanning.

## Audit events

Sensitive patient record view, doctor appointment view, visit finalization, upload, and download actions append an audit event containing actor, resource, hospital, correlation ID, timestamp, and outcome. Clinical text and binary contents are deliberately omitted.

## Deferred safely

- Patient-isolated document retrieval and cited grounded answers are delivered in Phase 7; approved OCR and production model/vector adapters remain deferred.
- Lab/MRI/CT/X-ray ordering, capacity queues, and structured staff-verified results are delivered in Phase 8 through the separate `diagnostic` module. Production report-file publication into managed private object storage remains deferred.
- Consent delegation, emergency break-glass access, amendments, clinician verification of patient uploads, production object storage, antivirus scanning, and jurisdiction-specific retention require explicit later workflows.
