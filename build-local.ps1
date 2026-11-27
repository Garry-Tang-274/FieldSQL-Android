param([string]$Sdk = $env:ANDROID_HOME,[string]$Gradle)
$ErrorActionPreference = 'Stop'
if (!$Sdk) { $Sdk = "$env:LOCALAPPDATA\FieldSQL-Build\sdk" }
if (!(Test-Path -LiteralPath "$Sdk\platforms\android-35\android.jar")) { throw 'Set ANDROID_HOME to your Android SDK with platform 35 and build-tools 35.0.0.' }
$homeDir = "$env:LOCALAPPDATA\FieldSQL-Build"
$previous=@{};foreach($n in 'ANDROID_HOME','GRADLE_USER_HOME','FIELDSQL_KEYSTORE','FIELDSQL_SIGN_PASS','JAVA_TOOL_OPTIONS'){$previous[$n]=[Environment]::GetEnvironmentVariable($n,'Process')}
try {
$env:ANDROID_HOME = $Sdk
if (!$Gradle) {
  $installed=Join-Path $homeDir 'gradle-8.11.1\bin\gradle.bat'
  $Gradle=if(Test-Path -LiteralPath $installed){$installed}else{"$PSScriptRoot\gradlew.bat"}
}
if (!$env:GRADLE_USER_HOME -and (Test-Path -LiteralPath "$homeDir\gradle-cache")) {$env:GRADLE_USER_HOME="$homeDir\gradle-cache"}
$keys = Join-Path $homeDir 'signing'
New-Item -ItemType Directory -Path $keys -Force | Out-Null
$env:FIELDSQL_KEYSTORE = Join-Path $keys 'FieldSQL.p12'
$credentialFile = Join-Path $keys 'password.dpapi'
if (!(Test-Path $credentialFile) -and !(Test-Path $env:FIELDSQL_KEYSTORE)) {
  $raw = [byte[]]::new(36); [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($raw)
  ConvertTo-SecureString ([Convert]::ToBase64String($raw)) -AsPlainText -Force | ConvertFrom-SecureString | Set-Content -LiteralPath $credentialFile
}
if (!(Test-Path -LiteralPath $credentialFile)) {throw 'The signing key is present but its credential is missing.'}
$sec = Get-Content -Raw -LiteralPath $credentialFile | ForEach-Object {ConvertTo-SecureString $_.Trim()}
$env:FIELDSQL_SIGN_PASS = ([Net.NetworkCredential]::new('', $sec)).Password
  if (!(Test-Path -LiteralPath $env:FIELDSQL_KEYSTORE)){
    & keytool -genkeypair -storetype PKCS12 -keystore $env:FIELDSQL_KEYSTORE -storepass:env FIELDSQL_SIGN_PASS -alias fieldsql -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=FieldSQL Local'
    if ($LASTEXITCODE) {throw 'Could not generate the local signing key.'}
  }
  if ($env:HTTPS_PROXY) {
    $u=[uri]$env:HTTPS_PROXY
    $env:JAVA_TOOL_OPTIONS=($env:JAVA_TOOL_OPTIONS + " -Dhttps.proxyHost=$($u.Host) -Dhttps.proxyPort=$($u.Port) -Dhttp.proxyHost=$($u.Host) -Dhttp.proxyPort=$($u.Port)").Trim()
  }
  & $Gradle -p $PSScriptRoot :app:assembleRelease --console=plain
  if ($LASTEXITCODE) {throw 'Release build failed.'}
  Write-Output "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk"
} finally { foreach($n in $previous.Keys){[Environment]::SetEnvironmentVariable($n,$previous[$n],'Process')} }
