# REST API Map

All routes are versioned under `/api/v1`. JSON uses ISO-8601 dates/times. Errors use RFC 9457-style problem details with a correlation ID. Mutating retry-prone operations will accept `Idempotency-Key`.

## Implemented routes

| Method | Route | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register a patient account |
| `POST` | `/api/v1/auth/login` | Public | Exchange credentials for a JWT |
| `GET` | `/api/v1/auth/me` | Authenticated | Current user summary |
| `GET` | `/api/v1/hospitals` | Public | Active hospital directory |
| `GET` | `/api/v1/hospitals/{hospitalId}` | Public | Hospital with departments |
| `POST` | `/api/v1/hospitals` | Hospital/Super admin | Create hospital |
| `PUT` | `/api/v1/hospitals/{hospitalId}` | Hospital/Super admin | Update hospital |
| `GET` | `/api/v1/hospitals/{hospitalId}/departments` | Public | List departments |
| `POST` | `/api/v1/hospitals/{hospitalId}/departments` | Hospital/Super admin | Create department |
| `GET` | `/api/v1/doctors` | Public | Filter doctors by hospital/department/search |
| `GET` | `/api/v1/doctors/{doctorId}` | Public | Doctor and schedule detail |
| `POST` | `/api/v1/doctors` | Hospital/Super admin | Create doctor |
| `PUT` | `/api/v1/doctors/{doctorId}` | Hospital/Super admin | Update doctor |
| `POST` | `/api/v1/doctors/{doctorId}/schedules` | Hospital/Super admin | Add weekly schedule |
| `DELETE` | `/api/v1/doctors/{doctorId}/schedules/{scheduleId}` | Hospital/Super admin | Remove schedule |
| `GET` | `/actuator/health` | Public | Liveness/health summary |
| `GET` | `/api/v1/appointments/availability` | Public | Doctor-day capacity and waitlist summary |
| `POST` | `/api/v1/appointments` | Patient | Idempotently reserve a position or join waitlist |
| `GET` | `/api/v1/appointments/mine` | Patient | Own booking history |
| `POST` | `/api/v1/appointments/{id}/cancel` | Owning patient | Cancel and atomically release/promote |
| `POST` | `/api/v1/appointments/{id}/cash-confirmation` | Cashier/Hospital/Super admin | Confirm cash before deadline |
| `POST` | `/api/v1/payments/intents` | Owning patient | Idempotently create provider payment intent |
| `GET` | `/api/v1/payments/mine` | Patient | Own payment/refund history |
| `POST` | `/api/v1/payments/webhooks/{provider}` | Signed provider | Deduplicated payment/refund state transition |
| `POST` | `/api/v1/check-in/appointments/{id}` | Owner or authorized staff | Idempotent mobile/QR/reception/kiosk check-in |
| `GET` | `/api/v1/check-in/appointments/{id}` | Owner or authorized staff | Check-in record and privacy token |
| `GET` | `/api/v1/queues/{doctorId}/live?date=...` | Public | Privacy-safe currently-serving snapshot without patient names |
| `GET` | `/api/v1/queues/appointments/{id}` | Owning patient | Own position, patients ahead, and estimated wait |
| `POST` | `/api/v1/queues/{doctorId}/serve-next?date=...` | Doctor/Reception/Hospital/Super admin | Complete current and call next checked-in patient |
| `GET` | `/api/v1/notifications` | Patient | Latest 100 patient-owned notifications |
| `GET` | `/api/v1/notifications/unread-count` | Patient | Notification badge count |
| `PATCH` | `/api/v1/notifications/{id}/read` | Owning patient | Mark one notification read |
| `PATCH` | `/api/v1/notifications/read-all` | Patient | Mark notification inbox read |
| `GET` | `/api/v1/navigation/hospitals/{hospitalId}/map` | Public | Verified indoor locations and QR checkpoints |
| `GET` | `/api/v1/navigation/hospitals/{hospitalId}/route` | Public | Deterministic route from QR checkpoint to destination code |
| `GET` | `/api/v1/navigation/checkpoints/{publicCode}` | Public | Resolve a QR code to its verified hospital location |
| `GET` | `/api/v1/navigation/appointments/{appointmentId}/destination` | Owning patient | Resolve an appointment to an exact verified room |
| `POST` | `/api/v1/navigation/hospitals/{hospitalId}/locations` | Hospital/Super admin | Create an indoor map node |
| `POST` | `/api/v1/navigation/hospitals/{hospitalId}/paths` | Hospital/Super admin | Connect two nodes with bilingual instructions |
| `POST` | `/api/v1/navigation/hospitals/{hospitalId}/checkpoints` | Hospital/Super admin | Attach a public QR checkpoint to a node |
| `POST` | `/api/v1/doctors/{doctorId}/account-link` | Hospital/Super admin | Link one user account to a doctor and grant the doctor role |
| `GET` | `/api/v1/medical-records/mine` | Patient | Own longitudinal visits, prescriptions, allergies, and documents |
| `GET` | `/api/v1/medical-records/appointments/{appointmentId}/patient` | Assigned linked doctor | Appointment patient record after care-context authorization |
| `POST` | `/api/v1/medical-records/visits` | Assigned linked doctor | Finalize an append-only visit for an in-consultation/completed appointment |
| `POST` | `/api/v1/medical-records/documents` | Patient | Upload an own private PDF/JPG/PNG report, maximum 10 MB |
| `GET` | `/api/v1/medical-records/documents/{documentId}/content` | Owner or care-related linked doctor | Audited authorized file download with no-store headers |
| `GET` | `/api/v1/ai/status` | Patient | Grounded-mode privacy and document-support status |
| `POST` | `/api/v1/ai/conversations` | Patient | Create an owned private assistant conversation |
| `GET` | `/api/v1/ai/conversations` | Patient | List only own conversation history |
| `GET` | `/api/v1/ai/conversations/{conversationId}/messages` | Owning patient | Read own assistant messages and citations |
| `POST` | `/api/v1/ai/conversations/{conversationId}/messages` | Owning patient | Ask a rate-limited, safety-checked grounded question |
| `GET` | `/api/v1/diagnostics/procedures?hospitalId=...` | Public | Active hospital diagnostic catalogue with preparation and location |
| `GET` | `/api/v1/diagnostics/procedures/{procedureId}/availability?date=...` | Public | Procedure-day capacity and remaining positions |
| `POST` | `/api/v1/diagnostics/procedures` | Hospital/Super admin | Publish a diagnostic procedure |
| `POST` | `/api/v1/diagnostics/orders` | Assigned linked doctor | Order a hospital procedure during/after consultation |
| `GET` | `/api/v1/diagnostics/orders/mine` | Patient | Own diagnostic orders and staff-verified results |
| `POST` | `/api/v1/diagnostics/orders/{orderId}/schedule` | Owning patient | Lock day capacity and assign a queue position |
| `POST` | `/api/v1/diagnostics/orders/{orderId}/cancel` | Owning patient | Cancel before processing and release scheduled capacity |
| `GET` | `/api/v1/diagnostics/worklist?hospitalId=...&date=...` | Diagnostic staff/admin | Ordered day worklist without public patient names |
| `POST` | `/api/v1/diagnostics/orders/{orderId}/collect` | Diagnostic staff/admin | Record arrival or sample collection |
| `POST` | `/api/v1/diagnostics/orders/{orderId}/start` | Diagnostic staff/admin | Start laboratory/imaging processing |
| `POST` | `/api/v1/diagnostics/orders/{orderId}/verify-result` | Diagnostic staff/admin | Atomically publish one structured verified result |
| `GET` | `/api/v1/blood-banks?hospitalId=...` | Authenticated | Authorized internal and partner blood-bank directory |
| `POST` | `/api/v1/blood-banks` | Hospital/Super admin | Register an authorized blood bank |
| `GET` | `/api/v1/blood-banks/availability?hospitalId=...&bloodGroup=...&component=...&units=...` | Authenticated | Ranked recently verified exact-match availability |
| `GET` | `/api/v1/blood-banks/inventory` | Blood-bank staff/admin | Batch inventory with reserved units and verifier provenance |
| `POST` | `/api/v1/blood-banks/inventory` | Blood-bank staff/admin | Create/reverify/quarantine a batch without reducing below reservations |
| `POST` | `/api/v1/blood-requests` | Doctor/Blood-bank staff/admin | Idempotently create and search an authorized emergency request |
| `GET` | `/api/v1/blood-requests/mine` | Patient | Own request state and authorized allocations only |
| `GET` | `/api/v1/blood-requests?hospitalId=...&status=...` | Blood-bank staff/admin | Hospital request worklist |
| `POST` | `/api/v1/blood-requests/{requestId}/search` | Blood-bank staff/admin | Retry locked allocation against newly verified inventory |
| `POST` | `/api/v1/blood-requests/{requestId}/fulfil` | Blood-bank staff/admin | Fulfil a fully reserved request and consume batch units |
| `POST` | `/api/v1/blood-requests/{requestId}/cancel` | Creating clinician/Blood-bank staff/admin | Cancel and release every active allocation atomically |
| `GET` | `/api/v1/blood-donors/me` | Authenticated | Own donor-consent and verification state |
| `POST` | `/api/v1/blood-donors/consent` | Authenticated | Explicitly opt into private donor consideration |
| `POST` | `/api/v1/blood-donors/withdraw` | Authenticated | Withdraw own donor consent immediately |
| `POST` | `/api/v1/blood-donors/{donorId}/verify` | Blood-bank staff/admin | Record independently verified group and eligibility |
| `GET` | `/api/v1/blood-requests/{requestId}/donor-matches` | Blood-bank staff/admin | Private exact-group eligible matches after inventory shortfall |
| `GET` | `/api/v1/ambulances/availability?hospitalId=...` | Authenticated | Indicative available/active vehicle counts and synthetic-data flag |
| `GET` | `/api/v1/ambulances?hospitalId=...` | Ambulance dispatcher/admin | Hospital fleet with crew and exact dispatcher-only location fields |
| `POST` | `/api/v1/ambulances` | Hospital/Super admin | Register a real operational vehicle; demo records are seeded separately |
| `POST` | `/api/v1/ambulances/{ambulanceId}/location` | Ambulance dispatcher/admin | Update approximate area and exact dispatcher-only coordinates |
| `POST` | `/api/v1/ambulances/{ambulanceId}/availability` | Ambulance dispatcher/admin | Explicitly move an idle vehicle in/out of service |
| `POST` | `/api/v1/ambulance-requests` | Patient/authorized operational staff | Idempotently record patient or staff transport request; never auto-dispatch |
| `GET` | `/api/v1/ambulance-requests/mine` | Patient | Own requests, coarse assigned-vehicle area, and audited timeline |
| `GET` | `/api/v1/ambulance-requests?hospitalId=...&status=...` | Ambulance dispatcher/admin | Hospital dispatch worklist |
| `POST` | `/api/v1/ambulance-requests/{requestId}/assign` | Ambulance dispatcher/admin | Lock request/vehicle and explicitly assign one available ambulance |
| `POST` | `/api/v1/ambulance-requests/{requestId}/acknowledge` | Ambulance dispatcher/admin | Record crew acknowledgement after assignment |
| `POST` | `/api/v1/ambulance-requests/{requestId}/status` | Ambulance dispatcher/admin | Apply the next allowed operational stage only |
| `POST` | `/api/v1/ambulance-requests/{requestId}/cancel` | Owner before dispatch or dispatcher/admin | Cancel safely and atomically release an assigned vehicle when allowed |
| `GET` | `/api/v1/operations/dashboard?hospitalId=...&date=...` | Doctor/reception/cashier/admin | Role-aware operational summary and queue; linked doctors see only their own work |
| `POST` | `/api/v1/operations/doctor-status` | Doctor/reception/admin | Report on-time, delay, interruption, unavailability, or cancellation and notify affected patients |
| `POST` | `/api/v1/operations/appointments/{appointmentId}/no-show` | Doctor/reception/admin | Mark a confirmed due appointment no-show, release capacity, and promote the waitlist |
| `GET` | `/api/v1/operations/recovery/mine` | Patient | Patient-owned doctor-cancellation recovery cases |
| `POST` | `/api/v1/operations/recovery/{caseId}/decision` | Patient owner | Explicitly approve rescheduling/eligible transfer/priority future queue, or request refund review |
| `POST` | `/api/v1/blood-group-analyses` | Patient | Submit a private decodable JPG/PNG slide image after explicit safety acknowledgement |
| `GET` | `/api/v1/blood-group-analyses/mine` | Patient | Own image-review submissions and verified status; never other patients' records |
| `GET` | `/api/v1/blood-group-analyses/worklist?hospitalId=...&status=...` | Lab/blood-bank/admin | Authorized hospital review worklist |
| `GET` | `/api/v1/blood-group-analyses/{analysisId}/image` | Owner or authorized reviewer | Private no-store image bytes after object authorization |
| `POST` | `/api/v1/blood-group-analyses/{analysisId}/observations` | Lab/blood-bank staff | Record Anti-A/Anti-B/Anti-D reactions once |
| `POST` | `/api/v1/blood-group-analyses/{analysisId}/verify` | Independent lab/blood-bank/admin | Verify the reaction-derived group; observer and verifier must differ |
| `POST` | `/api/v1/blood-group-analyses/{analysisId}/reject` | Lab/blood-bank/admin | Reject a non-final submission with a patient-visible reason |

Pagination responses use `{ content, page, size, totalElements, totalPages }` once a collection can grow beyond directory scale. Phase 1 doctor search already uses this shape.

## Planned module routes

```text
/api/v1/patients/{patientId}/...
/api/v1/admin/...
```

## Authorization rules

RBAC is the first gate; service-layer object authorization is the decisive gate. A `HOSPITAL_ADMIN` cannot administer another hospital. A doctor account must be linked to exactly one active doctor; patient-record access requires a real care relationship, diagnostic creation requires the assigned appointment, and emergency blood requests are restricted to the linked doctor's hospital. A patient sees only their own appointments, documents, diagnostic orders/results, blood requests, ambulance requests, assistant conversations, and messages. Ambulance assignment/acknowledgement/status/location require dispatcher/admin roles; patients never receive exact coordinates or crew contact. Blood inventory search excludes expired, quarantined, and verification-stale batches; donor contacts are visible only to blood-bank staff after explicit consent, staff verification, and an inventory shortfall. AI retrieval queries the database with the authenticated patient ID before ranking. Sensitive actions are audited without copying diagnosis, result, document, question, answer, donor-contact, pickup-address, or exact-location content into audit payloads.
