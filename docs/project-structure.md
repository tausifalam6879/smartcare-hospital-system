# Project Directory Structure

```text
raahmediq-health/
├── backend/
│   ├── .mvn/wrapper/             # pinned, checksummed Maven wrapper
│   ├── src/main/java/com/raahmediq/
│   │   ├── auth/                 # accounts, roles, JWT, security
│   │   ├── patient/              # patient registration profile
│   │   ├── hospital/             # hospital and department directory
│   │   ├── doctor/               # doctor profiles and weekly schedules
│   │   ├── appointment/          # Phase 2 booking, capacity ledger, waitlist
│   │   ├── payment/              # Phase 3 intents, webhooks, receipts, refunds
│   │   ├── checkin/              # Phase 4 audited patient arrival channels
│   │   ├── queue/                # Phase 4 privacy-safe live queue and advancement
│   │   ├── notification/         # Phase 4 inbox and delivery adapters
│   │   ├── navigation/           # Phase 5 verified graph, QR checkpoints, routing
│   │   ├── medicalrecord/          # Phase 6 visits, prescriptions, allergies, private files
│   │   ├── ai/                   # Phase 7 patient-isolated chunks, retrieval, conversations
│   │   ├── diagnostic/           # Phase 8 orders, capacity, worklist, verified results
│   │   ├── bloodbank/            # Phase 9 verified inventory, requests, allocations, donor consent
│   │   ├── bloodgroupai/          # Phase 12 private slide review, analyzer port, maker-checker verification
│   │   ├── ambulance/            # Phase 10 fleet, locked dispatch, timeline, patient status
│   │   ├── operations/           # Phase 11 doctor-day state, recovery, role-aware workboard
│   │   ├── audit/                # append-only audit events
│   │   └── common/               # shared errors, IDs, paging, correlation
│   ├── src/main/resources/
│   │   └── db/migration/         # Flyway schema history
│   ├── src/test/                 # authentication, directory, and queue tests
│   ├── Dockerfile
│   └── pom.xml                   # Java release 17
├── frontend/
│   ├── src/
│   │   ├── components/           # accessible shared UI
│   │   ├── context/              # session/auth state
│   │   ├── pages/                # home, directory, auth, care dashboard
│   │   └── services/             # typed API client
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   └── vite.config.ts            # React + Tailwind Vite plugin
├── docs/                         # architecture and workflow decisions
├── .github/workflows/ci.yml
├── docker-compose.yml
├── .env.example
└── README.md
```

Future domain packages are added inside the same modular monolith (`admin` and later integrations). The delivered `diagnostic`, `bloodbank`, `bloodgroupai`, `ambulance`, and `operations` packages and all future packages own their domain model and persistence adapters and expose application interfaces/events rather than sharing repositories.
