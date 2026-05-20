import { GoogleGenAI } from "@google/genai";

async function run() {
    const ai = new GoogleGenAI({});
    try {
        const response = await ai.models.generateContent({
            model: 'gemini-2.5-flash-banana',
            contents: { parts: [{ text: "A robot holding a red skateboard." }] },
        });
        console.log(response);
    } catch (e) {
        console.error(e.message);
    }
}
run();
