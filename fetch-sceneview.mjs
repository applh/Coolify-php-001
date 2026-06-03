import fs from 'fs';
import { execSync } from 'child_process';

async function run() {
  console.log("Fetching sources jar...");
  const res = await fetch("https://repo1.maven.org/maven2/io/github/sceneview/sceneview/4.15.0/sceneview-4.15.0-sources.jar");
  const buffer = await res.arrayBuffer();
  fs.writeFileSync("/sceneview-sources.jar", Buffer.from(buffer));
  
  console.log("Unzipping...");
  try {
    execSync("mkdir -p /sceneview-sources && cd /sceneview-sources && unzip -q ../sceneview-sources.jar", {stdio: 'inherit'});
    const out = execSync("grep -A 20 -rI 'fun SceneView(' /sceneview-sources", {encoding: 'utf-8'});
    console.log("Result:\n" + out);
  } catch(e) {
    console.log(e);
  }
}
run();
