# SmartCare implementation roadmap

This roadmap separates the current portfolio prototype from the work required before any real hospital deployment. It is a planning document; no live hospital, payment, patient, or clinical data is connected by the project today.

## Current release: portfolio prototype

The repository already demonstrates the end-to-end product flows for OPD booking and queueing, indoor navigation, payments, check-in, clinical records, diagnostics, blood support, ambulance coordination, operations, and a local-only indicative wait-time service. The GitHub Pages version deliberately uses browser-only sample data.

## Next implementation sequence

### 1. Demo hardening

- Run a complete patient walkthrough on mobile and desktop: hospital selection, doctor selection, booking, payment simulation, queue check-in, and room navigation.
- Fix only reproducible UI, copy, route, accessibility, or responsive-layout issues found during those walkthroughs.
- Keep every public demo label explicit: synthetic, prototype, and not for emergency or clinical use.

### 2. Production platform readiness

- Replace sample-directory records with hospital-approved source data and establish a review/update owner.
- Configure managed PostgreSQL, private object storage, backups, monitoring, rate limits, secret management, and deployment environments.
- Add a real payment-gateway adapter only after merchant credentials, webhook handling, refund operations, and security review are approved.
- Add a hospital-approved map-authoring/review workflow and QR lifecycle before publishing real indoor routes.
- Integrate approved notification, dispatch, diagnostic/LIMS, blood-bank, and identity providers behind auditable adapters.

### 3. Governance and validation

- Perform role-based access, audit-log, privacy, retention, backup/restore, and incident-response reviews.
- Validate operational workflows with hospital staff: no-show handling, doctor delays, waitlist promotion, recovery choices, blood requests, diagnostics, and ambulance dispatch.
- Complete clinical, legal, privacy, accessibility, and security approvals before handling real patient data or representing the service as clinical software.

### 4. Data and ML work (local research only)

- Keep downloaded datasets, notebooks, experiments, and trained artifacts out of GitHub unless explicitly selected and privacy-reviewed.
- Choose a queue dataset only when it includes credible operational predictors such as patients ahead, staffing/doctor availability, triage, department, timestamp, and actual wait time.
- Establish a leakage review, train/test protocol, baseline comparison, error analysis, fairness checks, and model monitoring plan before considering integration.
- Integrate a model only after validation shows meaningful operational value; otherwise retain transparent rule-based estimates and prototype labels.

## Definition of a production-ready release

SmartCare is not production-ready until real integrations, operational ownership, privacy/security controls, and hospital validation are complete. A deployed static frontend is a demonstrable portfolio release, not a hospital service.
