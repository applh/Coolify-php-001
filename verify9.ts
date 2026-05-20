import { GoogleGenAI } from "@google/genai";

async function run() {
    const ai = new GoogleGenAI({});
    try {
        const response = await ai.models.generateContent({
            model: 'gemini-2.5-flash',
            contents: { parts: [{ text: "test" }] },
        });
        console.log(response.text);
    } catch (e) {
        console.error(e.message);
    }
}
run();
