import { GoogleGenAI } from "@google/genai";

async function run() {
    const ai = new GoogleGenAI({});
    try {
        const response = await ai.models.generateContent({
            model: 'gemini-2.5-flash-image',
            contents: { parts: [{ text: "A robot holding a red skateboard." }] },
            config: {
                imageConfig: {
                    aspectRatio: "1:1"
                }
            }
        });
        console.log(response);
    } catch (e) {
        console.error(e);
        if (e.status) console.error("Status:", e.status);
    }
}
run();
