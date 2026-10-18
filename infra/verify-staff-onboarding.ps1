param(
    [string]$BaseUrl = 'http://127.0.0.1:8081',
    [Parameter(Mandatory)][string]$AdminMobile,
    [Parameter(Mandatory)][securestring]$AdminPassword
)
$ErrorActionPreference = 'Stop'
# This writes synthetic QA records. It deliberately refuses cloud/production hosts.
$uri = [uri]$BaseUrl
if (-not $uri.IsLoopback -or $uri.Scheme -ne 'http') { throw 'Use an isolated local test backend on a loopback HTTP address.' }
$BaseUrl = $BaseUrl.TrimEnd('/')
function Call-Api([string]$Method, [string]$Path, $Body = $null, [string]$Token = '', [hashtable]$ExtraHeaders = @{}) {
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; ContentType = 'application/json'; TimeoutSec = 30; Headers = @{} }
    foreach ($key in $ExtraHeaders.Keys) { $params.Headers[$key] = $ExtraHeaders[$key] }
    if ($Token) { $params.Headers.Authorization = "Bearer $Token" }
    if ($null -ne $Body) { $params.Body = ConvertTo-Json -InputObject $Body -Depth 10 -Compress }
    $apiResponse = Invoke-RestMethod @params
    foreach ($apiItem in $apiResponse) { Write-Output $apiItem }
}
function Assert-That([bool]$Condition, [string]$Description) { if (-not $Condition) { throw "Verification failed: $Description" } }
function Assert-Status([int]$Status, [scriptblock]$Action) {
    try { $null = & $Action } catch {
        if ([int]$_.Exception.Response.StatusCode -eq $Status) { return }
        throw
    }
    throw "Expected HTTP $Status."
}
$credential = [pscredential]::new('qa', $AdminPassword)
$admin = Call-Api POST '/api/v1/auth/login' @{ credential = $AdminMobile; password = $credential.GetNetworkCredential().Password; accountType = 'SUPER_ADMIN' }
$adminToken = $admin.accessToken
$tag = [guid]::NewGuid().ToString('N').Substring(0, 10)
$testPassword = 'QA-' + [guid]::NewGuid().ToString('N')
$hospital = Call-Api POST '/api/v1/hospitals' @{ code = "QA-$tag"; name = "QA Onboarding $tag"; addressLine = 'Synthetic QA only'; city = 'Delhi'; state = 'Delhi'; postalCode = '110001'; contactNumber = '+919000008000'; timeZone = 'Asia/Kolkata'; active = $true } $adminToken
$other = Call-Api POST '/api/v1/hospitals' @{ code = "QA-OTHER-$tag"; name = "QA Other $tag"; addressLine = 'Synthetic QA only'; city = 'Delhi'; state = 'Delhi'; postalCode = '110001'; contactNumber = '+919000008000'; timeZone = 'Asia/Kolkata'; active = $true } $adminToken
$department = Call-Api POST "/api/v1/hospitals/$($hospital.id)/departments" @{ code = 'QA'; name = 'QA Medicine'; description = 'Synthetic QA only' } $adminToken
$doctor = Call-Api POST '/api/v1/doctors' @{ hospitalId = $hospital.id; departmentId = $department.id; name = 'QA Doctor'; specialization = 'QA Medicine'; registrationNumber = "QA-$tag"; consultationFee = 0; expectedConsultationMinutes = 15; dailyMaxCapacity = 20; building = 'QA'; floorLabel = '1'; roomNumber = '101'; active = $true } $adminToken
$null = Call-Api POST "/api/v1/doctors/$($doctor.id)/schedules" @{ dayOfWeek = 'MONDAY'; startTime = '09:00'; endTime = '13:00'; slotDurationMinutes = 15 } $adminToken
$detail = Call-Api GET "/api/v1/doctors/$($doctor.id)"
Assert-That ($detail.schedules.Count -eq 1) 'OPD schedule persisted'
$roles = @('DOCTOR', 'HOSPITAL_ADMIN', 'OFFICE_CLERK', 'RECEPTIONIST', 'CASHIER', 'LAB_TECHNICIAN', 'BLOOD_BANK_STAFF', 'AMBULANCE_DISPATCHER')
$worklists = @{ HOSPITAL_ADMIN='/api/v1/operations/dashboard'; OFFICE_CLERK='/api/v1/operations/dashboard'; RECEPTIONIST='/api/v1/operations/dashboard'; CASHIER='/api/v1/operations/dashboard'; LAB_TECHNICIAN='/api/v1/diagnostics/worklist'; BLOOD_BANK_STAFF='/api/v1/blood-requests'; AMBULANCE_DISPATCHER='/api/v1/ambulance-requests' }
$roleSessions = @{}
foreach ($role in $roles) {
    $mobile = '+9199' + (Get-Random -Minimum 10000000 -Maximum 99999999)
    $null = Call-Api POST '/api/v1/auth/staff-access-requests' @{ name = "QA $role"; mobileNumber = $mobile; accountType = $role; hospitalId = $hospital.id; message = "Synthetic QA staff request $tag" }
    $pending = Call-Api GET "/api/v1/auth/staff-access-requests?hospitalId=$($hospital.id)" $null $adminToken
    $request = @($pending.content | Where-Object mobileNumber -eq $mobile)[0]
    Assert-That ($null -ne $request) 'request is visible to administrator'
    $invitePayload = @{ mobileNumber = $mobile; accountType = $role; hospitalId = $hospital.id; requestId = $request.id }
    if ($role -eq 'DOCTOR') { $invitePayload.doctorId = $doctor.id }
    $invite = Call-Api POST '/api/v1/auth/staff-invitations' $invitePayload $adminToken
    Assert-Status 409 { Call-Api POST '/api/v1/auth/staff-invitations' $invitePayload $adminToken }
    $session = Call-Api POST '/api/v1/auth/register-staff' @{ invitationCode = $invite.invitationCode; accountType = $role; patient = @{ name = "QA $role"; mobileNumber = $mobile; password = $testPassword; preferredLanguage = 'en' } }
    $signedIn = Call-Api POST '/api/v1/auth/login' @{ credential = $mobile; password = $testPassword; accountType = $role }
    $roleSessions[$role] = $signedIn
    Assert-That ($signedIn.user.id -eq $session.user.id) 'password login uses activated account'
    Assert-Status 403 { Call-Api GET "/api/v1/auth/staff-access-requests?hospitalId=$($hospital.id)" $null $session.accessToken }
    if ($role -eq 'DOCTOR') {
        Assert-Status 403 { Call-Api POST '/api/v1/auth/register-staff' @{ invitationCode = $invite.invitationCode; accountType = $role; patient = @{ name = 'QA replay'; mobileNumber = $mobile; password = $testPassword; preferredLanguage = 'en' } } }
    }
    if ($role -eq 'DOCTOR') { $null = Call-Api GET '/api/v1/appointments/doctor/mine' $null $signedIn.accessToken }
    else {
        $path = $worklists[$role]
        $date = Get-Date -Format 'yyyy-MM-dd'
        $null = Call-Api GET "${path}?hospitalId=$($hospital.id)&date=$date" $null $signedIn.accessToken
        Assert-Status 403 { Call-Api GET "${path}?hospitalId=$($other.id)&date=$date" $null $signedIn.accessToken }
    }
    Write-Output "PASS $role request -> approval -> activation -> password login -> authorized worklist; administrator-only inbox denied."
}
$mobile = '+9199' + (Get-Random -Minimum 10000000 -Maximum 99999999)
$patient = Call-Api POST '/api/v1/auth/register' @{ name = 'QA Existing Patient'; mobileNumber = $mobile; password = $testPassword; preferredLanguage = 'en' }
$hospitalNow = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([datetime]::UtcNow, 'India Standard Time')
$serviceDate = $hospitalNow.ToString('yyyy-MM-dd')
$startTime = if ($hospitalNow.DayOfWeek -eq [DayOfWeek]::Monday) { '14:00' } else { '00:00' }
$null = Call-Api POST "/api/v1/doctors/$($doctor.id)/schedules" @{ dayOfWeek = $hospitalNow.DayOfWeek.ToString().ToUpperInvariant(); startTime = $startTime; endTime = '23:59'; slotDurationMinutes = 15 } $adminToken
$bookingBody = @{ doctorId = $doctor.id; serviceDate = $serviceDate; paymentMethod = 'CASH' }
$booking = Call-Api POST '/api/v1/appointments' $bookingBody $patient.accessToken @{ 'Idempotency-Key' = "qa-$tag" }
$retryBooking = Call-Api POST '/api/v1/appointments' $bookingBody $patient.accessToken @{ 'Idempotency-Key' = "qa-$tag" }
Assert-That ($retryBooking.id -eq $booking.id) 'booking retry is idempotent'
$null = Call-Api POST "/api/v1/appointments/$($booking.id)/cash-confirmation" $null $roleSessions['OFFICE_CLERK'].accessToken
$null = Call-Api POST "/api/v1/check-in/appointments/$($booking.id)" @{ channel = 'RECEPTION_DESK' } $roleSessions['OFFICE_CLERK'].accessToken
$null = Call-Api POST "/api/v1/queues/$($doctor.id)/serve-next?date=$serviceDate" $null $roleSessions['DOCTOR'].accessToken
$visit = Call-Api POST '/api/v1/medical-records/visits' @{
    appointmentId = $booking.id; diagnosis = 'Synthetic QA only - no diagnosis'; doctorNotes = 'No real patient or clinical care';
    followUpRecommendation = 'Synthetic QA only'; followUpDate = $hospitalNow.AddDays(1).ToString('yyyy-MM-dd');
    medicines = @(@{ medicineName = 'QA placeholder - not a medication'; dosage = 'Not applicable'; frequency = 'Not applicable'; duration = 'Not applicable' });
    allergies = @(); medicationReminderEnabled = $false
} $roleSessions['DOCTOR'].accessToken
$records = Call-Api GET '/api/v1/medical-records/mine' $null $patient.accessToken
Assert-That ([bool]($records.visits | Where-Object id -eq $visit.id)) 'patient receives finalized visit'
Assert-That ($records.visits[0].prescription.medicines[0].medicineName -eq 'QA placeholder - not a medication') 'prescription persists'
$followUps = @(Call-Api GET '/api/v1/follow-ups/mine' $null $patient.accessToken)
Assert-That ($followUps.Count -gt 0) 'follow-up created'
$mine = @(Call-Api GET '/api/v1/appointments/mine' $null $patient.accessToken)
Assert-That (($mine | Where-Object id -eq $booking.id).status -eq 'COMPLETED') 'appointment reaches completed status'
Write-Output 'PASS patient registration -> idempotent booking -> authorized cash record -> check-in -> assigned doctor consultation -> synthetic prescription -> patient record -> follow-up.'
$invite = Call-Api POST '/api/v1/auth/staff-invitations' @{ mobileNumber = $mobile; accountType = 'CASHIER'; hospitalId = $hospital.id } $adminToken
Assert-Status 401 { Call-Api POST '/api/v1/auth/activate-staff' @{ invitationCode = $invite.invitationCode; accountType = 'CASHIER'; mobileNumber = $mobile; password = 'incorrect-qa-password' } }
$activated = Call-Api POST '/api/v1/auth/activate-staff' @{ invitationCode = $invite.invitationCode; accountType = 'CASHIER'; mobileNumber = $mobile; password = $testPassword }
Assert-That ($activated.user.id -eq $patient.user.id -and $activated.user.roles -contains 'CASHIER') 'existing patient activation preserves account'
$eligible = @(Call-Api GET "/api/v1/auth/staff-invitations/doctors?hospitalId=$($hospital.id)" $null $adminToken)
Assert-That (-not ($eligible | Where-Object id -eq $doctor.id)) 'linked doctor excluded from eligible profiles'
Write-Output 'PASS existing patient activation requires the current password and preserves identity; linked doctor excluded.'
Write-Output 'PASS reused invitation denied; duplicate active issuance denied; cross-hospital worklists denied for all seven non-doctor staff roles.'
Write-Output "Synthetic data created in local QA hospital $($hospital.id). No production or cloud endpoint was contacted."
$admin = $null; $adminToken = $null; $credential = $null; $testPassword = $null; $session = $null; $signedIn = $null; $activated = $null; $roleSessions = $null; $patient = $null
