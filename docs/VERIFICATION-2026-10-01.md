# Verification — 1 October 2026

## Diagnostic workflow and fixes

- Real PostgreSQL testing exposed HTTP 500 on diagnostic order creation: `ck_notification_type` omitted diagnostic notification enum values. Added forward migration V22 (no modification of applied migrations). Backend image rebuilt, migration deployed, container healthy. The same authorized order creation then succeeded.
- Added an enum-to-migration allow-list regression test. The full 39-test backend suite passed with expanded cross-hospital assertions; the newly added migration test passed separately (40 tests across these runs, not one full 40-test run).
- Browser scheduling exposed a native date-picker value not reaching React change state before submission. Diagnostics scheduling now submits the displayed input date. Regression test added; all 68 frontend tests passed and Docker production build passed.
- Cancelled only the disposable incorrectly dated QA diagnostic order. A new QA order was browser-scheduled for 30 September, and the returned UI confirmed that exact date.
- Authorized demo lab account used browser actions: sample collection -> start processing -> enter result -> verify and release. Result explicitly states synthetic QA only, no specimen/measurement, not for clinical use; flag INDETERMINATE.
- On 1 October, patient browser confirmed the persisted verified result in both Tests and Health records, including verifier identity and timestamp.
- Doctor order creation used the normal authorized API on an already completed QA consultation. This is a mixed API/browser end-to-end check, not an entirely browser-driven order creation test.

## Private document round-trip

- Patient browser uploaded a QA-only screenshot under Other document. UI confirmed one private file, marked Patient uploaded (not clinician verified).
- Clicked Authorized download, but browser automation's download event timed out. Do not claim browser filesystem download capture succeeded.
- The real authorized document endpoint returned HTTP 200, 91,381 bytes, SHA-256 identical to the uploaded file.
- Unrelated lab account received HTTP 403 for the same document endpoint.

## Permission coverage added

Expanded existing isolated integration workflows to reject other-hospital staff actions:

- Diagnostics: collect, start, verify result; order remains scheduled with no result.
- Blood bank: verify inventory, retry matching, fulfil, cancel, donor contact matching; original reservation remains unchanged.
- Ambulance: assign, acknowledge, update status, cancel, update vehicle location; request remains requested and vehicle available.

These are service integration checks, supplementing prior real-token HTTP tests; they are not exhaustive per-role HTTP coverage. Donor verification's global authorization policy and historical record access rules still need explicit review. Desktop translation/visual review and production/offsite setup remain open. No real payment or clinical data was used; no Git commit/push performed.

## Follow-up focused batch

- Historical record gap fixed: merely pending, cancelled, expired, waitlisted or no-show bookings no longer grant doctor record access. Doctor must be active and have a checked-in, in-consultation or completed appointment. Completed treating visits continue to permit longitudinal care; this does not implement time-limited patient consent.
- Added cancelled-booking private-file HTTP 403 regression; owning patient retains access. Pending appointment record access is denied.
- Network-wide donor consent remains network-wide. Donor verification now additionally requires staff to retain an active hospital assignment (SUPER_ADMIN exception); removing all assignments denies verification. Added regression coverage. No donor's clinical eligibility was changed in the live database.
- Browser download resolved: both `smartcare-qa-lab-result-oct01.png` and `(1).png` were found in the user's Downloads directory, 91,381 bytes each, and both SHA-256 hashes match the original uploaded file. Previous event timeout was not a failed download. Files retained, not deleted.
- Queue page gained Hindi status headings, primary labels/navigation/privacy text and localized dates; Hindi rendering regression added. This is a focused page improvement, not a complete site-wide translation/visual audit.
- Full backend suite: 40 passed. Full frontend suite: 69 passed; after the last two label changes, focused queue suite: 2 passed. Docker production images rebuilt.

## Desktop follow-up error-state regression

- Follow-up fetch failure no longer renders the misleading no-follow-up empty state. Added English/Hindi retry state; successful retry restores the records and clears the error.
- Full frontend suite: 70 passed. TypeScript/Vite production build passed, with the existing large bundle warning still present.
- This focused change is not a complete role-by-role desktop visual review or a fresh full browser patient-flow verification.

## Staging preparation verification results

- Added opt-in staging configuration and startup guards for required secrets, explicit HTTPS CORS origins, disabled demo seeding and acknowledged persistent private-document storage. These checks do not provision or prove storage durability.
- Full backend suite after these changes: 43 tests, zero failures/errors/skips (1 October, 19:22 IST).
- Explicit Docker staging target built successfully. An isolated, network-disabled container reported uid=100(smartcare), gid=101(smartcare); its image document directory passed the writable-directory check. No existing document volume was mounted or modified. Mounted-volume permissions still require deployment verification.
- All five existing local Compose services remain healthy. The staging image was not substituted into the local demo deployment.
- Cloud deployment, external backup destination, secrets provisioning and cloud smoke tests remain pending; see STAGING-READINESS.md. No cloud resources, Git commit or push were performed in this batch.
