import fs from "fs";
import path from "path";

function searchDir(dir, term) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            searchDir(fullPath, term);
        } else if (fullPath.endsWith(".js") || fullPath.endsWith(".mjs")) {
            const content = fs.readFileSync(fullPath, "utf-8");
            if (content.includes("Something went wrong")) {
                console.log("Found Something went wrong in", fullPath);
                const idx = content.indexOf("Something went wrong");
                console.log(content.substring(Math.max(0, idx - 50), idx + 100));
            }
        }
    }
}
searchDir("node_modules/@google/genai", "Something went wrong");
