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
4. Start a backend revision without moving traffic. Flyway applies the additive V23 migration on startup. After the revision is ready, move backend traffic to that exact revision and check health and the new public request validator using an empty, invalid request. This check creates no staff request or account.
5. Retain backend startup logs for migration verification. If Flyway log evidence has not arrived yet, explicitly verify migration 23 in `flyway_schema_history`; health alone does not establish the migration history.
6. Deploy the frontend, route traffic to its ready revision, then verify its HTML and JavaScript contain the new request/activation routes and the correct backend URL.

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

If the script stops, retain its output and checkpoint. It leaves the existing frontend unchanged until backend readiness and route validation pass. If a frontend step fails after backend rollout, the new backend remains compatible with the old frontend. No database rollback or table deletion is attempted. Previous revision names are retained for deliberate traffic rollback if required.

The SQL backup is not a complete medical-document/offsite backup. Persistent document recovery and the broader cloud browser workflow remain separate verification work.

Command references: [Cloud Build submit](https://docs.cloud.google.com/sdk/gcloud/reference/builds/submit), [Cloud Run service update](https://docs.cloud.google.com/sdk/gcloud/reference/run/services/update), [Cloud Run traffic update](https://docs.cloud.google.com/sdk/gcloud/reference/run/services/update-traffic), and [Cloud SQL backup creation](https://docs.cloud.google.com/sdk/gcloud/reference/sql/backups/create).
