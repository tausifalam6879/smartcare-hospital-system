# RaahMediQ Health — Intelligent Hospital Navigation, Queue & Patient Care Platform

RaahMediQ Health is a production-style academic foundation for hospital navigation, fair queue management, and coordinated patient care. It uses a modular Spring Boot monolith so booking, cancellation, capacity release, and waitlist promotion can share one reliable transaction.

## Delivered in Phase 1

- Java **17** compilation target with Spring Boot 3.5.16 and Maven.
- Patient registration and login with BCrypt, signed short-lived JWTs, and all nine requested roles.
- Patient UUID plus a separate human-facing patient number.
- Public hospital, department, doctor, and schedule directory.
- Admin-protected hospital/doctor management operations.
- PostgreSQL schema managed by Flyway, optimistic entity versions, constraints, and useful indexes.
- Append-only audit events and request correlation IDs.
- Responsive React patient shell using Tailwind CSS 4 through its Vite plugin.
- Working directory search, registration/login, protected patient dashboard, and transparent phase labels.
- Docker Compose, health checks, Java 17 container, environment-based secrets, and CI.

## Delivered in Phase 2

- Real authenticated OPD booking backed by a doctor-day capacity ledger and pessimistic database locking.
- Idempotent booking commands, unique doctor/date/position constraints, and no double-issued number.
- Online-verification and cash-confirmation pending states with configurable expiry deadlines.
- FIFO waitlisting when capacity is full and automatic promotion after cancellation or expiry.
- Patient-owned booking history and cancellation in the React/Tailwind My Care dashboard.
- Local-only opt-in sample directory, navigation, diagnostic catalogue, and synthetic blood-bank inventory for development; production keeps demo data disabled.

## Delivered in Phase 3

- Separate payment, refund, and deduplicated webhook-event ledgers with Flyway constraints and indexes.
- Idempotent online payment-intent creation tied to the owning patient and appointment.
- HMAC-signed webhook verification; browser redirects never confirm payment.
- Verified payment confirmation, late-success refund requests, duplicate-event protection, and receipt numbers.
- Cashier-only cash confirmation with a durable payment record and receipt.
- Configurable confirmed-appointment cancellation cutoff and automatic refund-pending workflow.
- A development gateway adapter that creates references but never pretends money was collected or refunded.

## Delivered in Phase 4

- Audited mobile/QR/reception/kiosk check-in with configurable hospital-time-zone windows and idempotent repeat handling.
- Privacy-preserving live queue snapshots: public responses expose only OPD position/private tokens, never patient names.
- Staff-authorized `serve-next` transitions for checked-in patients, completion state, patients-ahead calculation, and clearly labelled wait estimates.
- Durable in-app notification inbox with unread state, event deduplication, delivery-attempt records, and provider adapter isolation.
- Development external-channel adapter records `SKIPPED_NOT_CONFIGURED` instead of claiming an email/SMS/WhatsApp/push/voice delivery.
- Responsive React/Tailwind live-queue screen with 15-second refresh fallback, check-in action, private token, location, notification badge, and notification inbox.

## Delivered in Phase 5

- Hospital-scoped indoor location graph for entrances, registration, lifts, stairs, reception, rooms, labs, pharmacy, emergency, and exits.
- Public, non-identifying QR checkpoints that establish a verified current location without pretending indoor GPS is reliable.
- Deterministic shortest-route calculation with a step-free option that excludes stairs and returns “unavailable” instead of inventing directions.
- English/Hindi route instructions with an API structure that can add more hospital-approved translations later.
- Patient-owned appointment-to-room resolution using the doctor’s exact verified building, floor, and room.
- Responsive React/Tailwind QR assistant with large controls, destination icons, route summary, simple map sketch, and appointment/live-queue entry points.

## Delivered in Phase 6

- Patient-owned longitudinal record for doctor-finalized visits, documented symptoms, diagnoses, notes, discharge summaries, follow-up guidance, prescriptions, medicines, and active allergies.
- Appointment-bound clinician entry: a linked doctor account can finalize a clinical visit only for its assigned real appointment and only during/after consultation.
- Private PDF/JPG/PNG uploads with a 10 MB limit, server-side file-signature detection, SHA-256 integrity metadata, opaque storage keys, safe filenames, and no public document URL.
- Object-level authorization for every record read and download: patients access only their own data; doctors require a linked account and a real care relationship.
- Append-only audit events for sensitive record reads, clinician writes, uploads, and downloads.
- Responsive React/Tailwind My Health Record page with allergy warnings, consultation timeline, structured medicine cards, report upload, and authorized download.
- Replaceable private-storage port with a persistent local/Docker adapter for development. Production still requires a managed private object store and malware scanning.

