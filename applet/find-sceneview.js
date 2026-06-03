const fs = require('fs');
const cp = require('child_process');

console.log('Searching for SceneView cache...');
try {
  const result = cp.execSync('find / -name "*sceneview*.aar" 2>/dev/null | grep 4.15.0 || true', { encoding: 'utf-8' });
  console.log('Results:\n', result);
} catch(e) {
  console.log(e);
}
