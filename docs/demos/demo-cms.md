# CMS Demo Documentation

## Overview
**CMS.demo** (CoreCMS) is a technical powerhouse designed to showcase the "Headless" and "API-First" capabilities of the platform. It functions both as a developer documentation site and a live API provider.

## Key Features
- **Dynamic API Routes**: Routes under `/app/api/*` return JSON instead of HTML.
- **API Console**: An interactive dashboard (`/app`) allow users to test live API endpoints directly in the browser.
- **Technical Aesthetic**: Uses 'JetBrains Mono' and a dark, high-contrast theme suitable for developer tools.
- **Stat Generation**: Random data generation for API responses to simulate live engine activity.

## Key Endpoints
- `GET /app/api/site-info`: Returns technical metadata about the CMS instance.
- `GET /app/api/stats`: Returns simulated uptime and usage metrics.

## Implementation Details
The `index.php` checks for the `app/api` prefix in the URI. If found, it clears the buffer, sets the `Content-Type: application/json` header, and exits after outputting the requested data. This demonstrates how to build "Headless" backends within the same CMS structure.
