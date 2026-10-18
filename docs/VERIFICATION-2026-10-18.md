# SmartCare verification on 18 October 2026

## Changes completed

- Added the missing staff access request flow, reachable from Login and staff registration. A request collects hospital, role, mobile, name, optional contact email and an employment reference; it grants no account or role.
- Added a Super Admin request inbox with independent identity verification, approval through invitation issuance, and rejection. The server checks that the approved invitation matches the pending request's hospital, role and mobile. Duplicate pending submissions are deduplicated; endpoints use attempt limits.
- Added invitation history with active, accepted, revoked and expired states, pagination, refresh and revocation after page reload. List responses contain neither the raw code nor its hash. Lost active codes can be revoked and replaced.
- Added full invitation copying with the activation URL, exact mobile/role, hospital and expiry. A generated code remains visible until explicitly dismissed or revoked. Issuance rejects competing active invitations for the same hospital/mobile or doctor, using a hospital lock to coordinate concurrent requests.
- Doctor selection now uses an administrator-only endpoint returning active unlinked doctor profiles. Registration still rechecks eligibility under a doctor lock.
- Existing account holders can activate an approved staff role using a valid invitation and their current password. Their account and patient records are preserved. Wrong passwords do not consume the invitation or grant permissions. New registration cannot silently overwrite an existing account.
- Added invitation-code visibility control and explicit instructions about exact mobile/role matching. Admin and registration pages include request/activation links. Codes are shared manually; no SMS or email delivery is claimed.
- Fixed Hospital setup showing no OPD hours after a schedule was saved: directory summaries omit schedules, so setup now loads doctor details. Hospital switching is disabled during saves, and changing hospitals remounts dependent forms to clear stale selections.
- Fixed the dark login/register panel's low-contrast logo by using the existing inverted brand variant.

## Automated checks

- Full backend suite: **49 tests passed**, no failures/errors/skips. Tests cover the existing appointment, payment, check-in/queue, consultation/records, diagnostics, follow-up, blood-bank, ambulance, navigation and authorization workflows, plus the new request/invitation checks.
- After simplifying the nullable doctor parameter in the duplicate-invitation query, the focused authentication and staff-access suites passed again: **16 tests**. These are included in the 49, not additional tests.
- Full frontend suite: **82 tests passed** across 32 files. Coverage added for request submission, admin review, copying the original invitation context, required verification, existing-account activation, invitation visibility, failed hospital loading, authenticated inbox calls and saved OPD schedule display.
- TypeScript and Vite production build passed. The existing large JavaScript bundle warning remains.
- Backend tests used isolated H2 databases and Java 24 with the project's Java 17 compilation target. Flyway is disabled in those tests; they do not prove that a new migration has run on PostgreSQL or Cloud SQL.

## Running backend checks

An isolated local Spring Boot application was started on port 8086 with a temporary in-memory H2 database, test-only administrator and synthetic records. It did not connect to Cloud SQL or the active hospital database.

All eight staff roles completed request, administrator approval, invitation activation, password login and their relevant authorized worklist: doctor, hospital administrator, office clerk, receptionist, cashier, laboratory technician, blood-bank staff and ambulance dispatcher. Seven non-doctor staff roles were denied worklists for another hospital; doctors used their linked-profile appointment list. Ordinary staff were denied the administrator request inbox. Duplicate active invitations and a reused code were rejected. Existing-patient activation required the current password and preserved the user ID. Linked doctors were absent from the eligible-profile endpoint.

The same HTTP check also connected a synthetic patient to the newly activated doctor and office clerk: patient registration, booking, retry with the same idempotency key returning the same appointment, authorized cash confirmation, reception check-in, assigned-doctor queue advancement, consultation finalization, a clearly labelled non-medication QA prescription, patient record retrieval and follow-up creation. The appointment reached `COMPLETED`. No real money, medical assessment or patient data was involved. This exercised real HTTP and persistence boundaries on H2, not a browser or PostgreSQL.

The synthetic QA server was stopped after verification; its in-memory test database was discarded. No active hospital database or private-document volume was deleted or restored. The temporary Vite test server was also stopped.

`infra/verify-staff-onboarding.ps1` reproduces these checks using synthetic data and refuses non-loopback hosts. It intentionally leaves synthetic records in the selected local test database; use a disposable test application, not a populated local database. The test does not print passwords or invitation tokens.

## Deployment and remaining work

1. Deploy the backend containing `V23__staff_access_requests.sql` before the matching frontend using [the staging release guide](CLOUD-RELEASE.md). The release script passed Bash syntax validation and offline success-path simulation. Simulated build, SQL-backup, backend-health and new-route failures each stopped before a frontend update. These are mocked checks, not a Cloud deployment; this desktop has no configured Google Cloud CLI/login. Existing staging deployment history is separate from verification of these changes.
2. Apply and verify V23 on PostgreSQL/Cloud SQL, then test this access flow through the deployed website. The local Docker engine was unavailable, and the in-app browser failed to attach; local component and HTTP checks are not a completed desktop browser visual audit.
3. Complete cloud-specific private-document persistence/replacement and offsite backup/restore checks. Prior local restore drills do not establish cloud/offsite recovery.
4. Review the reported problems in other screens against reproducible actions and current deployed responses. Existing green workflow tests do not prove every screen or action is error-free. Broad desktop translation/visual coverage and production observability remain separate work.
5. Keep real payment, live ambulance GPS and advanced 3D indoor mapping outside the current implemented scope. Payments remain development/demo only; mobile work remains deferred as requested.

See [Staff access guide](STAFF-ACCESS.md) for the complete user/admin process.
