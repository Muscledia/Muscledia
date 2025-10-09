# MongoDB Keyfile Setup Script for Windows
Write-Host "Creating MongoDB keyfile..." -ForegroundColor Green

try {
    # Create directory
    Write-Host "Creating config directory..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Force -Path .\config\mongodb | Out-Null

    # Generate secure random keyfile
    Write-Host "Generating secure 756-byte keyfile..." -ForegroundColor Yellow
    $bytes = New-Object byte[] 756
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $base64 = [Convert]::ToBase64String($bytes)
    
    # Write keyfile
    $keyfilePath = ".\config\mongodb\mongo-keyfile"
    $base64 | Out-File -FilePath $keyfilePath -Encoding ASCII -NoNewline
    $rng.Dispose()
    
    # Set strict permissions
    Write-Host "Setting restrictive file permissions..." -ForegroundColor Yellow
    $resolvedPath = Resolve-Path $keyfilePath
    $acl = Get-Acl $resolvedPath
    $acl.SetAccessRuleProtection($true, $false)
    $acl.Access | ForEach-Object { $acl.RemoveAccessRule($_) | Out-Null }

    $currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $permission = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $currentUser,
        "Read,Write",
        "Allow"
    )
    $acl.AddAccessRule($permission)
    Set-Acl -Path $resolvedPath -AclObject $acl

    Write-Host "`nKeyfile created successfully!" -ForegroundColor Green
    Write-Host "Location: $resolvedPath" -ForegroundColor Cyan
    
} catch {
    Write-Host "`nError: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