## Delivered in Phase 7

- Patient-owned care-assistant conversations with durable private message history.
- Mandatory patient-ID database predicate before chunk ranking; another patient's chunks never enter the ranking set.
- Lazy indexing of clinician-finalized visits, prescriptions, allergies, follow-up notes, and uploaded report metadata/content.
- Secure text extraction for text PDFs using Apache PDFBox 3.0.8, page-aware chunking, local hashed embeddings, patient-scoped retrieval, and source/page citations.
- Explicit OCR-required/no-text/encrypted/failed extraction states. JPG/PNG and image-only PDFs are never guessed.
- Deterministic local grounded-answer mode with no external medical-data sharing, plus emergency escalation, diagnosis/prescription boundaries, unsupported-evidence responses, and per-user rate limiting.
- Audited assistant queries and conversation reads without copying questions, answers, report text, or diagnosis content into audit payloads.
- Responsive React/Tailwind Care Assistant with conversation history, quick questions, cited source cards, safety states, and direct health-record links.

## Delivered in Phase 8

- Hospital diagnostic catalogue for laboratory, MRI, CT, and X-ray services with preparation instructions, location, fee, turnaround, duration, and daily capacity.
- Appointment-bound clinician orders: only the assigned linked doctor can order a test during or after a real consultation.
- Patient-owned scheduling with pessimistically locked procedure/day ledgers, monotonic diagnostic queue positions, capacity enforcement, and safe cancellation release.
- Role-protected diagnostic worklist transitions for arrival/sample collection, processing start, and atomic staff verification.
- Structured verified results with overall flags and ordered result items; draft or unverified findings are never returned as final patient results.
- Object-level patient isolation and append-only audits for order, scheduling, worklist, and result actions without copying result content into audit payloads.
- Responsive React/Tailwind Diagnostics page for scheduling, preparation, hospital location, queue number, progress, and staff-verified result review.

## Delivered in Phase 9

- Authorized hospital and partner blood-bank directory with distance, transfer-time, contact, and source provenance.
- Batch-level blood inventory for ABO/Rh group and component, with expiry, total/reserved units, verifier provenance, and a configurable six-hour freshness window.
- Exact group/component availability search that ranks only recently verified, unexpired inventory and labels stale or missing verification instead of inventing stock.
- Idempotent clinician/staff emergency requests with patient ownership, optional appointment context, automatic FEFO allocation, partial reservation, fulfilment, cancellation release, and durable status notifications.
- Pessimistic inventory locks plus database checks prevent concurrent requests from reserving more units than verified stock.
- Explicit voluntary donor consent, staff-only blood-group/eligibility verification, private matching only after verified inventory is insufficient, and immediate consent withdrawal.
- Responsive React/Tailwind Blood Support page for verified availability, own request tracking, safe contact guidance, and donor consent controls.

## Delivered in Phase 10

- Hospital ambulance fleet with registration/call sign, crew assignment label, operational status, approximate area, optional exact dispatcher-only coordinates, and synthetic-data provenance.
- Idempotent patient and authorized staff transport requests for patient or blood transport, with hospital destination, pickup details, priority, and explicit dispatcher approval.
- Pessimistically locked vehicle assignment prevents double-dispatch; a request never assigns a vehicle automatically.
- Strict request and vehicle state machines cover assignment, crew acknowledgement, pickup, hospital transit, arrival, completion, cancellation, out-of-service, and safe vehicle release.
- Patient-owned request history exposes only the assigned vehicle identity and coarse shared area; dispatcher-only fleet views retain crew contact and exact coordinates.
- Append-only dispatch timeline, patient notifications, and audit records for request, assignment, acknowledgement, every operational transition, cancellation, fleet status, and location updates.
- Responsive React/Tailwind ambulance page with a patient request flow, dispatcher worklist, fleet readiness, synthetic-demo warnings, and emergency-service escalation.

## Delivered in Phase 11

