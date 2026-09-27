# Workflow checkpoint — 27 September 2026

## Verified

- All 44 frontend tests passed across 23 files on 27 September.
- All 17 local demo doctor/staff logins and their workspace API reads passed on 27 September via `infra/verify-local-logins.ps1`.
- Frontend Docker production build passed and was deployed locally; the root URL returned HTTP 200.
- On 26 September, all 23 backend tests passed. The subsequently expanded medical-record integration test also passed: unpaid check-in is rejected, the queue cannot advance before finalization, and after finalization the next patient enters consultation without inheriting the previous patient's record. Existing assertions cover prescription, follow-up and private record isolation.

## Fixes included

- Home stays accessible after login instead of redirecting back to My care.
- Office worklist has intersecting search, doctor, payment-method and booking-status filters, with explicit next-step messages.
- Desktop Home banner separates copy from the full illustration; mobile changes deferred by request.
- Doctor drafts reset when the active appointment changes. Same-patient refresh preserves the draft. Regression test covers both cases.

## Remaining release gates

- Browser-driven full patient journey, including failure/retry paths, is not yet comprehensively verified. Service integration tests and workspace reads are not a substitute for that check.
- Real online payment needs a chosen provider, test credentials and verified webhook setup; current online flow remains development-only.
- Continue desktop Hindi/English coverage and authorized patient identity display; do not fabricate names, portraits or gender.
- Backup/restore drill and production security review remain outstanding.
- GitHub Pages publishes a static frontend demo, not the Docker backend or patient database.
- Live ambulance GPS and 3D indoor navigation are separate unimplemented integrations.
