# Android APK Build Guide

This document explains **how** and **when** the Android application (CameraX Companion App) is built, and how it is distributed through the main dashboard.

## 1. Overview

The platform includes a native Android companion app located in the `repo-android/` directory. This Kotlin and Jetpack Compose-based application allows users to capture visual assets seamlessly. 

The web dashboard provides a direct download link and a QR code for the compiled APK via the `/api/android/download` endpoint. However, the Node.js server itself does **not** compile the APK at runtime. The APK must be pre-built.

## 2. When is the APK Built?

Due to the heavy resource requirements of the Android SDK and Gradle, the APK is not built dynamically during Node.js server startup. Instead, it should be built during one of the following parts:

1.  **Local Development:** Manually built by developers before committing or testing the download endpoint.
2.  **CI/CD Pipeline (Recommended):** Built via GitHub Actions (or similar) prior to deploying the Docker container.
3.  **Dedicated Build Server:** Run via an orchestration layer (like Coolify) using a custom CI script before the Node server starts serving the final images.

## 3. How to Build the APK (Manually)

To build the debug APK locally, ensure you have the [Android SDK](https://developer.android.com/studio) installed, then run the Gradle wrapper from within the `repo-android` directory.

```bash
cd repo-android

# Ensure the gradle wrapper is executable
chmod +x gradlew

# Build the debug APK
./gradlew assembleDebug
```

After a successful build, the APK will be generated at:
`repo-android/app/build/outputs/apk/debug/app-debug.apk`

As long as the APK exists at that path, the Node.js express server will serve it to users who scan the QR code or click the download button on the Site Dashboard.

## 4. Serving the APK in Production

The main backend server (`server.ts`) includes a dedicated endpoint for serving the APK:

```javascript
app.get('/api/android/download', async (req, res) => {
  const apkPath = path.join(rootDir, 'repo-android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
  try {
    await fs.access(apkPath);
    res.download(apkPath, 'CameraXApp-debug.apk');
  } catch {
    res.status(404).json({ error: 'APK build not found. Please run build in repo-android.' });
  }
});
```

### Docker Considerations

When deploying the full framework using Docker, you must ensure that the generated `app-debug.apk` is included in the Docker context.

Add a build step to your `Dockerfile` if your CI/CD runner has the Android SDK available, or simply ensure the file is generated entirely prior to the `docker build` command. Since `.dockerignore` might exclude build directories, ensure `repo-android/app/build` is selectively permitted if you build outside of Docker and then copy it in.

## 5. Security & Signing

Currently, the endpoint serves the `debug` build. For a production-ready application, you should:
1. Generate a keystore and sign the APK.
2. Run `./gradlew assembleRelease` instead.
3. Update the `server.ts` path to point to the `release` APK directory (`repo-android/app/build/outputs/apk/release/app-release.apk`).
4. Consider distributing through Google Play to bypass "Unknown Sources" warnings on user devices.
