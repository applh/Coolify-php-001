import { GoogleGenAI } from "@google/genai";

async function run() {
    const ai = new GoogleGenAI({});
    try {
        const response = await ai.models.generateImages({
            model: 'imagen-3.0-generate-002',
            prompt: 'A robot holding a red skateboard.',
            config: {
                aspectRatio: "1:1"
            }
        });
        console.log("Success with imagen-3.0-generate-002", response);
    } catch (e) {
        console.error(e);
        if (e.status) console.error("Status:", e.status);
    }
}
run();
