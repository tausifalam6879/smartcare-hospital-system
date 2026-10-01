# SmartCare release gates

> Current status: see `VERIFICATION-2026-09-30.md` and `VERIFICATION-2026-09-29.md`. The dated entries below are historical: hospital membership, profile-photo support and local restore drills have since been implemented/verified to the extent described in those reports. Their older "still required" statements are not the current implementation status. Remaining release gates are listed in the 30 September report.

Do not interpret a green build as production approval.

## Verified in the current batch

- Queue advancement checks doctor account ownership, not only the DOCTOR role. Existing reception/admin authority is retained.
- Integration coverage rejects an unrelated doctor before changing queue state and permits the assigned doctor.
- Clinical drafts clear before the newly selected appointment is painted.
- Late diagnostic catalogue responses are ignored after their hospital context is removed.
- Desk worklist supports Hindi labels, statuses, filters and actions without translating API identifiers.
- Broken profile images fall back to initials or a neutral icon; replacement URLs may load normally.

## Still required

Follow-up verification: all 26 backend tests passed after adding booking/payment retry-key mismatch protection and rejecting changed payloads for an existing webhook event ID. Payment tests cover invalid signatures and late success after cancellation requesting a refund without reopening the appointment. These use isolated transactional test data, not real money. The preceding frontend localization batch passed all 50 tests.

Batch test evidence: 23 backend tests, 48 frontend tests and 8 ML tests passed locally on 27 September 2026. ML dependency check reported no broken requirements. A frontend booking test initially timed out under concurrent builds and passed on retry; a patient-switch test exposed the before-paint draft issue, which was fixed before the final complete frontend pass.

- Browser-driven real-backend patient journey, including cancellation, retries and expired reservations.
- Hospital-scoped staff authorization audit: the doctor-queue fix does not establish hospital assignment restrictions for every staff endpoint.
- Real payment provider selection, sandbox credentials, signed webhook verification and reconciliation before any live collection.
- Patient-name/profile-photo delivery through appropriately authorized APIs. Shared queues still display patient IDs; do not infer identity or gender from names.
- Remaining desktop page translations and visual review. Mobile deliberately deferred.
- Backup and restore drill in an isolated environment; never test a restore against the active database.

## Backup scope and safety

The database alone is not a complete backup. Preserve the PostgreSQL database and the `medical_documents` volume together. Redis may contain transient state and must be considered when planning recovery. Preserve deployment configuration and encryption/signing material separately in approved secret storage, not Git.

For a consistent local snapshot, schedule a maintenance window, stop application writes, create a PostgreSQL custom-format dump using the configured database/user, and archive private documents. Check each command exit code and record checksums, database version and timestamp. Store encrypted copies outside this repository with restricted access. `/backups/` is ignored as an additional safeguard, not encryption.

Restore only into a separate database and document volume. Verify appointment/payment counts, document access, role permissions and patient-record isolation before declaring the backup usable. No backup or restore has been executed by this checklist.

## Product boundaries

Staff enrolment batch: added one-time, mobile/role-bound 24-hour invitations, hashed token storage, revocation and locked consumption. Only SUPER_ADMIN issues invitations; public registration remains patient-only. Login workspace selection is checked against stored roles. Existing trusted staff accounts retain password login; invitations are not WebAuthn/MFA. See `STAFF-ACCESS.md`. Security integration coverage checks spoofed roles, invalid/revoked/expired/reused codes, wrong mobile/role, forbidden issuance and verified doctor-profile linking. The deployed schema includes migration V17.

28 September follow-up: private doctor appointment responses now include the patient display name and recorded gender; shared desk/public queue DTOs have not been expanded. Missing gender remains neutral, and no profile-photo upload/source has been implemented. Doctor check-in access now verifies the treating doctor relationship. Follow-up completion eligibility is calculated by the backend using the hospital date; its desktop page now has Hindi labels and a light blue header.

`infra/verify-backup-restore.ps1 -AllowBriefDowntime` creates an isolated temporary PostgreSQL container, pauses backend writes during the snapshot, restores a custom-format database dump, compares table counts and document checksums, and removes only its labelled drill container/temporary copy. It does not overwrite the active database, retain an offsite backup, or verify the complete restored application's authorization. Docker must be running. Execution status must be recorded separately; the presence of this script is not proof of recovery.

Scope clarification from the user: keep payments demo-only; Google Cloud hosting will be selected later. Neither real money collection nor paid cloud resources are authorized in this batch.

Executed on 28 September: all 26 backend and 53 frontend tests passed; production build passed. The local restore drill passed for 49 public tables and the private-document archive (7 files present in the source). The labelled temporary restore container was removed, and backend writes resumed. No retained/offsite backup was created.

Permission audit findings still open: users currently have roles but no hospital-membership relation. `HospitalOperationsService.dashboard`, `PaymentService.confirmCash`, `DiagnosticWorkflowService` staff worklists/result actions, `BloodBankService` staff actions and `AmbulanceService` dispatch actions therefore need explicit hospital assignment enforcement before multi-hospital production use. Do not treat the narrower treating-doctor check-in/queue checks as resolving this. Implement a persisted assignment model, authorized provisioning and tests with two hospitals; do not infer membership from the hospital selected in the browser.

GitHub Pages is a frontend demo. Docker hosts the working local backend. Online payment is development-only until a provider is configured. Live ambulance GPS and a maintained 3D indoor map require separate integrations and data. Do not label simulated data as live.
