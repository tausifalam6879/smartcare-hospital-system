# Staff registration and login

Public registration always creates only a patient account. A client-supplied role cannot promote it.

Staff enrolment uses a cryptographically random, one-use invitation that expires after 24 hours. Only its SHA-256 hash is stored. The invitation binds the new account's mobile number, staff role and intended hospital; doctor invitations also bind an existing active unlinked doctor profile. Invalid, revoked, expired, reused or mismatched invitations are rejected. Existing accounts cannot be upgraded through this public flow.

## System administrator

Sign in with a securely provisioned `SUPER_ADMIN` account and open `/staff/access`. Verify the staff member independently, choose their role/hospital/mobile and issue an invitation. Share the code privately, not in Git or a public chat. It is shown only in the current page session. The page can revoke the newly issued invitation. API revocation by invitation ID is also available at `DELETE /api/v1/auth/staff-invitations/{id}`.

Do not give a demo account the system administrator role merely to test this feature. Use the existing protected administrator bootstrap configuration for controlled provisioning. Normal hospital administrators cannot issue invitations yet, because hospital membership enforcement across staff services is still a release gate. `SUPER_ADMIN` cannot be granted by an invitation.

## Staff member

Open `/register`, select the invited role, enter the invitation code, the exact invited mobile number, name and a personal password. Then sign in at `/login` with that password. Selecting a workspace never grants permission: the server checks the account's stored roles. Office clerk means both receptionist and cashier authority.

The invitation is an enrolment credential, **not a login second factor or a WebAuthn passkey**. It is not requested on every login. Existing institution-provisioned/demo staff credentials continue to work; there is no shared master password. Patient registration stays open without an invitation.

GitHub Pages remains patient-demo-only and cannot enrol or authenticate real staff. Tests must use the backend.

## Remaining security boundaries

The invitation records an intended hospital; that alone does not enforce hospital scope throughout existing operations, lab, blood-bank or dispatch services. Do not deploy this as a fully isolated multi-hospital system until that authorization work is complete. Production login rate limiting, recovery, MFA/WebAuthn and session revocation also need a separate hardening pass.
