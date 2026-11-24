# Nhajc Android

## Local backend

Start the backend with PostgreSQL and MinIO first. The default API URL is
`http://localhost:8080/api/v1/`. With the emulator running, forward its port to the
development machine before launching the app:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
```

Run the forwarding command again after restarting ADB or the emulator. To use a
different backend, set the `nhajc.serverUrl` Gradle property and rebuild the app.
Search uses the app's existing login session and the real backend database. An
empty catalog displays an empty state; there is no sample-data fallback.

## Verification

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.verifyBackend=true'
```

The second command needs a connected emulator, the port forwarding above, and a
running backend. It checks the filter layout, multiple selections, and API
reachability from the Android app process. The backend's
`scripts/verify-search.py` also verifies search, pagination, privacy, and
validation against PostgreSQL, then removes only its own temporary records.
