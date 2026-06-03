const { execSync } = require('child_process');
try {
  const result = execSync('sh ./gradlew assembleDebug', { cwd: 'repo-android', encoding: 'utf-8' });
  console.log('SUCCESS:');
  console.log(result.split('\n').slice(-20).join('\n'));
} catch (e) {
  console.log('ERROR:');
  console.log(e.stdout.split('\n').slice(-50).join('\n'));
  console.log(e.stderr);
}
