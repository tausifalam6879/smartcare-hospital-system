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
RESUME_REVISION=''
if [[ $# -gt 0 ]]; then
  if [[ $# -ne 2 || "$1" != '--resume-backend' ]]; then
    printf 'Usage: bash infra/deploy-staging.sh [--resume-backend REVISION]\n' >&2; exit 1
  fi
  if [[ "$2" =~ ^smartcare-backend-r([0-9a-f]{12})-([0-9a-f]{6})$ ]]; then
    RESUME_REVISION="$2"; RELEASE="${BASH_REMATCH[1]}"
  else
    printf 'Invalid backend revision name.\n' >&2; exit 1
  fi
fi
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
    printf 'Previous backend traffic: %s\n' "$BACKEND_OLD_REVISION" >&2
  fi
  if [[ -n "$FRONTEND_OLD_REVISION" ]]; then
    printf 'Previous frontend traffic: %s\n' "$FRONTEND_OLD_REVISION" >&2
  fi
  printf 'Do not delete migration V23 or restore over the active database to retry this deployment.\n' >&2
  exit "$exit_code"
}
trap on_error ERR

for tool in gcloud git python3 curl; do command -v "$tool" >/dev/null || { printf 'Missing required command: %s\n' "$tool" >&2; exit 1; }; done
DEPLOY_ID="r${RELEASE}-$(python3 -c 'import secrets; print(secrets.token_hex(3))')"
if [[ -n "$RESUME_REVISION" ]]; then DEPLOY_ID="${RESUME_REVISION#${BACKEND}-}"; fi
FRONTEND_DEPLOY_ID="r${RELEASE}-$(python3 -c 'import secrets; print(secrets.token_hex(3))')"
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
read_serving_traffic() {
  python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(",".join(t["revisionName"]+"="+str(t["percent"]) for t in d["status"].get("traffic",[]) if int(t.get("percent",0))>0))' "$1"
}
BACKEND_OLD_REVISION="$(read_serving_traffic "$WORK_DIR/backend-before.json")"
FRONTEND_OLD_REVISION="$(read_serving_traffic "$WORK_DIR/frontend-before.json")"

# A zero-traffic revision can be Ready but Retired. Check its exact resource,
# then activate it through a tagged URL before trusting startup or schema readiness.
check_revision() {
  gcloud run revisions describe "$1" --project="$PROJECT_ID" --region="$REGION" \
    --format='json(metadata.name,spec.containers[0].image,status.conditions)' > "$WORK_DIR/$1.json"
  python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["metadata"]["name"]==sys.argv[2]; assert any(c.get("type")=="Ready" and c.get("status")=="True" for c in d.get("status",{}).get("conditions",[])), "Exact revision is not Ready"; assert d["spec"]["containers"][0]["image"] in sys.argv[3:], "Revision image does not match the selected release"' "$WORK_DIR/$1.json" "$1" "$2" "$3"
}
candidate_url() {
  local service="$1" revision="$2" tag="verify-${2#${1}-}"
  gcloud run services update-traffic "$service" --project="$PROJECT_ID" --region="$REGION" \
    --update-tags="${tag}=${revision}" --quiet >&2
  gcloud run services describe "$service" --project="$PROJECT_ID" --region="$REGION" --format='json(status.traffic)' |
    python3 -c 'import json,sys; d=json.load(sys.stdin); t=next(t for t in d["status"]["traffic"] if t.get("tag")==sys.argv[1]); assert t.get("revisionName")==sys.argv[2], "Candidate tag changed unexpectedly"; print(t["url"])' "$tag" "$revision"
}

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
if [[ -z "$RESUME_REVISION" ]] && ! gcloud artifacts docker images describe "$BACKEND_IMAGE" --project="$PROJECT_ID" --format='value(image_summary.digest)' >/dev/null 2>&1; then
  # Use the same Dockerfile/default target as the existing backend image.
  # Runtime environment, mounts, Cloud SQL connector and secrets are preserved by image-only updates.
  gcloud builds submit "$ROOT_DIR/backend" --project="$PROJECT_ID" --tag="$BACKEND_IMAGE" --quiet
fi
STEP='building frontend'
if [[ -z "$RESUME_REVISION" ]] && ! gcloud artifacts docker images describe "$FRONTEND_IMAGE" --project="$PROJECT_ID" --format='value(image_summary.digest)' >/dev/null 2>&1; then
  gcloud builds submit "$ROOT_DIR/frontend" --project="$PROJECT_ID" --config="$ROOT_DIR/frontend/cloudbuild.yaml" \
    --substitutions="_API_URL=${BACKEND_URL},_IMAGE=${FRONTEND_IMAGE}" --quiet
fi

BACKEND_DIGEST="$(gcloud artifacts docker images describe "$BACKEND_IMAGE" --project="$PROJECT_ID" --format='value(image_summary.digest)')"
FRONTEND_DIGEST="$(gcloud artifacts docker images describe "$FRONTEND_IMAGE" --project="$PROJECT_ID" --format='value(image_summary.digest)')"
[[ "$BACKEND_DIGEST" =~ ^sha256:[0-9a-f]{64}$ && "$FRONTEND_DIGEST" =~ ^sha256:[0-9a-f]{64}$ ]] || { printf 'Release image digests could not be resolved.\n' >&2; exit 1; }

# All builds must succeed before changing traffic or applying the forward migration.
STEP='creating pre-migration database backup'
if [[ -z "$RESUME_REVISION" ]]; then
  gcloud sql backups create --instance="$DB_INSTANCE" --project="$PROJECT_ID" \
    --description="Before staff onboarding ${DEPLOY_ID}" --quiet
else
  printf 'Resume mode: reusing release images and the original SQL backup; no backend redeployment.\n'
fi
gcloud sql backups list --instance="$DB_INSTANCE" --project="$PROJECT_ID" \
  --filter="description='Before staff onboarding ${DEPLOY_ID}'" --sort-by='~startTime' --limit=1 \
  --format='json(id,status,type,startTime)' > "$WORK_DIR/database-backup.json"
python3 -c 'import json,sys; b=json.load(open(sys.argv[1])); assert b and b[0]["status"]=="SUCCESSFUL", "Pre-migration backup did not succeed"; print("Pre-migration SQL backup verified.")' "$WORK_DIR/database-backup.json"

STEP='deploying backend and applying Flyway migration V23'
if [[ -z "$RESUME_REVISION" ]]; then
  gcloud run services update "$BACKEND" --project="$PROJECT_ID" --region="$REGION" \
    --image="${IMAGE_ROOT}/backend@${BACKEND_DIGEST}" --revision-suffix="$DEPLOY_ID" --no-traffic --quiet
fi
BACKEND_REVISION="${BACKEND}-${DEPLOY_ID}"
check_revision "$BACKEND_REVISION" "$BACKEND_IMAGE" "${IMAGE_ROOT}/backend@${BACKEND_DIGEST}"
BACKEND_CANDIDATE_URL="$(candidate_url "$BACKEND" "$BACKEND_REVISION")"
curl --fail --silent --show-error --retry 4 --retry-delay 3 --max-time 90 \
  "$BACKEND_CANDIDATE_URL/actuator/health" > "$WORK_DIR/backend-health.json"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1])).get("status")=="UP", "Backend is not healthy"' "$WORK_DIR/backend-health.json"

