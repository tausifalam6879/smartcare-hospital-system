# Staff registration and login

Public patient registration creates only a patient account. Staff access requires independent administrator approval and a valid invitation; a client-supplied role cannot promote an account.

## Complete access workflow

1. A staff member opens **Request staff access** from Login or from staff registration (`/staff/request-access`). They choose the hospital and role, and enter their name, mobile, optional contact email and a short department/staff reference. No password, patient information or identity documents are requested here.
2. Submitting creates a **pending request**, not an account or permission. The response does not expose another person's request. Repeated submissions for the same pending hospital/mobile/role do not create duplicate entries. Request endpoints are rate limited.
3. The Super Admin opens **Staff access and invitations** (`/staff/access`), chooses the hospital, and reviews its request inbox. The administrator independently verifies identity and employment. They may reject the request or choose **Review for invitation**. Requested hospital, mobile and role must match the issued invitation on the server.
4. For a doctor, create/verify their hospital doctor profile first in Hospital setup. Only active profiles without a linked user appear in the invitation selector. Another active invitation for the same mobile or doctor in that hospital must be revoked before issuing a replacement.
5. After verification, issue the invitation. **Copy full invitation** includes hospital, role, exact mobile, full code, expiry and the activation URL. Share it privately with the verified person. There is no automatic SMS or email integration.
6. The staff member opens `/register`, chooses the invited role and enters the full code and exact invited mobile number. A new user supplies a name and creates a password. An existing user selects **I already have a SmartCare account** and confirms their current password. The existing account and patient records are preserved; knowing a phone number and code alone cannot take over an existing account.
7. Successful activation consumes the invitation, assigns the approved hospital/roles, links the doctor profile if applicable, and signs in with the new permissions. Later login requires the user's password; it does not require another invitation.

The administrator's paginated **Invitation history** remains available after refresh. It shows Active, Accepted, Revoked or Expired and permits revoking active invitations. Neither the original token nor its hash is exposed by the list endpoint. If a code is lost, revoke it and issue a replacement; codes cannot be recovered from history. A successful issuance must be explicitly dismissed before creating another code in the same page, so an accidental click cannot hide an unshared code.

Staff enrolment uses a cryptographically random, one-use invitation that expires after 24 hours. Only its SHA-256 hash is stored. The invitation binds the account's mobile number, staff role and intended hospital; doctor invitations also bind an existing active unlinked doctor profile. Invalid, revoked, expired, reused or mismatched invitations are rejected. Existing account activation additionally requires the current account password. New account registration does not silently overwrite an existing account.

## System administrator

Sign in with a securely provisioned `SUPER_ADMIN` account and open `/staff/access`. Use the request inbox or manually enter the independently verified staff member's details. Share the code privately, not in Git or a public chat. The code is shown only in the current page session. New and older active invitations can be revoked. API revocation is available at `DELETE /api/v1/auth/staff-invitations/{id}`.

Use the protected administrator bootstrap configuration for controlled provisioning. Issuing, listing and reviewing invitations/requests remain restricted to `SUPER_ADMIN`. Normal hospital administrators cannot issue them. `SUPER_ADMIN` cannot be granted by an invitation.

## Staff member

Open `/register`, select the invited role, enter the invitation code, the exact invited mobile number, name and a personal password. Then sign in at `/login` with that password. Selecting a workspace never grants permission: the server checks the account's stored roles. Office clerk means both receptionist and cashier authority.

The invitation is an enrolment credential, **not a login second factor or a WebAuthn passkey**. It is not requested on every login. Existing institution-provisioned/demo staff credentials continue to work; there is no shared master password. Patient registration stays open without an invitation.

GitHub Pages remains patient-demo-only and cannot enrol or authenticate real staff. Tests must use the backend.

## Remaining security boundaries

Hospital membership checks and integration tests exist across the clinical and staff workflows; those tests do not establish exhaustive production isolation. The request/invitation endpoints use server-side role checks and hospital bindings. The process-local request/login limiter still needs a shared ingress/distributed policy for multiple cloud instances. MFA/WebAuthn and session revocation remain separate hardening work. Do not treat the invitation as proof that every other hospital endpoint has been audited.

## Deployment compatibility

This flow adds Flyway migration `V23__staff_access_requests.sql` and new backend endpoints. Deploy the backend with the migration before publishing the matching frontend. Existing invitation tokens remain valid until used, revoked or expired. Do not reset the staging database or edit an already applied migration.
