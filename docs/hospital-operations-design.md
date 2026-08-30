# Phase 11 Hospital Operations

## Purpose

Phase 11 connects daily hospital disruptions to the existing capacity, queue, notification, payment, diagnostic, blood, and ambulance modules. It is an operational coordination layer, not a clinical-triage or autonomous scheduling system.

## Doctor-day states

Each doctor/date has at most one operational record: `ON_TIME`, `DELAYED_30`, `DELAYED_60`, `EMERGENCY_INTERRUPTION`, `TEMPORARILY_UNAVAILABLE`, or `CANCELLED_FOR_DAY`. Non-on-time updates require a short patient-safe reason. A reported 30/60-minute delay is added to the live patient estimate, which remains labelled approximate because emergencies and clinical priorities can change the order.

## Cancellation recovery

Cancelling a doctor's day creates one recovery case for each confirmed or checked-in appointment and sends a durable notification. The appointment is intentionally unchanged at this point. The patient must choose:

- the same doctor on another available date;
- an active eligible doctor in the same hospital and department;
- a priority future queue date; or
- cancellation with refund review.

For a reschedule, RaahMediQ Health locks the target doctor/day ledger, verifies guaranteed capacity, releases the original position with normal FIFO promotion, and confirms the new position in one transaction. `REFUND_REVIEW_REQUIRED` means staff/payment workflow must still determine eligibility and gateway outcome; it is not a successful refund.

## Operational dashboard

Doctor, reception, cashier, hospital-admin, and super-admin roles receive a shared shell with role-specific controls. The workboard shows appointment status counts, active and delayed doctors, pending payments, diagnostic load, verified-blood alerts, ambulance readiness, recorded waits, and privacy-safe queue rows. Only super admins receive global notification-failure totals. Doctors are object-scoped to their linked doctor. Cashiers can view but cannot change doctor state or no-show status.

No-show marking is explicit, audited, limited to confirmed non-future appointments, and reuses the locked capacity release/FIFO promotion path.

## Production boundary

Before production, add hospital-scoped staff assignment for every reception/cashier/admin identity, MFA, maker-checker approval for cancellation/refund operations, formal reason taxonomy, escalation SLAs, union/clinical governance, signed external notification adapters, real gateway refund reconciliation, shift handover, immutable operational exports, monitoring, and disaster-recovery exercises. Automated rollover may only be introduced with a documented patient-consent and capacity policy.