# An invalid request verifies the new public route/DTO without creating a person or granting access.
STEP='checking the new backend request route'
STATUS="$(curl --silent --show-error --max-time 60 -o "$WORK_DIR/request-validation.json" -w '%{http_code}' \
  -H 'Content-Type: application/json' --data '{}' "$BACKEND_CANDIDATE_URL/api/v1/auth/staff-access-requests")"
[[ "$STATUS" == '400' ]] || { printf 'Expected HTTP 400 for an empty staff request; got %s. Frontend remains unchanged.\n' "$STATUS" >&2; exit 1; }
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert "hospitalId" in d.get("errors",{}), "New staff request validation was not found"' "$WORK_DIR/request-validation.json"
gcloud logging read "resource.type=\"cloud_run_revision\" AND resource.labels.service_name=\"${BACKEND}\" AND resource.labels.revision_name=\"${BACKEND_REVISION}\" AND (textPayload:\"Successfully applied\" OR textPayload:\"Current version of schema\" OR jsonPayload.message:\"Successfully applied\" OR jsonPayload.message:\"Current version of schema\")" \
  --project="$PROJECT_ID" --freshness=30d --limit=100 --order=desc \
  --format='value(textPayload,jsonPayload.message)' > "$WORK_DIR/backend-startup.log"