- Role-aware hospital operations dashboard for appointment flow, doctor availability, queues, waitlists, payments, no-shows, diagnostics, blood alerts, ambulance readiness, recorded wait, and super-admin notification failures.
- Doctor-day operational states for on-time, 30/60-minute delay, emergency interruption, temporary unavailability, and cancellation for the day, with durable patient notifications.
- Doctor delays are included in patient live-queue estimates with an explicit approximate-estimate warning.
- Sudden doctor cancellation creates a patient-owned recovery case; RaahMediQ Health never silently changes the doctor or date.
- Patient-approved same-doctor rescheduling, priority future queue, eligible-doctor backend transfer, or cancellation with refund review. New capacity is locked and rechecked before an OPD position is issued.
- Staff no-show control releases capacity and promotes the fair waitlist; doctor accounts are restricted to their linked doctor.
- Responsive React/Tailwind recovery and operations views with explicit refund-review and emergency boundaries.

## Delivered in Phase 12

- Private patient submission of actual decodable JPG/PNG blood-slide images with acknowledgement, 10 MB limit, minimum dimensions, pixel-limit protection, magic-byte validation, opaque storage keys, SHA-256 metadata, and authorized no-store image access.
- Pluggable Java `BloodSlideAnalyzer` port. The development adapter reports `NOT_CONFIGURED` and never fabricates an AI blood-group suggestion.
- Explicit Anti-A, Anti-B, and Anti-D observation workflow with a deterministic reaction-to-ABO/Rh mapping.
- Maker-checker verification: the staff member recording observations cannot independently verify the result.
- Patient-owned submission history, lab/blood-bank worklist, rejection flow, durable notifications, and sensitive-operation audit events.
- Responsive React/Tailwind patient and staff views that distinguish experimental model suggestions, preliminary reactions, and independently laboratory-verified results.

## Prototype experience additions

- Compact India hospital picker covering all 28 states and 8 union territories with 70 government/private project entries; the list and doctor profiles stay inside RaahMediQ Health and never redirect to hospital websites.
- Scrollable hospital and doctor panels, mobile overflow protection, and an explicitly synthetic Ranchi profile for Dr. Prakash Chandra based only on project assumptions supplied for the prototype.
- Expanded demo care centre with 10 departments, 10 operational doctors, 10 diagnostic procedures, and role-specific demo staff accounts when `RAAHMEDIQ_DEMO_DATA=true`.
- Plain-language Dijkstra route explanation and legend, clickable diagnostic preparation guides, and role-aware staff task inboxes for dispatcher, laboratory, and blood-bank work.
- Safer simulated online-payment walkthrough: review, provider selection, authorization buffer, and callback-pending status. It never asks for a UPI PIN and never claims that real money was collected.
- Ambulance request next-step guidance, automatic status refresh, dispatcher call/action controls, and explicit task ownership so requests do not appear to wait without explanation.

Not yet implemented: a clinically validated ONNX/OpenCV blood-slide model and governed training/evaluation dataset, LIMS-backed blood-group confirmation, production image-retention/deletion lifecycle, production map-authoring/review UI, physical QR printing lifecycle, optional beacon adapter, a production gateway-specific checkout adapter/credentials, WebSocket/SSE transport, automatic end-of-day rollover without a patient decision, approved OCR pipeline, approved external/on-prem LLM adapter, production vector database, real dispatch-provider/GPS/driver-app integration, production object-storage adapter, malware scanner, diagnostic payment integration, trusted external blood-bank adapters, private donor-contact dispatch, and production hospital-scoped staff assignments. Their safety/consistency design is documented under [`docs/`](docs/).

## Architecture documents

- [System architecture](docs/architecture.md)
- [Database and entity model](docs/database-design.md)
- [Queue, rollover, payment, and recovery algorithms](docs/queue-algorithm.md)
- [REST API map](docs/api-documentation.md)
- [Verified indoor navigation design](docs/navigation-design.md)
- [Private medical-record design](docs/medical-record-design.md)
- [Security and privacy baseline](docs/security.md)
- [Patient-isolated RAG design](docs/rag-design.md)
- [Diagnostic workflow design](docs/diagnostic-workflow.md)
- [Blood-bank workflow design](docs/blood-bank-design.md)
- [Ambulance coordination design](docs/ambulance-coordination-design.md)
- [Hospital operations design](docs/hospital-operations-design.md)
- [Blood-group image decision-support design](docs/blood-group-image-design.md)
- [Project directory structure](docs/project-structure.md)

