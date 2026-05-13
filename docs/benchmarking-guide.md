# Benchmarking Guide

The PHP CMS Manager includes a built-in benchmarking tool to monitor the performance and availability of your multisite network and external resources.

## Accessing the Benchmarker

Navigate to the **Benchmarks** section in the main navigation.

## Configuration

The benchmarker uses a JSON configuration to define the sites you wish to test. 

### Format

The input expects an array of objects:

```json
[
  {
    "name": "Production Site",
    "url": "https://mysite.com"
  },
  {
    "name": "Staging Site",
    "url": "https://staging.mysite.com"
  }
]
```

## Metrics Tracked

1. **Response Time**: Measured in milliseconds (ms) from the server's perspective to handle the request.
2. **Status Code**: The HTTP response code returned by the destination.
3. **Availability**: Real-time status indicating if the site is reachable.

## Technical Implementation

- **Backend**: `/api/benchmark` endpoint in `server.ts` uses Node.js `fetch` with a 5-second timeout.
- **Frontend**: `SiteBenchmarker.vue` provides a technical dashboard interface with real-time feedback and progress tracking.

> [!NOTE]
> Server-side benchmarking helps bypass CORS issues that typically occur when measuring performance directly from the browser.
