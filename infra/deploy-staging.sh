#!/usr/bin/env bash
# Run from a clean checkout in an authenticated Google Cloud Shell.
set -Eeuo pipefail

PROJECT_ID=smartcare-staging-2026
REGION=asia-south1
DB_INSTANCE=smartcare-staging-db
BACKEND=smartcare-backend
FRONTEND=smartcare-frontend
IMAGE_ROOT="${REGION}-docker.pkg.dev/${PROJECT_ID}/smartcare"
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE="$(git -C "$ROOT_DIR" rev-parse --short=12 HEAD)"
WORK_DIR="$(mktemp -d -t smartcare-deploy-XXXXXXXX)"
chmod 700 "$WORK_DIR"
BACKEND_OLD_REVISION=''
FRONTEND_OLD_REVISION=''
STEP='preflight'

on_error() {
  local exit_code=$?
  printf '\nDeployment stopped during %s. Existing accounts and database were not reset.\n' "$STEP" >&2
  printf 'Release: %s\nDiagnostics directory: %s\n' "$RELEASE" "$WORK_DIR" >&2
  if [[ -n "$BACKEND_OLD_REVISION" ]]; then
    printf 'Previous backend revision: %s\n' "$BACKEND_OLD_REVISION" >&2
  fi
  if [[ -n "$FRONTEND_OLD_REVISION" ]]; then
    printf 'Previous frontend revision: %s\n' "$FRONTEND_OLD_REVISION" >&2
  fi
  printf 'Do not delete migration V23 or restore over the active database to retry this deployment.\n' >&2
  exit "$exit_code"
}
trap on_error ERR

for tool in gcloud git python3 curl; do command -v "$tool" >/dev/null || { printf 'Missing required command: %s\n' "$tool" >&2; exit 1; }; done
DEPLOY_ID="r${RELEASE}-$(python3 -c 'import secrets; print(secrets.token_hex(3))')"
if [[ -n "$(git -C "$ROOT_DIR" status --porcelain --untracked-files=no)" ]]; then
  printf 'Tracked files have local edits. Use a fresh checkout so the image tag identifies its source exactly.\n' >&2
  exit 1
fi
if [[ -z "$(gcloud auth list --filter=status:ACTIVE --format='value(account)')" ]]; then
  printf 'No active Google Cloud account. Open this script in your signed-in Cloud Shell.\n' >&2
  exit 1
fi
printf 'Updating existing SmartCare staging services only. Project: %s; release: %s\n' "$PROJECT_ID" "$RELEASE"
printf 'Cloud Build and the retained pre-migration SQL backup use the existing project billing.\n'
gcloud artifacts repositories describe smartcare --project="$PROJECT_ID" --location="$REGION" --format='value(name)' >/dev/null
DB_STATE="$(gcloud sql instances describe "$DB_INSTANCE" --project="$PROJECT_ID" --format='value(state)')"
[[ "$DB_STATE" == 'RUNNABLE' ]] || { printf 'Database is not RUNNABLE; no service update performed.\n' >&2; exit 1; }

# Select only nonsecret metadata for the retained deployment receipt.
gcloud run services describe "$BACKEND" --project="$PROJECT_ID" --region="$REGION" \
  --format='json(metadata.name,status.url,status.latestReadyRevisionName,status.traffic,spec.template.spec.containers[0].image)' > "$WORK_DIR/backend-before.json"
gcloud run services describe "$FRONTEND" --project="$PROJECT_ID" --region="$REGION" \
  --format='json(metadata.name,status.url,status.latestReadyRevisionName,status.traffic,spec.template.spec.containers[0].image)' > "$WORK_DIR/frontend-before.json"
read_service_field() {
  python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(d["status"][sys.argv[2]])' "$1" "$2"
}
BACKEND_URL="$(read_service_field "$WORK_DIR/backend-before.json" url)"
FRONTEND_URL="$(read_service_field "$WORK_DIR/frontend-before.json" url)"
BACKEND_OLD_REVISION="$(read_service_field "$WORK_DIR/backend-before.json" latestReadyRevisionName)"
FRONTEND_OLD_REVISION="$(read_service_field "$WORK_DIR/frontend-before.json" latestReadyRevisionName)"

