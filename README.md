# Morse Flash (Android)

An Android app that uses the camera flashlight to blink Morse code.

## Features
- Flash SOS with one tap
- Enter custom text (A–Z, 0–9, space) to flash
- Stop button to cancel flashing

## Build
Open the project in Android Studio (Giraffe+ recommended) and press Run.

If you prefer CLI and have the Android SDK configured, you can try:
```bash
./gradlew :app:assembleDebug
```
The APK will be in `app/build/outputs/apk/debug/`.

## Permissions
- Camera permission is requested to control the torch.
