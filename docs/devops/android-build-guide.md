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

### Option A: Via the Main Node.js Backend
The main backend server (`server.ts`) includes a dedicated endpoint for serving the APK:

```javascript
app.get('/api/android/download', async (req, res) => {
  const apkPath = path.join(rootDir, 'repo-android', 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
  // ...
});
```

### Option B: As a Standalone Container (Coolify)
The `repo-android/Dockerfile` allows you to deploy a dedicated "Download & Build" service. This service:
1. Compiles the APK within the container.
2. Serves a simple landing page on port 3000 where users can download `app.apk`.

## 5. Common Deployment Errors (Coolify / Docker)

If you encounter errors during deployment, check the following:

- **Out of Memory (OOM):** Android builds are resource-intensive. If your server has less than 4GB of RAM, the Gradle build might fail. We have set the default heap to `3072m` in the `Dockerfile`. If it still fails, increase your VPS specs or build the APK locally and push it to the repo.
- **Build Timeout:** The first build can take over 10 minutes. Ensure Coolify's build timeout is sufficient.
- **Port Conflicts:** Ensure port `3000` is available or mapped correctly in your Coolify application settings.
- **Health Check Failures:** Coolify expects the container to respond to HTTP requests. The `Dockerfile` includes a healthcheck that pings the Nginx root.

## 6. Security & Signing

Currently, the endpoint serves the `debug` build. For a production-ready application, you should:
1. Generate a keystore and sign the APK.
2. Run `./gradlew assembleRelease` instead.
3. Update the `server.ts` path to point to the `release` APK directory (`repo-android/app/build/outputs/apk/release/app-release.apk`).
4. Consider distributing through Google Play to bypass "Unknown Sources" warnings on user devices.
