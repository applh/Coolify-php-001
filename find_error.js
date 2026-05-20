import fs from "fs";
import path from "path";

function searchDir(dir, term) {
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            searchDir(fullPath, term);
        } else if (fullPath.endsWith(".js") || fullPath.endsWith(".ts") || fullPath.endsWith(".mjs") || fullPath.endsWith(".cjs")) {
            const content = fs.readFileSync(fullPath, "utf-8");
            if (content.includes(term)) {
                console.log("Found in", fullPath);
            }
        }
    }
}
searchDir("node_modules/@google", "deserialize");
