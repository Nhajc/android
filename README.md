# Nhajc Android

The Android app of Nhajc, a music streaming app.

## Build and Run

### Requirements

- Android Studio, or the Android SDK
- A running Nhajc server (see the backend README)

> `adb` is in `<Android SDK>/platform-tools` and `emulator` is in `<Android SDK>/emulator`. On Windows, use `gradlew.bat` instead of `./gradlew`.

### 1. Start a device

#### a. Emulator: start one from Android Studio's Device Manager, or:

```sh
emulator -list-avds
emulator -avd <name> # Pixel_4a for example
```

#### b. Phone or tablet: turn on USB debugging in Developer options, connect it with USB and allow the computer.

### 2. Build and install

#### a. Server on this computer: forward the ports through adb.

```sh
adb reverse tcp:8080 tcp:8080 # API
adb reverse tcp:9000 tcp:9000 # MinIO (images and audio)
./gradlew installDebug
```

> Run `adb reverse` again every time the device reconnects or restarts.

#### b. Server on another address: the device must reach `<address>` and the server's `MINIO_PUBLIC_ENDPOINT` (images and audio).

```sh
./gradlew installDebug -Pnhajc.serverUrl=http://<address>:8080/api/v1/
```

### Other commands

* Builds the app without installing: `./gradlew assembleDebug`
* Shows the connected devices: `adb devices`
* Shows the forwarded ports: `adb reverse --list`
* Stops Gradle in the background: `./gradlew --stop`
* Formats the Kotlin code: `./gradlew ktlintFormat`
* Checks the formatting (the same check runs on every pull request): `./gradlew ktlintCheck`
* Formats your staged Kotlin files on every commit (run once per clone): `./gradlew addKtlintFormatGitPreCommitHook`
