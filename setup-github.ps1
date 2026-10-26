# Legt das GitHub-Repo Flexy06/ilias-mobile an, hinterlegt den Signaturschluessel
# als Actions-Secrets und pusht den Code. Danach baut GitHub Actions die APK automatisch.
# Voraussetzung: git und GitHub CLI (winget install GitHub.cli), einmal "gh auth login".
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$repo = "Flexy06/ilias-mobile"
$pw = (Get-Content "signing\password.txt" -Raw).Trim()
$ks = [Convert]::ToBase64String([IO.File]::ReadAllBytes("$PSScriptRoot\signing\release.jks"))

if (-not (Test-Path ".git")) {
    git init -b main
    git add .
    git commit -m "ILIAS-App fuer die HKA: Compose + WebView, dauerhafter Login"
}

gh repo create $repo --public --description "Android-App fuer ILIAS der Hochschule Karlsruhe" --source . --remote origin
gh secret set SIGNING_KEYSTORE_B64 --repo $repo --body $ks
gh secret set SIGNING_STORE_PASSWORD --repo $repo --body $pw
gh secret set SIGNING_KEY_PASSWORD --repo $repo --body $pw
gh secret set SIGNING_KEY_ALIAS --repo $repo --body "ilias"
git push -u origin main

Write-Host ""
Write-Host "Fertig! Build laeuft: https://github.com/$repo/actions"
Write-Host "APK danach unter:     https://github.com/$repo/releases/latest"
Write-Host "Bewahre den Ordner 'signing' gut auf - ohne ihn kannst du keine Updates ueber die installierte App spielen."
