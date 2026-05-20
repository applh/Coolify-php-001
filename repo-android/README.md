# Android Multi-App Hub

This project is a multi-purpose Android application built with Jetpack Compose. It serves as a container for multiple specialized "applets".

## Features
- **Launcher Hub**: Quick access to all integrated applets.
- **CameraX Applet**: A fully functional camera application.
- **File Explorer Applet**: Browse and manage local device storage.
- **Settings Applet**: Manage shared configurations.

## Architecture
Built with modern Android practices:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Navigation**: Compose Navigation
- **Camera**: CameraX API

## Documentation
- Detailed implementation plan can be found in [docs/multi-app-plan.md](docs/multi-app-plan.md).
- Gemini API Integration and Android SDK Error Guide can be found in [docs/android-gemini-integration.md](../docs/android-gemini-integration.md).

## Deployment (Coolify / Docker)
This module is ready for standalone deployment as a build server:
- **Dockerfile**: Includes Android SDK 35 and Gradle 8.7.
- **Port**: Serves the generated APK on port 3000.
- **Coolify Setup**: Point Coolify to the `repo-android` directory and use the provided `Dockerfile`.
- **Resources**: Ensure your server has at least 4GB of RAM for the build process.
