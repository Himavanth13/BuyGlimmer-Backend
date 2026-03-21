param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipSmoke
)

$ErrorActionPreference = "Stop"

function New-WrappedBody {
    param(
        [string]$Token,
        [hashtable]$Data
    )

    return (@{
        token = $Token
        requestId = "req-" + [guid]::NewGuid().ToString()
        data = $Data
    } | ConvertTo-Json -Depth 12)
}

function Invoke-WrappedApi {
    param(
        [string]$Name,
        [string]$Path,
        [string]$Token,
        [hashtable]$Data
    )

    try {
        $response = Invoke-RestMethod -Method Post -Uri ($BaseUrl + $Path) -ContentType "application/json" -Body (New-WrappedBody -Token $Token -Data $Data)
        return [pscustomobject]@{
            Api = $Name
            Ok = $true
            Status = $response.status
            Message = $response.message
            Data = $response.data
        }
    }
    catch {
        $body = ""
        if ($_.Exception.Response) {
            $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
            $body = $reader.ReadToEnd()
        }

        return [pscustomobject]@{
            Api = $Name
            Ok = $false
            Status = "FAILED"
            Message = ($_.Exception.Message + " " + $body).Trim()
            Data = $null
        }
    }
}

function Assert-ServerReachable {
    try {
        $null = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl + "/swagger-ui.html") -TimeoutSec 8
    }
    catch {
        throw "Backend is not reachable at $BaseUrl. Start the app first (for example: mvn spring-boot:run)."
    }
}

Write-Host "[1/4] Checking server availability..."
Assert-ServerReachable
Write-Host "Server is reachable."

Write-Host "[2/4] Creating verification user and token..."
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$email = "precheck.$ts@example.com"
$phone = "9" + ($ts.ToString().Substring(1, 9))

$register = Invoke-WrappedApi -Name "AuthRegister" -Path "/api/v1/auth/register" -Token "" -Data @{
    name = "Precheck User"
    email = $email
    password = "StrongPass@123"
    phone = $phone
}

if (-not $register.Ok -or $register.Status -ne "SUCCESS" -or -not $register.Data.token) {
    throw "Pre-release check failed at AuthRegister: $($register.Message)"
}

$token = $register.Data.token
Write-Host "Token acquired."

Write-Host "[3/4] Running schema-compatibility probes (critical endpoints)..."
$customerId = "39225d99-1f70-11f1-9651-ed7fb304f8d2"
$productId = "38e45a96-1f70-11f1-9651-ed7fb304f8d2"

$probes = @()
$probes += Invoke-WrappedApi -Name "ProductsDetailProbe" -Path "/api/v1/products/detail" -Token $token -Data @{ productId = $productId }
$probes += Invoke-WrappedApi -Name "CouponValidateProbe" -Path "/api/v1/coupons/validate" -Token $token -Data @{
    customerId = $customerId
    couponCode = "SAVE10"
    orderAmount = 1500.00
}

$probeTable = $probes | Select-Object Api, Ok, Status, Message
$probeTable | Format-Table -AutoSize

$failedProbes = $probes | Where-Object { -not $_.Ok -or $_.Status -ne "SUCCESS" }
if ($failedProbes.Count -gt 0) {
    throw "Pre-release compatibility probe failed. Fix these endpoints before running release smoke checks."
}

Write-Host "Critical probes passed."

if ($SkipSmoke) {
    Write-Host "[4/4] Smoke run skipped by flag."
    Write-Host "Pre-release checklist PASSED (probes only)."
    exit 0
}

Write-Host "[4/4] Running smoke suite..."
powershell -ExecutionPolicy Bypass -File .\run_api_smoke.ps1

if ($LASTEXITCODE -ne 0) {
    throw "Smoke script execution failed with exit code $LASTEXITCODE"
}

if (Test-Path .\api_test_results.txt) {
    Get-Content .\api_test_results.txt | Out-String | Write-Host

    $failedLines = Select-String -Path .\api_test_results.txt -Pattern "FAILED"
    if ($failedLines) {
        throw "Smoke results contain FAILED entries."
    }
}

Write-Host "Pre-release checklist PASSED (probes + smoke)."
exit 0
