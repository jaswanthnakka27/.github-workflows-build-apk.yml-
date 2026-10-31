# KAREN

A native Android AI assistant powered by the Gemini API.

## Build on GitHub
1. Create a new GitHub repository.
2. Upload the contents of this ZIP (the files inside the `KAREN` folder, not the folder itself).
3. Open **Actions**. The workflow builds the debug APK automatically after push.
4. Open the completed workflow run and download the `KAREN-debug-apk` artifact.
5. Extract the artifact ZIP and install `app-debug.apk` on your Android phone.

You can also open the project in Android Studio and run `:app:assembleDebug`.

## Connect Gemini
1. Install and open KAREN.
2. Tap the gear button.
3. Paste your Gemini API key and save.
4. Ask KAREN a question.

Get an API key from Google AI Studio: https://aistudio.google.com/app/apikey

## Security note
The key is saved in the app's private preferences on this device. Do not paste your key into Kotlin source code, README files, GitHub commits, screenshots, or public repositories. For a public release, use a small authenticated backend rather than shipping a long-lived API key in an APK.

## Current scope
- Gemini text chat
- Android speech recognition input (uses the device's available speech recognition service)
- Local API-key setting
- Native dark sci-fi interface
- GitHub Actions debug APK build

The app needs internet access for Gemini. Speech recognition availability and offline behavior depend on the Android device and installed speech services.