if grep -Ei 'Successfully applied.*version v?23([^0-9]|$)|Current version of schema.*[^0-9]23([^0-9]|$)' "$WORK_DIR/backend-startup.log"; then
  printf 'Flyway startup evidence is retained in %s/backend-startup.log\n' "$WORK_DIR"
else
  printf 'V23 success evidence is not visible yet. Main traffic remains unchanged. Retain diagnostics and verify Flyway history before resuming.\n' >&2
  exit 1
fi

STEP='deploying frontend'
gcloud run services update "$FRONTEND" --project="$PROJECT_ID" --region="$REGION" \
  --image="${IMAGE_ROOT}/frontend@${FRONTEND_DIGEST}" --revision-suffix="$FRONTEND_DEPLOY_ID" --no-traffic --quiet
FRONTEND_REVISION="${FRONTEND}-${FRONTEND_DEPLOY_ID}"
check_revision "$FRONTEND_REVISION" "$FRONTEND_IMAGE" "${IMAGE_ROOT}/frontend@${FRONTEND_DIGEST}"
FRONTEND_CANDIDATE_URL="$(candidate_url "$FRONTEND" "$FRONTEND_REVISION")"

STEP='checking deployed frontend assets'
check_frontend() {
local url="$1"
curl --fail --silent --show-error --retry 4 --retry-delay 3 --max-time 90 \
  "$url/staff/request-access" > "$WORK_DIR/frontend.html"
ASSET_URL="$(python3 -c '
from html.parser import HTMLParser
from urllib.parse import urljoin
import sys
class Parser(HTMLParser):
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        if tag=="script" and a.get("type")=="module" and a.get("src"): self.src=a["src"]
p=Parser(); p.feed(open(sys.argv[1]).read()); print(urljoin(sys.argv[2]+"/",p.src))
' "$WORK_DIR/frontend.html" "$url")"
[[ "$ASSET_URL" == "$url"/* ]] || { printf 'Unexpected frontend asset origin.\n' >&2; return 1; }
curl --fail --silent --show-error --max-time 90 "$ASSET_URL" > "$WORK_DIR/frontend.js"
python3 -c '
import sys
s=open(sys.argv[1]).read()
for marker in ("/api/v1/auth/staff-access-requests","/api/v1/auth/activate-staff",sys.argv[2]):
    assert marker in s, "Deployed frontend is missing "+marker
print("Deployed frontend contains the new request/activation flow and correct backend URL.")
' "$WORK_DIR/frontend.js" "$BACKEND_URL"
}
check_frontend "$FRONTEND_CANDIDATE_URL"

STEP='routing verified backend traffic'
gcloud run services update-traffic "$BACKEND" --project="$PROJECT_ID" --region="$REGION" \
  --to-revisions="${BACKEND_REVISION}=100" --quiet
curl --fail --silent --show-error --retry 4 --retry-delay 3 --max-time 90 \
  "$BACKEND_URL/actuator/health" > "$WORK_DIR/backend-health.json"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1])).get("status")=="UP", "Backend is not healthy"' "$WORK_DIR/backend-health.json"
STEP='routing verified frontend traffic'
gcloud run services update-traffic "$FRONTEND" --project="$PROJECT_ID" --region="$REGION" \
  --to-revisions="${FRONTEND_REVISION}=100" --quiet
check_frontend "$FRONTEND_URL"
for service in "$BACKEND" "$FRONTEND"; do
  revision="$BACKEND_REVISION"; [[ "$service" == "$FRONTEND" ]] && revision="$FRONTEND_REVISION"
  if ! gcloud run services update-traffic "$service" --project="$PROJECT_ID" --region="$REGION" \
    --remove-tags="verify-${revision#${service}-}" --quiet; then
    printf 'Warning: release checks passed, but temporary verification-tag cleanup failed for %s.\n' "$service" >&2
  fi
done

STEP='complete'
printf '\nDeployment HTTP checks passed.\nRelease: %s\nBackend revision: %s\nFrontend revision: %s\nFrontend: %s\n' \
  "$RELEASE" "$BACKEND_REVISION" "$FRONTEND_REVISION" "$FRONTEND_URL"
printf 'Receipt and startup logs: %s\n' "$WORK_DIR"
printf 'Next browser check: Login -> Request staff access; administrator review -> issue -> activate -> authorized workspace.\n'
printf 'Database backup covers SQL only; persistent medical-document and offsite recovery checks remain separate.\n'
