param([switch]$AllowBriefDowntime, [switch]$RetainEncryptedBackup)
$ErrorActionPreference = 'Stop'
if (!$AllowBriefDowntime) { throw 'Run with -AllowBriefDowntime to pause backend writes during the consistent snapshot.' }
Set-Location (Split-Path $PSScriptRoot -Parent)
function DockerChecked {
    $Arguments = $args
    $result = & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker command failed: $($Arguments[0])" }
    return $result
}
$databaseId = (DockerChecked compose ps -q postgres).Trim()
$backendId = (DockerChecked compose ps -q backend).Trim()
if (!$databaseId -or !$backendId) { throw 'Start the local postgres and backend services first.' }
$database = (DockerChecked inspect $databaseId | ConvertFrom-Json)[0]
$backend = (DockerChecked inspect $backendId | ConvertFrom-Json)[0]
$network = $database.NetworkSettings.Networks.PSObject.Properties.Name | Select-Object -First 1
$documents = $backend.Mounts | Where-Object { $_.Destination -eq '/var/lib/smartcare/private-documents' }
if (!$documents -or $documents.Type -ne 'volume') { throw 'Expected the private documents named volume.' }
$settings = @{}
foreach ($entry in $database.Config.Env) {
    $parts = $entry.Split('=', 2)
    $settings[$parts[0]] = $parts[1]
}
$drillName = 'smartcare-restore-drill-' + [Guid]::NewGuid().ToString('N')
$label = 'smartcare.restore-drill=' + $drillName
$created = $false
$paused = $false
$previousPassword = $env:POSTGRES_PASSWORD
$previousSourcePassword = $env:PGPASSWORD
try {
    # Secrets are inherited by Docker from the process environment, never printed or put in command arguments.
    $env:POSTGRES_PASSWORD = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
    $env:PGPASSWORD = $settings['POSTGRES_PASSWORD']
    DockerChecked run -d --name $drillName --label $label --network $network `
        -e POSTGRES_PASSWORD -e PGPASSWORD -e POSTGRES_DB=restore_verify `
        --mount "type=volume,source=$($documents.Name),target=/source-documents,readonly" `
        $database.Config.Image | Out-Null
    $created = $true
    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        & docker exec $drillName pg_isready -U postgres -d restore_verify *> $null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw 'Isolated PostgreSQL did not become ready.' }
    Write-Output 'Pausing backend briefly for a database + documents snapshot.'
    DockerChecked compose stop backend | Out-Null
    $paused = $true
    DockerChecked exec $drillName pg_dump -h $database.NetworkSettings.Networks.$network.IPAddress `
        -U $settings['POSTGRES_USER'] -d $settings['POSTGRES_DB'] --format=custom --file=/tmp/source.dump
    DockerChecked exec $drillName tar -cf /tmp/documents.tar -C /source-documents .
    DockerChecked exec $drillName sh -c 'cd /source-documents && find . -type f -exec sha256sum {} \; > /tmp/documents.sha256'
    # Row totals are captured while writes are paused; no patient field values are emitted.
    $countSql = @'
SELECT format('SELECT %L || ''|'' || count(*) FROM %I.%I;', tablename, schemaname, tablename)
FROM pg_tables WHERE schemaname='public' ORDER BY tablename
\gexec
'@
    $sourceCounts = $countSql | & docker exec -i $drillName psql -X -qAt -v ON_ERROR_STOP=1 `
        -h $database.NetworkSettings.Networks.$network.IPAddress -U $settings['POSTGRES_USER'] -d $settings['POSTGRES_DB']
    if ($LASTEXITCODE -ne 0) { throw 'Source row-count check failed.' }
    DockerChecked compose start backend | Out-Null
    $paused = $false
    DockerChecked exec $drillName pg_restore -U postgres -d restore_verify --no-owner --no-acl --exit-on-error /tmp/source.dump
    $restoredCounts = $countSql | & docker exec -i $drillName psql -X -qAt -v ON_ERROR_STOP=1 -U postgres -d restore_verify
    if ($LASTEXITCODE -ne 0) { throw 'Restored row-count check failed.' }
    if (Compare-Object $sourceCounts $restoredCounts) { throw 'Restored database row counts differ.' }
    DockerChecked exec $drillName sh -c 'mkdir /restored-documents && tar -xf /tmp/documents.tar -C /restored-documents'
    # Compare the restored document archive against its snapshot, not against the now-live source volume.
    DockerChecked exec $drillName sh -c 'cd /restored-documents && if test -s /tmp/documents.sha256; then sha256sum -c /tmp/documents.sha256 >/dev/null; fi'
    Write-Output "PASS: restored $($sourceCounts.Count) database table counts and verified the private document archive."
    if ($RetainEncryptedBackup) {
        # No plaintext medical archive is copied onto the host. DPAPI binds this local copy to
        # the current Windows user/machine; it is NOT a portable/offsite disaster-recovery copy.
        Add-Type -AssemblyName System.Security
        DockerChecked exec $drillName tar -cf /tmp/smartcare-backup.tar -C /tmp source.dump documents.tar documents.sha256
        $encodedArchive = (DockerChecked exec $drillName base64 -w 0 /tmp/smartcare-backup.tar) -join ''
        $archiveBytes = [Convert]::FromBase64String($encodedArchive)
        $protectedBytes = [System.Security.Cryptography.ProtectedData]::Protect($archiveBytes, $null,
            [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        # Immediately round-trip the encryption, in addition to the isolated database restore above.
        $decrypted = [System.Security.Cryptography.ProtectedData]::Unprotect($protectedBytes, $null,
            [System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        $hash = [System.Security.Cryptography.SHA256]::Create()
        try {
            if ([Convert]::ToBase64String($hash.ComputeHash($archiveBytes)) -ne [Convert]::ToBase64String($hash.ComputeHash($decrypted))) {
                throw 'Encrypted backup round-trip verification failed.'
            }
        } finally { $hash.Dispose() }
        $backupDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) 'backups'
        New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
        $backupFile = Join-Path $backupDirectory ('smartcare-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N') + '.dpapi')
        [System.IO.File]::WriteAllBytes($backupFile, $protectedBytes)
        [Array]::Clear($archiveBytes, 0, $archiveBytes.Length)
        [Array]::Clear($decrypted, 0, $decrypted.Length)
        $encodedArchive = $null
        Write-Output "Encrypted local backup saved: $backupFile"
        Write-Output 'Requires this Windows account/machine to decrypt. Keep offsite backup as a separate release gate.'
    }
    Write-Output 'Database/documents recovery drill passed; offsite disaster recovery and full application recovery remain separate checks.'
} finally {
    if ($paused) { & docker compose start backend | Out-Null }
    $env:POSTGRES_PASSWORD = $previousPassword
    $env:PGPASSWORD = $previousSourcePassword
    if ($created) {
        $owned = (& docker inspect --format '{{ index .Config.Labels "smartcare.restore-drill" }}' $drillName)
        if ($LASTEXITCODE -eq 0 -and $owned -eq $drillName) {
            & docker rm -f -v $drillName | Out-Null
            if ($LASTEXITCODE -ne 0) { Write-Warning "Remove the temporary drill container manually: $drillName" }
        }
    }
}
