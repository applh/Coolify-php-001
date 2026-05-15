# Gemini API Interactions & Session Memory Guide

This guide details the various interaction patterns available with the Gemini API using the `@google/genai` SDK, with a specific focus on maintaining session context and persistent memory.

## 1. Interaction Models

### Single-Turn Generation (`generateContent`)
Best for stateless, direct requests where context from previous messages is not needed.
```ts
const response = await ai.models.generateContent({
  model: "gemini-3-flash-preview",
  contents: "Summarize the key benefits of AI Agents in 3 bullet points."
});
console.log(response.text);
```

### Multimodal Generation
Sending text alongside images or other media.
```ts
const response = await ai.models.generateContent({
  model: "gemini-3-flash-preview",
  contents: {
    parts: [
      { text: "Describe this image:" },
      { inlineData: { data: base64Data, mimeType: "image/png" } }
    ]
  }
});
```

### Streaming Responses (`generateContentStream`)
Essential for long responses to improve perceived performance.
```ts
const stream = await ai.models.generateContentStream({
  model: "gemini-3-flash-preview",
  contents: "Write a detailed 500-word blog post about PHP CMS architecture."
});

for await (const chunk of stream) {
  process.stdout.write(chunk.text);
}
```

---

## 2. Managing Session Context (Persistent Memory)

To get a "common session context" running across multiple API requests, you have two primary strategies:

### Strategy A: The `Chat` Interface (Managed State)
The SDK provides a `chat` object that internally tracks the conversation history. This is the recommended way for interactive applications.

```ts
// 1. Initialize the chat session
const chat = ai.chats.create({
  model: "gemini-3-flash-preview",
  config: {
    systemInstruction: "You are a helpful technical assistant specializing in PHP and Vue.js."
  }
});

// 2. Send messages sequentially
// The 'chat' object automatically appends the history behind the scenes.
const res1 = await chat.sendMessage({ message: "What is PSR-12?" });
const res2 = await chat.sendMessage({ message: "Can you give me an example code block?" });
// res2 will 'know' you are talking about PSR-12.
```

### Strategy B: Manual History Management (Persistent Context)
If you need to save the session to a database or resume it after a browser refresh, you must handle the `contents` array manually.

**The Context Pattern:**
Gemini expects the `contents` array to follow a specific structure: `[{ role: 'user', parts: [...] }, { role: 'model', parts: [...] }]`.

```ts
// 1. Your stored history (e.g., from a database or localStorage)
const history = [
  { role: "user", parts: [{ text: "I'm building a CMS named 'BabiBlog'." }] },
  { role: "model", parts: [{ text: "That sounds great! How can I help with BabiBlog?" }] }
];

// 2. The new request appending the history
const response = await ai.models.generateContent({
  model: "gemini-3-flash-preview",
  contents: [
    ...history,
    { role: "user", parts: [{ text: "What should be the main layout structure for it?" }] }
  ]
});

// 3. Update your history with the new exchange
const newExchange = [
  { role: "user", parts: [{ text: "What should be the main layout structure for it?" }] },
  { role: "model", parts: [{ text: response.text }] }
];
const updatedHistory = [...history, ...newExchange];
```

---

## 3. Persistent Memory Across Multiple User Sessions

To maintain a "True Memory" that persists even if the code stops running:

1.  **State Extraction**: At the end of every interaction, the model can be asked to "Update a summary of facts about the user/project" (Summarization state).
2.  **Storage**: Save this summary string and the raw `history` array to a database (like Firestore).
3.  **Bootstrap**: When the user returns, load the summary and inject it into the `systemInstruction` or as the first message in the `history`.

### Example: The "Memory Bootstrap"
```ts
const userProfile = await db.getUserProfile(uid); // e.g., "User prefers dark mode and PHP"

const chat = ai.chats.create({
  model: "gemini-3.1-pro-preview",
  config: {
    systemInstruction: `You are an AI developer. Remember these facts about the user: ${userProfile}`
  },
  // If the SDK supports history parameter in create (standard behavior):
  history: await db.getChatHistory(sessionId) 
});
```

---

## 4. Advanced Interaction Features

### Function Calling
Allows the model to trigger code execution in your app (e.g., searching a database).
```ts
const tool = {
  functionDeclarations: [{
    name: "get_site_content",
    parameters: {
      type: Type.OBJECT,
      properties: { domain: { type: Type.STRING } }
    }
  }]
};

const response = await ai.models.generateContent({
  model: "gemini-3.1-pro-preview",
  contents: "Tell me what is on site1.com",
  config: { tools: [tool] }
});
```

### Grounding (Search/Maps)
Connects the model to real-time data.
```ts
const response = await ai.models.generateContent({
  model: "gemini-3-flash-preview",
  contents: "What is the current version of Tailwind CSS?",
  config: { tools: [{ googleSearch: {} }] }
});
```

---

## 5. Summary Table: Which Interaction to Choose?

| Goal | Method | Context Management |
| :--- | :--- | :--- |
| Quick summary / Q&A | `generateContent` | None (Single turn) |
| Interactive Web Chat | `ai.chats.create` | Automatic (In-memory) |
| Multi-tab / Resume Session | `generateContent` | Manual `contents` array |
| Low-latency Voice | `ai.live.connect` | Real-time WebSocket |
| Massive Content Extraction | `generateContent` | `urlContext` tool |
