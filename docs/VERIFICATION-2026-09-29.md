# Local verification — 29 September 2026

This dated report supersedes older implementation gaps in RELEASE-CHECKLIST.md; it is not production approval.

## Implemented
- Persisted hospital memberships, SUPER_ADMIN provisioning, assigned-hospital selectors and hospital checks across operational workflows. Active linked doctors retain their own hospital access.
- Invitation-only staff activation; public signup remains patient-only. Invitations are not MFA/passkeys.
- Own-account identity/photo management, image validation/re-encoding, password changes and session-version revocation.
- Short-lived, hashed, single-use recovery codes issued by a system administrator after identity verification. No automated SMS/email recovery.
- Private clinical patient-photo delivery; public/shared queue identity remains restricted.
- English/Hindi updates for account/authentication pages. Full site translation remains unfinished.

## Evidence
- Backend integration suite: 37 tests passed in this batch; latest Docker compilation passed.
- Frontend: 59 tests across 29 files passed on 29 September.
- Rebuilt backend/frontend deployed locally; all five Docker services healthy.
- Dedicated synthetic patient `SmartCare QA Patient Sep29` created through public registration; browser login and patient dashboard verified. No real money or real patient account used.
- Isolated restore drill passed: 52 database table row counts and private-document archive checksums matched. Active database was not overwritten.
- Encrypted backup retained under ignored `backups/`. DPAPI encryption round-trip verified; decryption requires this Windows user/machine. This is not offsite disaster recovery or a full restored-application test.

## Still not complete
- Entire browser patient journey, including clinical completion, reports, follow-up and retry/cancellation cases.
- Complete endpoint-by-endpoint permission audit, full desktop translation/visual review, production-grade distributed rate limiting and optional MFA.
- Offsite destination and scheduled portable backup/restore procedure (user has not selected a destination).
- Google Cloud hosting, production secrets/TLS and deployment verification; user deferred cloud hosting.
- Payments remain demo-only. Live GPS and maintained 3D hospital mapping require separate implementations. Mobile work remains deferred.

Existing sessions may need re-login after the session-version deployment. Test account has PATIENT only; no staff/admin access was granted.

## Follow-up permission fix
- Found and fixed an inactive-doctor document-download gap: an old appointment no longer authorizes document access after the linked doctor profile is deactivated.
- Regression coverage checks active treating-doctor access, denial for another patient, denial for the inactive doctor, and continued access for the patient who owns the document. HTTP requests with issued JWTs also verify these responses through the security filter and controller.
- Full backend suite passed (37 tests); the expanded HTTP medical-record test then passed separately. Rebuilt backend deployed locally and reported healthy.
- The connected integration scenario covers cash booking, confirmation, check-in, consultation, prescription, private document and follow-up state. This does not replace the still-pending full browser journey.

## Authentication request isolation
- Public login/registration/recovery requests no longer carry an old stored bearer token.
- A delayed 401 from a previous session cannot clear a newer login; rejection of the current session still signs it out.
- Six new frontend regression cases pass; full frontend suite now passes 65 tests across 29 files.
- Corrected English/Hindi login guidance: the current implementation persists login in browser local storage until expiry or sign-out, not merely until the tab closes. This is a disclosure correction, not a change to storage policy.
- These code findings do not establish the cause of the earlier browser automation login failure. Full browser patient-journey verification remains pending.

## Staff worklist HTTP matrix
- Added an isolated JWT-backed matrix for PATIENT, RECEPTIONIST, CASHIER, LAB_TECHNICIAN, BLOOD_BANK_STAFF, AMBULANCE_DISPATCHER and HOSPITAL_ADMIN across operations, diagnostics, blood-request and ambulance-request worklists.
- All 84 HTTP assertions passed: expected role access in the assigned hospital, denial in a different hospital, and denial after membership removal using the same token.
- This matrix covers these four read endpoints, not all mutation endpoints, SUPER_ADMIN or every doctor workflow; do not label it a complete permission audit.
- Found and fixed another deactivated-doctor gap in the operations dashboard and doctor-day status authorization when explicit hospital membership remains. Regression coverage includes the active profile before deactivation and denials afterward.
- Browser inspection found a non-QA patient session. No booking or clinical mutation was made in that account; dedicated test-session confirmation is pending.
- Full backend suite after these additions: 38 tests, zero failures/errors. Backend Docker image rebuilt successfully.

## Browser booking check
- Recovered browser interaction by opening a fresh test tab. Existing shared session signed out during testing; resumed with the dedicated QA patient, not the unrelated open patient record.
- Created QA cash booking `cd990dd0-b909-4e47-9792-1e5c16395954` for Dr. Ananya Mehta, 30 September 2026, OPD 1 through the browser. Cash-pending result and deadline rendered.
- Confirmed test cash through the demo office-clerk API (no real payment). Patient dashboard then displayed CONFIRMED, OPD 1, and the correct doctor/date; queue showed the correct hospital room.
- Fixed stale capacity display after booking/cancellation: the current selection's availability now refreshes through the existing guarded effect. Frontend suite (65 tests), production build and Docker deployment passed.
- This was not the complete clinical journey: visit is future-dated, and successful check-in/consultation/prescription/follow-up browser verification is still outstanding. Cash confirmation was API-driven, not cashier-UI tested.
