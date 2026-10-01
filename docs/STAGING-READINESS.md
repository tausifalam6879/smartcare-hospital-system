# Staging readiness audit

Status: configuration guardrails added; **not yet approved for public deployment**.

## Implemented

- `SPRING_PROFILES_ACTIVE=staging` removes development fallback credentials for database/JWT/webhook configuration.
- Staging startup rejects missing settings, short/known development signing secrets, shared demo account seeding, unsafe CORS origins and unconfirmed persistent document storage.
- Staging exposes only health through Actuator and suppresses health details.
- `.env.*` files are ignored except example templates. This does not remove secrets already tracked or in history; no history rewrite was performed.
- Local Docker configuration remains unchanged. Payment provider stays DEVELOPMENT: no real payment capability is implied.

## Required before deployment

1. Select staging project, region, hostname, frontend origin and a budget with the owner. No cloud resources have been created.
2. Inject unique secrets outside Git. Set DB_URL, DB_USERNAME, DB_PASSWORD, SMARTCARE_JWT_ISSUER, SMARTCARE_JWT_SECRET, SMARTCARE_PAYMENT_WEBHOOK_SECRET and SMARTCARE_CORS_ORIGINS. Origins must be explicit HTTPS origins with no path/trailing slash.
3. Keep SMARTCARE_DEMO_DATA=false and use a fresh staging database without known demo accounts. Disabling seeding does not delete existing demo accounts. Do not upload the local QA database as production data.
4. Provision durable private document storage and set SMARTCARE_DOCUMENT_STORAGE_PATH. Set SMARTCARE_PERSISTENT_DOCUMENT_STORAGE_CONFIRMED=true only after verifying the mounted storage survives replacement/restart. This acknowledgement is not an automated durability check. Current document implementation is filesystem-based, not an object-storage adapter.
5. Put database and ML service behind private networking. Configure HTTPS ingress and the exact public frontend API origin. Do not expose local compose ports as a production design.
6. Build the explicit non-root target: `docker build --target staging -t smartcare-backend:staging ./backend`. Grant that runtime user's write access on the new persistent document volume. The default local target intentionally preserves existing local volume behavior. Never mount the active local medical-document volume into an experimental staging container or assume image directory permissions override a mount.
7. Provision the initial administrator securely, then disable bootstrap and remove bootstrap password from deployment configuration. Never publish demo staff passwords as staging credentials.
8. Establish encrypted offsite database + document backups and restore into an isolated staging environment. Existing local Windows-user-bound backup is not offsite recovery.
9. Authentication/AI rate limiting is currently process-local. Do not scale to multiple application instances assuming shared enforcement. Review shared limiting, database pool limits and scheduling behavior before scaling.
10. Run cloud-specific health, CORS, registration, invitation, booking, demo payment, check-in, consultation, result release and private download/access-denial checks after deployment. Local green tests are not cloud evidence.

The remaining desktop translation/visual review is separate from these deployment gates. Google Cloud product selection and commands should be verified against current official documentation when the hosting setup is chosen.
