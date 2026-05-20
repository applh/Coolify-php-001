import fs from "fs";
import path from "path";

function searchDir(dir, term) {
    let files;
    try {
        files = fs.readdirSync(dir);
    } catch { return; }
    for (const file of files) {
        if (file === "node_modules") continue; // wait, I want to search node_modules!
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            searchDir(fullPath, term);
        } else if (fullPath.endsWith(".js") || fullPath.endsWith(".ts") || fullPath.endsWith(".mjs") || fullPath.endsWith(".cjs")) {
            const content = fs.readFileSync(fullPath, "utf-8");
            if (content.toLowerCase().includes(term.toLowerCase())) {
                console.log("Found in", fullPath);
            }
        }
    }
}
searchDir("node_modules", "deserialize a response");
