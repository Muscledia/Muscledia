# MongoDB Keyfile Setup Script for Windows
# Generates a secure keyfile for MongoDB replica set authentication

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
    $rng.Dispose()  # Clean up RNG

    # Verify file was created
    if (-not (Test-Path $keyfilePath)) {
        throw "Failed to create keyfile"
    }

    # Set strict permissions (equivalent to chmod 600)
    Write-Host "Setting restrictive file permissions..." -ForegroundColor Yellow
    $resolvedPath = Resolve-Path $keyfilePath
    $acl = Get-Acl $resolvedPath

    # Disable inheritance and remove existing permissions
    $acl.SetAccessRuleProtection($true, $false)
    $acl.Access | ForEach-Object { $acl.RemoveAccessRule($_) | Out-Null }

    # Add read/write for current user only
    $currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $permission = New-Object System.Security.AccessControl.FileSystemAccessRule(
        $currentUser,
        "Read,Write",
        "Allow"
    )
    $acl.AddAccessRule($permission)
    Set-Acl -Path $resolvedPath -AclObject $acl

    # Verify keyfile size and content
    $fileInfo = Get-Item $resolvedPath
    $content = Get-Content $resolvedPath -Raw

    Write-Host "`nKeyfile created successfully!" -ForegroundColor Green
    Write-Host "Location: $resolvedPath" -ForegroundColor Cyan
    Write-Host "Size: $($fileInfo.Length) characters" -ForegroundColor Cyan
    Write-Host "Preview (first 50 chars): $($content.Substring(0, [Math]::Min(50, $content.Length)))..." -ForegroundColor Cyan

    # Verify permissions
    Write-Host "`nFile permissions:" -ForegroundColor Cyan
    $acl = Get-Acl $resolvedPath
    foreach ($access in $acl.Access) {
        Write-Host "  $($access.IdentityReference): $($access.FileSystemRights)" -ForegroundColor Gray
    }

    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "1. Update docker-compose.yml to mount this keyfile" -ForegroundColor White
    Write-Host "2. Add --keyFile parameter to MongoDB command" -ForegroundColor White
    Write-Host "3. Initialize replica set with mongodb-setup service" -ForegroundColor White

} catch {
    Write-Host "`nError creating keyfile: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Make sure you have write permissions to the current directory" -ForegroundColor Yellow
    exit 1
}