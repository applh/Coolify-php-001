async function run() {
    const apiKey = process.env.GEMINI_API_KEY;
    const url = `https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-001:predict?key=${apiKey}`;
    const payload = {"instances": [{"prompt": "A robot holding a red skateboard"}], "parameters": {"sampleCount": 1}};
    try {
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const text = await res.text();
        console.log("Status:", res.status);
        console.log("Response:", text);
    } catch(e) { console.error(e); }
}
run();
