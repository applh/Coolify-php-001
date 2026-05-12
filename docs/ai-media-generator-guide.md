# AI Media Generator Guide

The CMS features a built-in AI Media Generator powered by Gemini, allowing for seamless image generation directly into your site folders.

## Architecture

The system uses a hybrid integration pattern to maximize security and efficiency:

1.  **Frontend Generation (API Caller):** The `AiMediaTasks.vue` view handles the Gemini API integration. By calling Google's API directly from your browser, your API Key stays secure and never touches our intermediate logs.
2.  **Backend Storage (The Saver):** Once an image is generated, the frontend sends the base64 data to the Express backend (`POST /api/media/save-base64`), which converts it to a binary file and saves it into the correct `repo-php/my-data` subfolder.

## Usage Guide

1. **Obtain API Key**: Get your free API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2. **Scan for Missing Media**: The dashboard automatically scans your PHP templates for `<img>` tags referencing files that don't exist.
3. **Run the Queue**:
   - Go to the **AI Media** section.
   - Enter your API Key.
   - select the tasks you wish to process.
   - Click **Run Queue**.

## Technical Workflow

### 1. Install Gemini SDK
Install the official `@google/genai` package for the frontend.
```bash
npm install @google/genai
```

### 2. Frontend Component (`AiMediaGenerator.vue`)

The Vue application will handle the UI and Gemini API integration:

```html
<template>
  <div class="ai-generator-panel bg-[#181818] p-4 rounded border border-[#2A2A2A]">
    <h3 class="text-xl font-semibold mb-4 text-white">Gemini Image Generator</h3>
    
    <input 
      v-model="apiKey" 
      type="password" 
      placeholder="Gemini API Key" 
      class="w-full bg-[#121212] p-2 mb-3 rounded"
    />
    
    <textarea 
      v-model="prompt" 
      placeholder="Describe the image you want..." 
      class="w-full bg-[#121212] p-2 mb-3 rounded h-24"
    ></textarea>
    
    <button 
      @click="generateAndSave" 
      :disabled="isGenerating"
      class="bg-[#F27D26] text-black px-4 py-2 rounded font-semibold w-full"
    >
      {{ isGenerating ? 'Generating...' : 'Generate & Save' }}
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { GoogleGenAI } from '@google/genai';

const apiKey = ref('');
const prompt = ref('');
const isGenerating = ref(false);
const targetPath = ref('content/site1.com/images'); // Current explorer path

const generateAndSave = async () => {
    if (!apiKey.value || !prompt.value) {
      alert('API Key and Prompt are required.');
      return;
    }
    
    isGenerating.value = true;
    try {
        // 1. Initialize Gemini API
        const ai = new GoogleGenAI({ apiKey: apiKey.value });
        
        // 2. Call the image generation model
        const response = await ai.models.generateContent({
          model: 'gemini-2.5-flash-image',
          contents: {
            parts: [
              { text: prompt.value },
            ],
          },
          config: {
            imageConfig: {
              aspectRatio: "1:1",
            }
          }
        });

        // 3. Extract the image from the response parts
        let base64ImageData = null;
        for (const part of response.candidates[0].content.parts) {
          if (part.inlineData) {
            base64ImageData = part.inlineData.data;
            break;
          }
        }
        
        if (!base64ImageData) {
          throw new Error('No image generated.');
        }

        // 4. Send the base64 string to the backend to save
        const res = await fetch('/api/media/save-base64', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                imageBase64: base64ImageData,
                targetPath: targetPath.value,
            })
        });
        
        const data = await res.json();
        if(data.success) {
            alert('Image saved successfully to: ' + data.savedPath);
            // Trigger refresh in Explorer view
        } else {
            throw new Error(data.error || 'Failed to save to backend');
        }
    } catch (err) {
        console.error(err);
        alert('Failed to generate or save image: ' + err.message);
    } finally {
        isGenerating.value = false;
    }
};
</script>
```

### 3. Backend Endpoint (`server.ts`)

The backend will only focus on securely saving the base64 content received from the frontend to the repository.

```typescript
import fs from 'fs/promises';
import path from 'path';

app.post('/api/media/save-base64', express.json({ limit: '10mb' }), async (req, res) => {
  try {
    const { imageBase64, targetPath } = req.body;
    
    if (!imageBase64 || !targetPath) {
       return res.status(400).json({ error: 'Missing image data or target path.' });
    }

    // Convert base64 back to binary data
    const buffer = Buffer.from(imageBase64, 'base64');

    // Generate filename and save path
    const fileName = `gemini_gen_${Date.now()}.png`;
    const fullPath = path.join(repoPhpPath, targetPath, fileName);
    
    // Security check to avoid path traversal
    if (!fullPath.startsWith(repoPhpPath)) {
        return res.status(403).json({ error: 'Invalid save path' });
    }

    // Save to the filesystem
    await fs.writeFile(fullPath, buffer);

    res.json({ success: true, savedPath: path.join(targetPath, fileName).replace(/\\/g, '/') });

  } catch (error) {
    console.error('File Save Error:', error);
    res.status(500).json({ error: 'Failed to save generated image' });
  }
});
```

## Security Considerations
1.  **API Key Management:** By requesting the API key on the frontend and keeping it locally in Vue state, the key goes directly from the user's browser to Google's API, skipping the intermediate proxy layer and enhancing privacy.
2.  **Payload Limits:** Generating images produces large base64 strings. Ensure Express is configured to accept JSON payloads up to `10mb` (`app.use(express.json({ limit: '10mb' }))`) so the base64 payload isn't rejected.
3.  **Path Traversal Prevention:** The backend `server.ts` code must strictly validate that the destination path resolves to somewhere inside the `repo-php` directory.
