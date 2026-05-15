# WebApps Demo Documentation

## Overview
**webapps.demo** (LogiFlow) moves beyond static content to demonstrate a full-featured "Logistics Control Center" web application. It combines traditional marketing pages with a rich, interactive dashboard.

## Key Features
- **Interactive Dashboard**: The `/app` route loads a full-screen application UI rather than a standard webpage.
- **Google Maps Integration**: Features a live fleet tracking map (requires `GOOGLE_MAPS_PLATFORM_KEY`).
- **Sidebar Navigation**: Dedicated application navigation separate from the public site.
- **Fleet Management UI**: Real-time status indicators, active shipment lists, and map markers.
- **Marketing Frontend**: 10 pages focused on logistics solutions and carrier onboarding.

## The Dashboard (`/app`)
- **Live Fleet View**: Visualizes vehicle locations.
- **Inventory & Shipments**: Sidebar links to different modules of the internal tool.
- **Syncing States**: Includes UI for "Live Sync" status to simulate real-time data flow.

## Setup
To enable the interactive map, ensure your `.env` contains a valid `GOOGLE_MAPS_PLATFORM_KEY`. Without it, a placeholder component will be rendered.
