# Release the verified staff access flow

Run the release from a fresh checkout in Google Cloud Shell signed into the existing SmartCare staging project. The local desktop does not currently have Google Cloud CLI or a configured Cloud login.

```bash
SMARTCARE_RELEASE_DIR=$(mktemp -d -t smartcare-release-XXXXXXXX)
git clone --depth 1 https://github.com/tausifalam6879/smartcare-hospital-system.git "$SMARTCARE_RELEASE_DIR" &&
bash "$SMARTCARE_RELEASE_DIR/infra/deploy-staging.sh"
```

The script targets only project `smartcare-staging-2026`, region `asia-south1`, database `smartcare-staging-db`, and the existing `smartcare-backend` and `smartcare-frontend` services. Every Cloud command specifies the project explicitly.

## Release sequence

1. Verify existing services, database and Artifact Registry repository. Check that the backend uses the staging profile, Flyway, and Hibernate schema validation. Do not print or change its secret values.
2. Build both images using the Git commit as the release tag. Existing images for that commit are reused on retries. Backend build keeps the current default Docker target; this release does not also change runtime identity or mounted-volume permissions.
3. Create and verify an on-demand Cloud SQL backup before starting the new backend. Cloud Build and retained backup storage consume the existing project billing; the script does not upgrade SQL, increase instance limits, change budgets or create new services.
4. Start a backend revision without moving main traffic. Check the exact revision resource, not the service's `latestReadyRevisionName`: a zero-traffic revision can be Ready but Retired. Assign a temporary verification tag, request its health endpoint to exercise startup, and check the new public request validator using an empty, invalid request. This creates no staff request or account. Image digests pin the selected release.
5. Retain backend startup logs and require explicit migration-success/current-schema-version evidence for V23 before any main traffic switch. A migration-start message is insufficient. If success evidence has not arrived yet, stop with main traffic unchanged; inspect Cloud SQL history and retain the output before proceeding.
6. Deploy the frontend without main traffic, verify its exact revision and tagged HTML/JavaScript, then move backend traffic first and frontend traffic second. Recheck the public URLs and remove the temporary `verify-*` tags. Pre-existing tags are preserved.

Image-only service updates preserve the existing environment, secret references, Cloud SQL attachment, mounted storage, service account, resource settings and permissions. They do not reset the database, replace account passwords, re-enable bootstrap, enable real payment collection or grant staff roles.

## Completion checks

- Cloud Shell prints `Deployment HTTP checks passed` and both new revision names.
- The SQL backup receipt and startup logs are retained in the printed temporary diagnostics directory; keep needed deployment evidence before that Cloud Shell storage is cleared.
- In Cloud SQL Studio, select database `smartcare` and authenticate as the schema owner. Run this read-only query to confirm V23:

```sql
SELECT version, description, success
FROM public.flyway_schema_history
WHERE version = '23';
```

- Open Login in the live frontend and confirm **Staff without an invitation? Request access** is visible.
- Perform a dedicated synthetic staff request, administrator review, invitation, activation and role-workspace check. Existing valid invitations and accounts should continue working. The automated release checks do not replace this authenticated browser check.

## Resume an already-built release

For the 18 October release that stopped after creating backend revision `smartcare-backend-rca4ccf8c0b8e-c8df9c`, run the updated script from a fresh checkout:

```bash
bash infra/deploy-staging.sh --resume-backend smartcare-backend-rca4ccf8c0b8e-c8df9c
```

Resume mode extracts the original image-release commit from the explicit revision name, checks both existing images and that revision's identity/image, and verifies the successful original SQL backup by its deployment description. It never rebuilds images, creates another SQL backup, or redeploys the backend. It repeats candidate validation and creates a fresh frontend candidate from the already-built frontend image. It does not claim that application source comes from the newer script checkout. Do not use a revision from a different release or remove migration history to resume.

If the script stops, retain its output and checkpoint. Both main traffic assignments remain unchanged until backend health, new-route validation, V23 evidence and candidate frontend assets pass. If a public frontend check fails after rollout, the new backend remains compatible with the old frontend; previous traffic assignments are retained for deliberate rollback. No database rollback or table deletion is attempted. Zero-traffic candidate creation can still apply an additive migration when the candidate starts; traffic isolation is not database isolation.

`bash infra/test-deploy-staging.sh` runs 13 offline scenarios with mocked Cloud/network commands: fresh/resumed success with stale service pointers and retired revisions, plus failure guards for builds, backups, revision readiness, image identity, health, routes, migration evidence, assets and missing resume images. These are regression checks, not a substitute for actual Cloud validation.

The SQL backup is not a complete medical-document/offsite backup. Persistent document recovery and the broader cloud browser workflow remain separate verification work.

The original frontend build reported dependency security warnings. A read-only `npm audit --omit=dev` on 18 October identified the runtime dependency Axios 1.19.0 as high severity, with a non-major update to 1.20.0 available. Resume deliberately reuses the original image and does not fix that warning. Keep this release limited to staging; patch and retest the dependency separately before claiming production readiness. Development-tool warnings require their own review; do not run `npm audit fix --force` blindly.

Command references: [Cloud Build submit](https://docs.cloud.google.com/sdk/gcloud/reference/builds/submit), [Cloud Run service update](https://docs.cloud.google.com/sdk/gcloud/reference/run/services/update), [Cloud Run traffic update](https://docs.cloud.google.com/sdk/gcloud/reference/run/services/update-traffic), and [Cloud SQL backup creation](https://docs.cloud.google.com/sdk/gcloud/reference/sql/backups/create).
