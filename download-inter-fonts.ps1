# PowerShell script to download Inter font files (Latin/English only)
# Save this as: download-inter-fonts.ps1

param(
    [string]$ProjectPath = "."
)

# Define paths
$FontsDir = Join-Path $ProjectPath "src\main\resources\static\fonts"
$CssDir = Join-Path $ProjectPath "src\main\resources\static\css"

# Create directories
Write-Host "Creating directories..." -ForegroundColor Cyan
New-Item -ItemType Directory -Path $FontsDir -Force | Out-Null
New-Item -ItemType Directory -Path $CssDir -Force | Out-Null

# Correct URLs for each weight (Latin only) - extracted from your CSS
$FontFiles = @(
    @{
        Url = "https://fonts.gstatic.com/s/inter/v19/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1ZL7W0I5nvwU.woff2"
        FileName = "Inter-Light.woff2"
        Weight = "300"
    },
    @{
        Url = "https://fonts.gstatic.com/s/inter/v19/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1ZL7W0I5nvwU.woff2"  
        FileName = "Inter-Regular.woff2"
        Weight = "400"
    },
    @{
        Url = "https://fonts.gstatic.com/s/inter/v19/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1ZL7W0I5nvwU.woff2"
        FileName = "Inter-Medium.woff2"
        Weight = "500"
    },
    @{
        Url = "https://fonts.gstatic.com/s/inter/v19/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1ZL7W0I5nvwU.woff2"
        FileName = "Inter-SemiBold.woff2"
        Weight = "600"
    },
    @{
        Url = "https://fonts.gstatic.com/s/inter/v19/UcC73FwrK3iLTeHuS_nVMrMxCp50SjIa1ZL7W0I5nvwU.woff2"
        FileName = "Inter-Bold.woff2"
        Weight = "700"
    }
)

Write-Host "Downloading Inter fonts (Latin only)..." -ForegroundColor Cyan
Write-Host "Target directory: $FontsDir" -ForegroundColor Yellow

# Download fonts
$SuccessCount = 0
foreach ($Font in $FontFiles) {
    $FilePath = Join-Path $FontsDir $Font.FileName
    Write-Host "Downloading $($Font.FileName) (weight: $($Font.Weight))..." -ForegroundColor Green
    
    try {
        Invoke-WebRequest -Uri $Font.Url -OutFile $FilePath -UseBasicParsing
        
        if (Test-Path $FilePath) {
            $Size = [math]::Round((Get-Item $FilePath).Length/1KB, 2)
            Write-Host "  [OK] $($Font.FileName) ($Size KB)" -ForegroundColor Green
            $SuccessCount++
        } else {
            Write-Host "  [FAIL] File not created: $($Font.FileName)" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "  [ERROR] Failed to download $($Font.FileName): $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Create CSS file
Write-Host "`nCreating CSS file..." -ForegroundColor Cyan

$CssContent = @'
/* Inter Font - Local Files (Latin only) */

@font-face {
    font-family: 'Inter';
    font-style: normal;
    font-weight: 300;
    font-display: swap;
    src: url('../fonts/Inter-Light.woff2') format('woff2');
    unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}

@font-face {
    font-family: 'Inter';
    font-style: normal;
    font-weight: 400;
    font-display: swap;
    src: url('../fonts/Inter-Regular.woff2') format('woff2');
    unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}

@font-face {
    font-family: 'Inter';
    font-style: normal;
    font-weight: 500;
    font-display: swap;
    src: url('../fonts/Inter-Medium.woff2') format('woff2');
    unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}

@font-face {
    font-family: 'Inter';
    font-style: normal;
    font-weight: 600;
    font-display: swap;
    src: url('../fonts/Inter-SemiBold.woff2') format('woff2');
    unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}

@font-face {
    font-family: 'Inter';
    font-style: normal;
    font-weight: 700;
    font-display: swap;
    src: url('../fonts/Inter-Bold.woff2') format('woff2');
    unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
}
'@

$CssFilePath = Join-Path $CssDir "fonts.css"
try {
    $CssContent | Out-File -FilePath $CssFilePath -Encoding UTF8
    Write-Host "[OK] CSS file created: fonts.css" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Failed to create CSS file: $($_.Exception.Message)" -ForegroundColor Red
}

# Display summary
Write-Host "`n" + ("="*50) -ForegroundColor Cyan
Write-Host "DOWNLOAD SUMMARY" -ForegroundColor Cyan
Write-Host ("="*50) -ForegroundColor Cyan

Write-Host "`nSuccessfully downloaded: $SuccessCount out of $($FontFiles.Count) fonts" -ForegroundColor Yellow

if (Test-Path $FontsDir) {
    Write-Host "`nDownloaded files:" -ForegroundColor Yellow
    Get-ChildItem $FontsDir -Filter "*.woff2" | ForEach-Object {
        $Size = [math]::Round($_.Length/1KB, 2)
        Write-Host "  - $($_.Name) ($Size KB)" -ForegroundColor White
    }
}

Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "1. Add this line to your HTML <head> section:" -ForegroundColor White
Write-Host '   <link rel="stylesheet" href="/css/fonts.css">' -ForegroundColor Gray
Write-Host "2. Remove any Google Fonts links from your HTML" -ForegroundColor White
Write-Host "3. Your CSS font-family: 'Inter' will now use local fonts" -ForegroundColor White

Write-Host "`nProject structure created:" -ForegroundColor Yellow
Write-Host "src/main/resources/static/" -ForegroundColor White
Write-Host "|-- fonts/" -ForegroundColor White
Write-Host "|   |-- Inter-Light.woff2" -ForegroundColor Gray
Write-Host "|   |-- Inter-Regular.woff2" -ForegroundColor Gray
Write-Host "|   |-- Inter-Medium.woff2" -ForegroundColor Gray
Write-Host "|   |-- Inter-SemiBold.woff2" -ForegroundColor Gray
Write-Host "|   +-- Inter-Bold.woff2" -ForegroundColor Gray
Write-Host "+-- css/" -ForegroundColor White
Write-Host "    +-- fonts.css" -ForegroundColor Gray

Write-Host "`nDone!" -ForegroundColor Green
