# Architecture Decisions Record

## 1. Gemini API Integration & Proxy Routing

### Context
The project contains multiple applications designed to interact with the Gemini API, primarily a web frontend (Vue.js) and an Android mobile application (`repo-android`). 

A bug emerged where a `404 NOT_FOUND` error was being thrown when calling the Gemini API, accompanied by a `MissingFieldException` in the Android SDK parsing. 

### Decisions & Corrections

1. **Model Version Correction (`gemini-1.5-flash`)**
   - The system was previously referencing `gemini-flash-latest` and `gemini-1.5-flash-latest`.
   - **Fix**: The model names have been standardized to `gemini-1.5-flash` across all applications. The `latest` alias was not properly resolving in the `v1beta` genai SDK, which caused the 404 error.

2. **Middleware API Server (`server.ts`) for Web Frontend**
   - **Why `server.ts` is involved**: For the Vue web application, the Gemini API is called via a Node.js express middleware router (`server.ts`).
   - **Reason**: The AI Studio platform architecture requires that web browser clients proxy their requests through an internal server to securely inject the protected `GEMINI_API_KEY` environment variable. Pushing the API key to the client's browser would create a security leak.

3. **Direct Client Integration for Android (`repo-android`)**
   - **Decision**: The Android application does **not** rely on `server.ts`. It directly utilizes the Google GenAI SDK for Android (`GenerativeModel`).
   - **Reason**: Mobile app builds handle secrets/keys natively through property injection during the build process, and maintaining absolute privacy by eliminating the middleware server is desirable and possible in the client-side binary. The Web application is subject to different constraints since its code is served directly over the network to a browser.

### Conclusion
- The **web application** uses a proxy model (`server.ts`) for strict security and infrastructure compliance.
- The **Android application** uses a direct client-to-API model for optimal privacy and lower latency.
- Both refer to `gemini-1.5-flash` reliably.
