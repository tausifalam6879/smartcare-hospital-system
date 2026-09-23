# SmartCare workflow verification — 24 September 2026

## Fixes delivered

- Ten demo doctors now have separate logins linked to their own doctor records; previously only two had demo logins.
- Added office clerk (receptionist + cashier), receptionist and cashier accounts. Restart does not duplicate accounts or reset existing passwords.
- Cashier-only users now see the cash confirmation action. Clinical/check-in controls remain restricted to the relevant roles.
- Doctor and operations queues refresh every 15 seconds while visible, and on window focus.
- Doctor consultation selection drops appointments that are no longer in consultation.
- Report upload, diagnostic dates, follow-ups and blood availability/expiry now use each hospital's local calendar day. UTC timestamps are retained. Tests initially caught three failures just after Indian midnight; these passed after the fixes.

## Automated verification

| Area | Evidence |
| --- | --- |
| Backend | 23 tests passed across 16 test classes, including new account-linking/restart and timezone checks |
| Frontend | 37 tests passed across 21 files, including cashier action regression |
| ML service | 8 tests passed in the running container; pip check reports no broken requirements |
| Frontend dependencies | npm ls resolves all declared top-level dependencies |
| Packaging | Backend and frontend Docker builds succeeded |
| Deployed accounts | All 17 doctor/staff logins and their authorized workspace API reads passed through localhost:5174 after deployment |

Existing backend integration coverage includes registration/auth, hospitals, doctors/schedules, capacity/waitlist booking, payment confirmations/refunds, check-in/live queue, hospital operations/recovery, finalized medical records/private documents, diagnostics/results, blood inventory/requests, ambulance dispatch, indoor navigation, patient-isolated care assistant and blood-image analysis. These tests use isolated test data; they do not constitute a manual review of every screen or a guarantee of zero bugs.

## Reference image features and current boundaries

| Reference feature | Project behavior |
| --- | --- |
| Role-specific dashboards | Separate patient, doctor, office, administrator, lab, blood and dispatcher access |
| Cash / online payment | Cash confirmation works through authorized staff. Online is DEVELOPMENT: no money collected and no real UPI/card checkout. Signed webhook confirmation is covered in tests. |
| Doctor queue / current consultation | Patient must have the matching doctor's confirmed appointment for today, then check in within the permitted window |
| Reports, prescriptions, follow-ups | Generated from authorized clinical/lab actions; not prefilled screenshot examples |
| Ambulance tracking | Dispatcher stage changes and last-shared area; no live GPS route tracking |
| Indoor map | Hospital-maintained locations and QR routes; not the reference's photorealistic 3D floor visualization |
| Patient portraits / names | Shared queue currently uses patient numbers and neutral avatars; verified photo/gender data is not supplied by its API |
| Payment/earnings chart, messages | Reference artwork does not prove these are implemented modules; real revenue analytics and general chat are not established by this audit |
| Other hospital directory bookings | Explicit prototype previews; the seeded linked accounts belong to SmartCare Demo Care Centre |

Use [LOCAL-LOGIN-ACCOUNTS.md](LOCAL-LOGIN-ACCOUNTS.md) for all IDs and the exact handoff sequence. `infra/verify-local-logins.ps1` checks authentication and authorized workspace reads without creating appointments or exposing tokens.
