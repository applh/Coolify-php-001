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
- AI Team Applet plan can be found in [docs/ai-team-plan.md](docs/ai-team-plan.md).
- AI Team Session Management guide can be found in [docs/ai-team-session-management.md](docs/ai-team-session-management.md).
- Gemini API Integration and Android SDK Error Guide can be found in [docs/android-gemini-integration.md](../docs/android-gemini-integration.md).
- Automated Backup Manager implementation plan can be found in [docs/backup-manager-plan.md](docs/backup-manager-plan.md).
- **AI Agent Applet Complexity & Prompting Guide** can be found in [docs/ai-agent-applet-complexity-guide.md](docs/ai-agent-applet-complexity-guide.md).

## Deployment (Coolify / Docker)
This module is ready for standalone deployment as a build server:
- **Dockerfile**: Includes Android SDK 35 and Gradle 8.7.
- **Port**: Serves the generated APK on port 3000.
- **Coolify Setup**: Point Coolify to the `repo-android` directory and use the provided `Dockerfile`.
- **Resources**: Ensure your server has at least 4GB of RAM for the build process.
