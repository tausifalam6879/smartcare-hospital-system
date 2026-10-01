# Verification — 30 September 2026

## Live local QA journey

Used only the dedicated disposable QA patient, not real patient records or real payments.

- Existing 30 September OPD 1 booking: browser mobile-web check-in succeeded; private queue token displayed.
- Treating doctor signed in through the browser, called the QA patient, entered clearly synthetic prescription/notes and finalized the visit.
- Doctor worklist showed Completed; patient dashboard showed one completed visit and Health records displayed the matching doctor, date, notes and synthetic prescription.
- Found a native date-input submission problem: the displayed follow-up date was not submitted in this browser run. Finalization now reads the displayed date from the form, with a regression test for submission before the date change event commits. This observation does not establish that all browsers have the same event behavior.
- Prepared a second disposable booking through ordinary patient/office APIs (cash confirmation and check-in; no database status overrides). Doctor browser finalized this visit after the fix. Patient Follow-ups displayed 1 October 2026 and the matching recommendation. Browser confirmation persisted as Confirmed after signing in again.
- First QA visit intentionally remains as recorded, without a scheduled follow-up; finalized clinical data was not rewritten to hide the failed test.

## Fixes and automated checks

- Native follow-up date is included at finalization; medication-reminder request requires a date and a structured prescription.
- Check-in action errors are separate from queue-refresh errors, so successful background refresh does not erase a rejected check-in explanation.
- Confirmed queue heading says Appointment confirmed, rather than promising the check-in window is open.
- Frontend: 30 test files, 67 tests passed. Docker production build passed and frontend container became healthy.
- Backend: 39 tests passed, zero failures/errors/skips.
- Added real-token MockMvc mutation coverage: foreign-hospital cashier/receptionist cannot confirm cash, check in or advance another hospital's queue (403), with appointment unchanged. Assigned staff can perform the same three operations and appointment becomes IN_CONSULTATION.
- This supplements the previous 84 worklist HTTP assertions, not a claim that every mutation of every module has been exhaustively audited.

## Remaining release gates

- Browser lab-order/scheduling/result-release path and private report upload/download still need a complete live walkthrough; existing integration tests are not browser evidence.
- Extend mutation-level authorization coverage across diagnostic, blood-bank and ambulance actions and review remaining historical-record access rules.
- Desktop Hindi/English and visual consistency review across remaining pages.
- Production Google Cloud hosting and an approved offsite backup destination remain deferred by the user. Demo-only payments remain intentional; no live gateway is required for this demo scope.
- Prior local backup/restore evidence is in VERIFICATION-2026-09-29.md; Windows-user-bound encrypted local backup is not offsite disaster recovery.

No commit or push performed in this batch.