# Read only the relevant nonsecret flags from the service response; never print its environment.
gcloud run services describe "$BACKEND" --project="$PROJECT_ID" --region="$REGION" --format=json |
  python3 -c '
import json,sys
d=json.load(sys.stdin)
containers=d["spec"]["template"]["spec"]["containers"]
assert len(containers)==1, "Expected the existing single-container backend"
env={e["name"]:e.get("value", "[secret reference]") for e in containers[0].get("env",[])}
assert env.get("SPRING_PROFILES_ACTIVE")=="staging", "Expected the existing staging profile; inspect configuration before deploying"
assert env.get("SPRING_FLYWAY_ENABLED","true").lower()=="true", "Flyway is disabled; migration cannot be verified"
assert env.get("SPRING_JPA_HIBERNATE_DDL_AUTO","validate")=="validate", "Expected Hibernate validation; automatic schema creation is not allowed"
print("Existing staging profile, Flyway migration and schema-validation configuration checked.")'

STEP='building backend'
BACKEND_IMAGE="${IMAGE_ROOT}/backend:release-${RELEASE}"
FRONTEND_IMAGE="${IMAGE_ROOT}/frontend:release-${RELEASE}"
if ! gcloud artifacts docker images describe "$BACKEND_IMAGE" --project="$PROJECT_ID" --format='value(image_summary.digest)' >/dev/null 2>&1; then
  # Use the same Dockerfile/default target as the existing backend image.
  # Runtime environment, mounts, Cloud SQL connector and secrets are preserved by image-only updates.
  gcloud builds submit "$ROOT_DIR/backend" --project="$PROJECT_ID" --tag="$BACKEND_IMAGE" --quiet
fi
STEP='building frontend'
if ! gcloud artifacts docker images describe "$FRONTEND_IMAGE" --project="$PROJECT_ID" --format='value(image_summary.digest)' >/dev/null 2>&1; then
  gcloud builds submit "$ROOT_DIR/frontend" --project="$PROJECT_ID" --config="$ROOT_DIR/frontend/cloudbuild.yaml" \
    --substitutions="_API_URL=${BACKEND_URL},_IMAGE=${FRONTEND_IMAGE}" --quiet
fi

# All builds must succeed before changing traffic or applying the forward migration.
STEP='creating pre-migration database backup'
gcloud sql backups create --instance="$DB_INSTANCE" --project="$PROJECT_ID" \
  --description="Before staff onboarding ${DEPLOY_ID}" --quiet
gcloud sql backups list --instance="$DB_INSTANCE" --project="$PROJECT_ID" \
  --filter="description='Before staff onboarding ${DEPLOY_ID}'" --sort-by='~startTime' --limit=1 \
  --format='json(id,status,type,startTime)' > "$WORK_DIR/database-backup.json"
python3 -c 'import json,sys; b=json.load(open(sys.argv[1])); assert b and b[0]["status"]=="SUCCESSFUL", "Pre-migration backup did not succeed"; print("Pre-migration SQL backup verified.")' "$WORK_DIR/database-backup.json"

STEP='deploying backend and applying Flyway migration V23'
gcloud run services update "$BACKEND" --project="$PROJECT_ID" --region="$REGION" \
  --image="$BACKEND_IMAGE" --revision-suffix="$DEPLOY_ID" --no-traffic --quiet
BACKEND_REVISION="$(gcloud run services describe "$BACKEND" --project="$PROJECT_ID" --region="$REGION" --format='value(status.latestReadyRevisionName)')"
[[ "$BACKEND_REVISION" == "${BACKEND}-${DEPLOY_ID}" ]] || { printf 'The expected backend revision is not ready; frontend will not be deployed.\n' >&2; exit 1; }
gcloud run services update-traffic "$BACKEND" --project="$PROJECT_ID" --region="$REGION" \
  --to-revisions="${BACKEND_REVISION}=100" --quiet
curl --fail --silent --show-error --retry 4 --retry-delay 3 --max-time 90 \
  "$BACKEND_URL/actuator/health" > "$WORK_DIR/backend-health.json"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1])).get("status")=="UP", "Backend is not healthy"' "$WORK_DIR/backend-health.json"

