import { GoogleGenAI } from "@google/genai";

async function run() {
    const ai = new GoogleGenAI({});
    try {
        const response = await ai.models.generateContent({
            model: 'gemini-2.5-flash-image',
            contents: { parts: [{ text: "testing" }] },
            config: {
                imageConfig: {
                    aspectRatio: "1:1"
                }
            }
        });
        console.log(response);
    } catch (e) {
        console.error("Name:", e.name);
        console.error("Message:", e.message);
    }
}
run();