## Prerequisites

- JDK 17 or newer. Maven always compiles with `--release 17`.
- Node.js 20.19+ or 22.12+ (Node 22.22 is used in Docker/CI).
- PostgreSQL 14+ for local non-Docker development.
- Docker Compose is optional but is the simplest full-stack startup.

## Run with Docker Compose

1. Copy `.env.example` to `.env`.
2. Replace `POSTGRES_PASSWORD` and `RAAHMEDIQ_JWT_SECRET` with long random values.
3. Optionally enable the one-time super-admin bootstrap and provide its values.
4. Start the stack:

```bash
docker compose up --build
```

Open `http://localhost:5173` or `http://127.0.0.1:5173`. Both local frontend origins are allowed by the backend. The API runs at `http://localhost:8080`; health is available at `/actuator/health`.

After the first administrator is created, set `RAAHMEDIQ_BOOTSTRAP_ADMIN=false` and restart. Existing accounts are never overwritten by the bootstrap.

## Run locally

Start PostgreSQL and create a `raahmediq` database, then set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a 32+ character `RAAHMEDIQ_JWT_SECRET`.

Backend on Windows PowerShell:

```powershell
cd backend
.\mvnw.ps1 spring-boot:run
```

Backend on Linux/macOS:

```bash
cd backend
sh mvnw spring-boot:run
```

Frontend in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` and `/actuator` to the backend during local development.

## Verify

```powershell
cd backend
.\mvnw.ps1 test

cd ..\frontend
npm test
npm run build
```

The committed Maven wrapper pins Maven 3.9.16 and verifies its SHA-256 checksum. The npm lockfile pins the frontend dependency graph.

## First API calls

Register a patient:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "mobileNumber": "+919876543210",
  "email": "asha@example.com",
  "name": "Asha Rao",
  "password": "a-long-unique-password",
  "preferredLanguage": "hi"
}
```

Use the returned token as `Authorization: Bearer <token>`. Public directory calls do not require authentication:

```text
GET /api/v1/hospitals
GET /api/v1/hospitals/{hospitalId}/departments
GET /api/v1/doctors?hospitalId={hospitalId}&search=oncology
GET /api/v1/doctors/{doctorId}
```

Authenticated Phase 2 booking calls:

