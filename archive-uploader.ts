import * as fs from 'fs';
import * as path from 'path';
import AdmZip from 'adm-zip';
import dotenv from 'dotenv';

// Load local .env if available (for local testing/dev servers)
dotenv.config();

/**
 * Configuration Options from Environment Variables
 */
const SHIPPING_API_URL = process.env.SHIPPING_API_URL;
const SHIPPING_API_TOKEN = process.env.SHIPPING_API_TOKEN;
const ZIP_OUTPUT_NAME = process.env.ZIP_OUTPUT_NAME || 'project-archive.zip';

// Default directories and files to exclude from the zip archive
const DEFAULT_EXCLUDES = new Set([
  'node_modules',
  '.git',
  '.github',
  'dist',
  'build',
  '.env',
  ZIP_OUTPUT_NAME, // Avoid self-referencing zip loop
  '.DS_Store'
]);

/**
 * Helper to recursively search for files in the workspace while respecting exclusions
 */
function getFilesRecursively(dir: string, baseDir: string, excludes: Set<string>): string[] {
  let results: string[] = [];
  const list = fs.readdirSync(dir);

  for (const file of list) {
    const filePath = path.join(dir, file);
    const relativePath = path.relative(baseDir, filePath);
    const stats = fs.statSync(filePath);

    // Skip excluded directories or files
    if (excludes.has(file) || excludes.has(relativePath)) {
      continue;
    }

    if (stats.isDirectory()) {
      results = results.concat(getFilesRecursively(filePath, baseDir, excludes));
    } else if (stats.isFile()) {
      results.push(filePath);
    }
  }

  return results;
}

/**
 * Main Executable function
 */
async function runArchiveAndUpload() {
  console.log('\n======================================================');
  console.log('🍓 FRAISE - SOVEREIGN ARCHIVER & SHIPPER TOOLCHAIN');
  console.log('======================================================\n');

  if (!SHIPPING_API_URL) {
    console.error('❌ Error: SHIPPING_API_URL environment variable is not defined.');
    console.error('Please define SHIPPING_API_URL in AI Studio Environment Settings.');
    process.exit(1);
  }

  const workspaceRoot = process.cwd();
  console.log(`📂 Scanning workspace at: ${workspaceRoot}`);

  try {
    const zip = new AdmZip();
    const files = getFilesRecursively(workspaceRoot, workspaceRoot, DEFAULT_EXCLUDES);

    console.log(`📦 Bundling ${files.length} project files...`);

    // Add files to the ZIP archive
    for (const file of files) {
      const relativePath = path.relative(workspaceRoot, file);
      const fileDir = path.dirname(relativePath);
      
      // Target directory inside the zip file
      const zipPath = fileDir === '.' ? '' : fileDir;
      zip.addLocalFile(file, zipPath);
    }

    // Write zip temporarily to local disk
    console.log(`💾 Writing zip archive to: ${ZIP_OUTPUT_NAME}`);
    zip.writeZip(ZIP_OUTPUT_NAME);

    const zipStats = fs.statSync(ZIP_OUTPUT_NAME);
    const sizeInMB = (zipStats.size / (1024 * 1024)).toFixed(2);
    console.log(`✅ Zip archive successfully created! Size: ${sizeInMB} MB`);

    // Prepare upload request
    console.log(`🚀 Contacting Shipping Destination: ${SHIPPING_API_URL}`);

    // Read ZIP buffer
    const zipBuffer = fs.readFileSync(ZIP_OUTPUT_NAME);

    const headers: Record<string, string> = {};
    if (SHIPPING_API_TOKEN) {
      console.log('🔒 Appending authorization token token to header...');
      headers['Authorization'] = SHIPPING_API_TOKEN.startsWith('Bearer ') 
        ? SHIPPING_API_TOKEN 
        : `Bearer ${SHIPPING_API_TOKEN}`;
    }

    // We can support both Raw Binary stream upload and Multipart Form data.
    // By default, we will send a direct binary upload with Content-Type application/zip.
    // Users can adjust this depending on the target gateway requirements.
    headers['Content-Type'] = 'application/zip';
    headers['Content-Disposition'] = `attachment; filename="${ZIP_OUTPUT_NAME}"`;

    console.log('📤 Uploading archive to external API...');
    const response = await fetch(SHIPPING_API_URL, {
      method: 'POST',
      body: zipBuffer,
      headers: headers
    });

    console.log(`📡 Gateway responded with Status: ${response.status} ${response.statusText}`);

    if (response.ok) {
      const responseText = await response.text();
      console.log('\n🎉 SUCCESS: Project successfully transmitted & shipped!');
      console.log('======================================================');
      console.log('Gate Way Response:\n', responseText);
      console.log('======================================================\n');
    } else {
      const errorText = await response.text();
      console.error(`\n❌ UPLOAD FAILED (HTTP ${response.status}):`);
      console.error(errorText);
      console.error('======================================================\n');
      process.exit(1);
    }

  } catch (error) {
    console.error('❌ Unexpected Operational Failure:');
    console.error(error);
    process.exit(1);
  } finally {
    // Cleanup the generated zip file locally to keep the container workspace clean
    try {
      if (fs.existsSync(ZIP_OUTPUT_NAME)) {
        console.log(`🧼 Cleaning up temporary zip file: ${ZIP_OUTPUT_NAME}`);
        fs.unlinkSync(ZIP_OUTPUT_NAME);
      }
    } catch (cleanupError) {
      console.error('⚠️ Note: Could not clean up temporary zip file:', cleanupError);
    }
  }
}

// Execute
runArchiveAndUpload();