# An invalid request verifies the new public route/DTO without creating a person or granting access.
STEP='checking the new backend request route'
STATUS="$(curl --silent --show-error --max-time 60 -o "$WORK_DIR/request-validation.json" -w '%{http_code}' \
  -H 'Content-Type: application/json' --data '{}' "$BACKEND_URL/api/v1/auth/staff-access-requests")"
[[ "$STATUS" == '400' ]] || { printf 'Expected HTTP 400 for an empty staff request; got %s. Frontend remains unchanged.\n' "$STATUS" >&2; exit 1; }
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert "hospitalId" in d.get("errors",{}), "New staff request validation was not found"' "$WORK_DIR/request-validation.json"
gcloud logging read "resource.type=\"cloud_run_revision\" AND resource.labels.service_name=\"${BACKEND}\" AND resource.labels.revision_name=\"${BACKEND_REVISION}\"" \
  --project="$PROJECT_ID" --freshness=1h --limit=300 --order=asc \
  --format='value(textPayload,jsonPayload.message)' > "$WORK_DIR/backend-startup.log"
if grep -Ei 'Successfully applied.*migration|Migrating schema.*23|current version.*23' "$WORK_DIR/backend-startup.log"; then
  printf 'Flyway startup evidence is retained in %s/backend-startup.log\n' "$WORK_DIR"
else
  printf 'Startup log evidence is not visible yet. The new route and backend health passed; verify V23 in Flyway history before calling database verification complete.\n'
fi

STEP='deploying frontend'
gcloud run services update "$FRONTEND" --project="$PROJECT_ID" --region="$REGION" \
  --image="$FRONTEND_IMAGE" --revision-suffix="$DEPLOY_ID" --no-traffic --quiet
FRONTEND_REVISION="$(gcloud run services describe "$FRONTEND" --project="$PROJECT_ID" --region="$REGION" --format='value(status.latestReadyRevisionName)')"
[[ "$FRONTEND_REVISION" == "${FRONTEND}-${DEPLOY_ID}" ]] || { printf 'The expected frontend revision is not ready.\n' >&2; exit 1; }
gcloud run services update-traffic "$FRONTEND" --project="$PROJECT_ID" --region="$REGION" \
  --to-revisions="${FRONTEND_REVISION}=100" --quiet

STEP='checking deployed frontend assets'
curl --fail --silent --show-error --retry 4 --retry-delay 3 --max-time 90 \
  "$FRONTEND_URL/staff/request-access" > "$WORK_DIR/frontend.html"
ASSET_URL="$(python3 -c '
from html.parser import HTMLParser
from urllib.parse import urljoin
import sys
class Parser(HTMLParser):
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag=="script" and a.get("type")=="module" and a.get("src"): self.src=a["src"]
p=Parser(); p.feed(open(sys.argv[1]).read()); print(urljoin(sys.argv[2]+"/",p.src))
' "$WORK_DIR/frontend.html" "$FRONTEND_URL")"
[[ "$ASSET_URL" == "$FRONTEND_URL"/* ]] || { printf 'Unexpected frontend asset origin.\n' >&2; exit 1; }
curl --fail --silent --show-error --max-time 90 "$ASSET_URL" > "$WORK_DIR/frontend.js"
python3 -c '
import sys
s=open(sys.argv[1]).read()
for marker in ("/api/v1/auth/staff-access-requests","/api/v1/auth/activate-staff",sys.argv[2]):
    assert marker in s, "Deployed frontend is missing "+marker
print("Deployed frontend contains the new request/activation flow and correct backend URL.")
' "$WORK_DIR/frontend.js" "$BACKEND_URL"

STEP='complete'
printf '\nDeployment HTTP checks passed.\nRelease: %s\nBackend revision: %s\nFrontend revision: %s\nFrontend: %s\n' \
  "$RELEASE" "$BACKEND_REVISION" "$FRONTEND_REVISION" "$FRONTEND_URL"
printf 'Receipt and startup logs: %s\n' "$WORK_DIR"
printf 'Next browser check: Login -> Request staff access; administrator review -> issue -> activate -> authorized workspace.\n'
printf 'Database backup covers SQL only; persistent medical-document and offsite recovery checks remain separate.\n'
