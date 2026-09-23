# SmartCare local demo logins

Open http://localhost:5174/login. Enter the complete mobile number (including `+91`) in **Mobile number or email**. Each number is a separate user ID. The server selects the workspace from the account's roles.

These are development/demo credentials, enabled only with `SMARTCARE_DEMO_DATA=true`. Do not use these shared passwords for a deployed hospital. Existing account passwords and existing doctor links are preserved on restart.

Verified on 24 September 2026: all 17 IDs below successfully logged in and accessed their authorized workspace on the running local deployment.

## Doctors

Password for each doctor below: `DemoDoctor@2026`

| Doctor | Specialty | Login ID |
| --- | --- | --- |
| Dr. Ananya Mehta | Internal Medicine | +919999990205 |
| Dr. Isha Kapoor | Dermatology | +919999990206 |
| Dr. Arjun Rao | Cardiology | +919999990210 |
| Dr. Kavya Nair | Neurology | +919999990211 |
| Dr. Rohan Singh | Orthopaedic Surgery | +919999990212 |
| Dr. Meera Iyer | Paediatrics | +919999990213 |
| Dr. Sana Ahmed | Obstetrics & Gynaecology | +919999990214 |
| Dr. Vivek Das | ENT | +919999990215 |
| Dr. Aditya Sen | Medical Oncology | +919999990216 |
| Dr. Nidhi Verma | Ophthalmology | +919999990217 |

Each login is linked to its own doctor record in SmartCare Demo Care Centre. Consultations show that doctor's appointments. These accounts do not represent doctors listed in simulated external hospital directory previews.

## Office and hospital team

| Account | Login ID | Password | Work |
| --- | --- | --- | --- |
| Office clerk | +919999990207 | DemoOffice@2026 | Reception + cashier: cash confirmation, check-in, queue and doctor day status |
| Receptionist | +919999990208 | DemoReception@2026 | Check-in and queue/day operations; cannot confirm cash |
| Cashier | +919999990209 | DemoCashier@2026 | View cash/online booking states and confirm received cash; cannot check in or edit doctor status |
| Hospital administrator | +919999990204 | DemoAdmin@2026 | Operations, staff tasks and ambulance desk |
| Lab technician | +919999990202 | DemoLab@2026 | Diagnostic worklist and lab verification |
| Blood bank staff | +919999990203 | DemoBlood@2026 | Blood requests/inventory and slide review |
| Ambulance dispatcher | +919999990201 | DemoDispatch@2026 | Transport requests and dispatch |

Patient: use your existing registered account, or create a patient account at http://localhost:5174/register. Public registration creates patient accounts only.

## Complete OPD handoff

1. Patient: select **SmartCare Demo Care Centre**, doctor and visit date, then book OPD.
2. Cash booking: clerk/cashier confirms cash only after receipt. Online booking: provider confirmation updates payment; a pending online intent is not a paid appointment.

   Current online provider is DEVELOPMENT: no real UPI/card checkout or money collection is connected. A signed test webhook is needed to confirm an online test payment. For the normal manual demonstration, choose Cash.
3. Clerk: open **Operations**, choose the correct hospital and visit date. The patient must be confirmed before check-in, and within the hospital check-in window.
4. Doctor: sign in with the ID belonging to the booked doctor. On the visit day, the checked-in patient appears in **Consultations**. Click **Call next patient**.
5. Doctor: finalize the visit. Clinical notes, prescriptions and follow-ups appear in the patient's records. Diagnostic orders and blood requests are handled by the respective staff roles.

Operations and doctor queues refresh every 15 seconds while the page is visible and when the window receives focus. Separate browsers/private windows are useful for simultaneous role testing: normal tabs share one login session.

Future appointments are visible for their selected date in Operations, but cannot appear in today's doctor queue or be checked in early. Patient queue numbers are scoped to doctor and date, not the whole hospital.