```text
GET  /api/v1/appointments/availability?doctorId={doctorId}&date=2026-08-22
POST /api/v1/appointments                   (requires Idempotency-Key)
GET  /api/v1/appointments/mine
POST /api/v1/appointments/{appointmentId}/cancel
POST /api/v1/payments/intents              (requires Idempotency-Key)
GET  /api/v1/payments/mine
POST /api/v1/payments/webhooks/{provider}  (requires signed event headers)
POST /api/v1/check-in/appointments/{appointmentId}
GET  /api/v1/queues/{doctorId}/live?date=2026-08-22
GET  /api/v1/queues/appointments/{appointmentId}
POST /api/v1/queues/{doctorId}/serve-next?date=2026-08-22
GET  /api/v1/notifications
GET  /api/v1/notifications/unread-count
PATCH /api/v1/notifications/{notificationId}/read
PATCH /api/v1/notifications/read-all
GET  /api/v1/navigation/hospitals/{hospitalId}/map
GET  /api/v1/navigation/hospitals/{hospitalId}/route?fromCheckpoint=...&destinationCode=...&language=hi&stepFree=true
GET  /api/v1/navigation/checkpoints/{publicCode}
GET  /api/v1/navigation/appointments/{appointmentId}/destination
GET  /api/v1/medical-records/mine
GET  /api/v1/medical-records/appointments/{appointmentId}/patient
POST /api/v1/medical-records/visits
POST /api/v1/medical-records/documents          (multipart PDF/JPG/PNG, max 10 MB)
GET  /api/v1/medical-records/documents/{documentId}/content
GET  /api/v1/ai/status
POST /api/v1/ai/conversations
GET  /api/v1/ai/conversations
GET  /api/v1/ai/conversations/{conversationId}/messages
POST /api/v1/ai/conversations/{conversationId}/messages
GET  /api/v1/diagnostics/procedures?hospitalId=...
GET  /api/v1/diagnostics/procedures/{procedureId}/availability?date=...
POST /api/v1/diagnostics/orders                 (assigned doctor)
GET  /api/v1/diagnostics/orders/mine
POST /api/v1/diagnostics/orders/{orderId}/schedule
POST /api/v1/diagnostics/orders/{orderId}/cancel
GET  /api/v1/diagnostics/worklist               (diagnostic staff)
POST /api/v1/diagnostics/orders/{orderId}/collect
POST /api/v1/diagnostics/orders/{orderId}/start
POST /api/v1/diagnostics/orders/{orderId}/verify-result
GET  /api/v1/blood-banks?hospitalId=...
GET  /api/v1/blood-banks/availability?hospitalId=...&bloodGroup=O_POSITIVE&component=PACKED_RED_CELLS&units=2
POST /api/v1/blood-banks                       (hospital/super admin)
GET  /api/v1/blood-banks/inventory             (blood-bank staff/admin)
POST /api/v1/blood-banks/inventory             (blood-bank staff/admin verification)
POST /api/v1/blood-requests                     (doctor/blood-bank staff/admin)
GET  /api/v1/blood-requests/mine
GET  /api/v1/blood-requests                     (blood-bank staff/admin worklist)
POST /api/v1/blood-requests/{requestId}/search
POST /api/v1/blood-requests/{requestId}/fulfil
POST /api/v1/blood-requests/{requestId}/cancel
GET  /api/v1/blood-donors/me
POST /api/v1/blood-donors/consent
POST /api/v1/blood-donors/withdraw
POST /api/v1/blood-donors/{donorId}/verify      (blood-bank staff/admin)
GET  /api/v1/blood-requests/{requestId}/donor-matches (blood-bank staff/admin)
GET  /api/v1/ambulances/availability?hospitalId=...
GET  /api/v1/ambulances                         (dispatcher/admin fleet)
POST /api/v1/ambulances                         (hospital/super admin)
POST /api/v1/ambulances/{ambulanceId}/location  (dispatcher/admin)
POST /api/v1/ambulances/{ambulanceId}/availability
POST /api/v1/ambulance-requests                 (patient/authorized staff)
GET  /api/v1/ambulance-requests/mine
GET  /api/v1/ambulance-requests                 (dispatcher/admin worklist)
POST /api/v1/ambulance-requests/{requestId}/assign
POST /api/v1/ambulance-requests/{requestId}/acknowledge
POST /api/v1/ambulance-requests/{requestId}/status
POST /api/v1/ambulance-requests/{requestId}/cancel

GET  /api/v1/operations/dashboard?hospitalId=...&date=...   (staff)
POST /api/v1/operations/doctor-status                       (doctor/reception/admin)
POST /api/v1/operations/appointments/{appointmentId}/no-show
GET  /api/v1/operations/recovery/mine                       (patient)
POST /api/v1/operations/recovery/{caseId}/decision          (patient owner)

POST /api/v1/blood-group-analyses                           (patient JPG/PNG upload)
GET  /api/v1/blood-group-analyses/mine                      (patient owner)
GET  /api/v1/blood-group-analyses/worklist?hospitalId=...   (authorized staff)
GET  /api/v1/blood-group-analyses/{analysisId}/image        (owner/authorized staff)
POST /api/v1/blood-group-analyses/{analysisId}/observations (lab/blood-bank staff)
POST /api/v1/blood-group-analyses/{analysisId}/verify       (independent authorized reviewer)
POST /api/v1/blood-group-analyses/{analysisId}/reject       (authorized reviewer)
```

## Safety notes

This is an academic Phase 12 foundation, not a certified medical device, emergency-dispatch system, or production hospital deployment. The blood-slide module has no configured image-classification model and produces no automatic result; even a future experimental suggestion must never replace validated tube testing, cross-matching, donation eligibility, or independent laboratory verification. A doctor-delay estimate is operational guidance, not a clinical priority or guarantee. Doctor transfers and future dates require the patient's explicit choice; a refund-review state is not a claim that money has been refunded. Submitting an ambulance request does not assign or guarantee a vehicle; users must contact official local emergency services for immediate or life-threatening help. Blood availability is operational information, not a transfusion decision. RaahMediQ Health displays diagnostic values exactly as verified by authorized staff but does not interpret them as a diagnosis or treatment decision. Its local grounded assistant never diagnoses, prescribes, changes clinician advice, or decides that an emergency can wait. The development adapters require accredited integrations, hospital-scoped staff grants, MFA, encryption/key management, backup, retention, and legal/clinical governance before real use.
