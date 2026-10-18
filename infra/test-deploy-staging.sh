#!/usr/bin/env bash
# Offline regression tests: every gcloud/curl/git call is a shell mock.
set -Eeuo pipefail
TEST_SCRIPT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)/test-deploy-staging.sh"
TARGET="${TEST_SCRIPT%/*}/deploy-staging.sh"
if [[ "${1:-}" != --scenario ]]; then
  RESULTS="$(mktemp -d -t smartcare-deploy-tests-XXXXXXXX)"
  for mode in success resume-success build-failure backup-failure not-ready wrong-image health-failure route-failure migration-missing migration-unapplied frontend-asset-failure resume-missing-images resume-backup-failure; do
    trace="$RESULTS/$mode.trace"
    if MOCK_TRACE="$trace" bash "$TEST_SCRIPT" --scenario "$mode" > "$RESULTS/$mode.log" 2>&1; then actual=0; else actual=$?; fi
    case "$mode" in
      success|resume-success)
        [[ "$actual" == 0 ]] || { printf 'FAIL %s: see %s\n' "$mode" "$RESULTS/$mode.log"; exit 1; }
        [[ "$(grep -c -- '--to-revisions=' "$trace")" == 2 ]] || { printf 'Missing exact traffic assignments\n'; exit 1; } ;;
      *)
        [[ "$actual" != 0 ]] || { printf 'Unexpected success: %s\n' "$mode"; exit 1; }
        if grep -q -- '--to-revisions=' "$trace"; then printf 'Failure changed main traffic: %s\n' "$mode"; exit 1; fi ;;
    esac
    if [[ "$mode" == resume-* ]] && grep -Eq '^gcloud (builds submit|sql backups create|run services update smartcare-backend )' "$trace"; then
      printf 'Resume rebuilt/redeployed backend or created another backup\n'; exit 1
    fi
    if grep -q 'value(status.latestReadyRevisionName)' "$trace"; then printf 'Used stale service pointer\n'; exit 1; fi
    printf 'PASS %s\n' "$mode"
  done
  printf 'All 13 offline scenarios passed. Diagnostics: %s\n' "$RESULTS"
  exit 0
fi
MODE="$2"
MOCK_BACKEND_BUILT=false
MOCK_FRONTEND_BUILT=false
if [[ "$MODE" == resume-* ]]; then MOCK_BACKEND_BUILT=true; MOCK_FRONTEND_BUILT=true; fi
MOCK_PYTHON="$(type -P python3 || type -P python)"
python3() { "$MOCK_PYTHON" "$@"; }
git() {
  case "$*" in
    *rev-parse*) printf '123456abcdef\n' ;;
    *status*) return 0 ;;
    *) printf 'Unexpected mock git call\n' >&2; return 99 ;;
  esac
}
gcloud() {
  printf 'gcloud %s\n' "$*" >> "$MOCK_TRACE"
  case "$*" in
    'auth list '*) printf 'synthetic@example.invalid\n' ;;
    'artifacts repositories describe '*) printf 'smartcare\n' ;;
    'artifacts docker images describe '*)
      if [[ "$MODE" == resume-missing-images ]]; then return 21; fi
      if [[ "$*" == *'/backend:'* && "$MOCK_BACKEND_BUILT" == false ]]; then return 1; fi
      if [[ "$*" == *'/frontend:'* && "$MOCK_FRONTEND_BUILT" == false ]]; then return 1; fi
      printf 'sha256:%064d\n' 1 ;;
    'sql instances describe '*) printf 'RUNNABLE\n' ;;
    'builds submit '*)
      if [[ "$MODE" == build-failure ]]; then return 23; fi
      if [[ "$*" == *'/backend '* ]]; then MOCK_BACKEND_BUILT=true; else MOCK_FRONTEND_BUILT=true; fi ;;
    'sql backups create '*) return 0 ;;
    'sql backups list '*)
      if [[ "$MODE" == *backup-failure ]]; then printf '[{"id":1,"status":"FAILED"}]\n'; else printf '[{"id":1,"status":"SUCCESSFUL"}]\n'; fi ;;
    'run revisions describe '*)
      local revision="$4" image="$BACKEND_IMAGE" ready=True
      [[ "$revision" == smartcare-frontend-* ]] && image="$FRONTEND_IMAGE"
      [[ "$MODE" == wrong-image ]] && image='wrong-release'
      [[ "$MODE" == not-ready ]] && ready=False
      printf '{"metadata":{"name":"%s"},"spec":{"containers":[{"image":"%s"}]},"status":{"conditions":[{"type":"Ready","status":"%s","reason":"Retired"},{"type":"Active","status":"False"}]}}\n' "$revision" "$image" "$ready" ;;
    'run services describe '*)
      local service="$4"
      if [[ "$*" == *'--format=json(status.traffic)'* ]]; then
        printf '{"status":{"traffic":[{"tag":"%s","revisionName":"%s","url":"https://%s.candidate.example.invalid"}]}}\n' "$MOCK_TAG" "$MOCK_REVISION" "$service"
      elif [[ "$*" == *'--format=json' && "$*" != *'--format=json('* ]]; then
        printf '{"spec":{"template":{"spec":{"containers":[{"env":[{"name":"SPRING_PROFILES_ACTIVE","value":"staging"}]}]}}}}\n'
      else
        # Deliberately stale latestReady pointer reproduces the real zero-traffic failure.
        printf '{"status":{"url":"https://%s.example.invalid","latestReadyRevisionName":"%s-old","traffic":[{"revisionName":"%s-old","percent":100}]}}\n' "$service" "$service" "$service"
      fi ;;
    'run services update-traffic '*)
      local arg assignment
      for arg in "$@"; do
        if [[ "$arg" == --update-tags=* ]]; then assignment="${arg#*=}"; MOCK_TAG="${assignment%%=*}"; MOCK_REVISION="${assignment#*=}"; fi
      done ;;
    'run services update '*) return 0 ;;
    'logging read '*)
      if [[ "$MODE" == migration-missing ]]; then return 0; fi
      if [[ "$MODE" == migration-unapplied ]]; then printf 'Migrating schema public to version 23\n'; else printf 'Successfully applied 1 migration to schema public, now at version v23\n'; fi ;;
    *) printf 'Unexpected mock gcloud call: %s\n' "$*" >&2; return 99 ;;
  esac
}
curl() {
  printf 'curl %s\n' "$*" >> "$MOCK_TRACE"
  local output='' url="${!#}" arg previous=''
  for arg in "$@"; do [[ "$previous" == -o ]] && output="$arg"; previous="$arg"; done
  case "$url" in
    */actuator/health)
      if [[ "$MODE" == health-failure ]]; then printf '{"status":"DOWN"}\n'; else printf '{"status":"UP"}\n'; fi ;;
    */api/v1/auth/staff-access-requests)
      printf '{"errors":{"hospitalId":"required"}}\n' > "$output"
      if [[ "$MODE" == route-failure ]]; then printf 404; else printf 400; fi ;;
    */staff/request-access) printf '<script type="module" src="/assets/main.js"></script>\n' ;;
    */assets/main.js)
      if [[ "$MODE" == frontend-asset-failure ]]; then printf 'old assets'; else printf '/api/v1/auth/staff-access-requests /api/v1/auth/activate-staff %s\n' "$BACKEND_URL"; fi ;;
    *) printf 'Unexpected mock URL\n' >&2; return 99 ;;
  esac
}
if [[ "$MODE" == resume-* ]]; then
  source "$TARGET" --resume-backend smartcare-backend-r123456abcdef-123abc
else
  set --
  source "$TARGET"
fi
