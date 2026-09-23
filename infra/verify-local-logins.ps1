param([string]$BaseUrl = 'http://localhost:5174')
$ErrorActionPreference = 'Stop'
$accounts = @(
    @('205', 'DemoDoctor@2026', 'DOCTOR'), @('206', 'DemoDoctor@2026', 'DOCTOR'),
    @('210', 'DemoDoctor@2026', 'DOCTOR'), @('211', 'DemoDoctor@2026', 'DOCTOR'),
    @('212', 'DemoDoctor@2026', 'DOCTOR'), @('213', 'DemoDoctor@2026', 'DOCTOR'),
    @('214', 'DemoDoctor@2026', 'DOCTOR'), @('215', 'DemoDoctor@2026', 'DOCTOR'),
    @('216', 'DemoDoctor@2026', 'DOCTOR'), @('217', 'DemoDoctor@2026', 'DOCTOR'),
    @('207', 'DemoOffice@2026', 'RECEPTIONIST,CASHIER'),
    @('208', 'DemoReception@2026', 'RECEPTIONIST'), @('209', 'DemoCashier@2026', 'CASHIER'),
    @('204', 'DemoAdmin@2026', 'HOSPITAL_ADMIN'), @('201', 'DemoDispatch@2026', 'AMBULANCE_DISPATCHER'),
    @('202', 'DemoLab@2026', 'LAB_TECHNICIAN'), @('203', 'DemoBlood@2026', 'BLOOD_BANK_STAFF')
)
$hospitals = Invoke-RestMethod "$BaseUrl/api/v1/hospitals"
$hospital = $hospitals | Where-Object { $_.name -eq 'SmartCare Demo Care Centre' } | Select-Object -First 1
if (!$hospital) { throw 'Demo hospital is missing. Enable SMARTCARE_DEMO_DATA and restart the backend.' }
$date = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId([DateTime]::UtcNow, 'India Standard Time').ToString('yyyy-MM-dd')
foreach ($account in $accounts) {
    $mobile = '+919999990' + $account[0]
    $body = @{credential=$mobile; password=$account[1]} | ConvertTo-Json -Compress
    $login = Invoke-RestMethod "$BaseUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $body
    $headers = @{Authorization='Bearer ' + $login.accessToken}
    foreach ($role in $account[2].Split(',')) {
        if ($login.user.roles -notcontains $role) { throw "Wrong role for $mobile" }
    }
    $null = Invoke-RestMethod "$BaseUrl/api/v1/auth/me" -Headers $headers
    $path = switch ($account[2]) {
        'DOCTOR' { '/api/v1/appointments/doctor/mine' }
        'LAB_TECHNICIAN' { "/api/v1/diagnostics/worklist?hospitalId=$($hospital.id)&date=$date" }
        'BLOOD_BANK_STAFF' { "/api/v1/blood-requests?hospitalId=$($hospital.id)" }
        'AMBULANCE_DISPATCHER' { "/api/v1/ambulances?hospitalId=$($hospital.id)" }
        default { "/api/v1/operations/dashboard?hospitalId=$($hospital.id)&date=$date" }
    }
    $null = Invoke-RestMethod "$BaseUrl$path" -Headers $headers
    [pscustomobject]@{ LoginId=$mobile; Name=$login.user.displayName; Roles=($login.user.roles -join ', '); Login='PASS'; Workspace='PASS' }
}
# Tokens are retained only in this process and are never written to output or disk.
