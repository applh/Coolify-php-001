import * as fs from 'fs';
import * as path from 'path';
import AdmZip from 'adm-zip';

// Global configurations
const EXCLUDE_PATTERNS = new Set([
  'node_modules',
  '.git',
  '.github',
  'dist',
  'build',
  'out',
  '.gradle',
  '.idea',
  '.venv',
  'venv',
  '__pycache__',
  '.DS_Store',
  'archive.zip', // Prevent recursive zipping of itself
]);

/**
 * Recursively scans files in a directory while excluding build/heavy directories
 */
function scanFiles(dir: string, baseDir: string, excludes: Set<string>): string[] {
  let fileList: string[] = [];
  
  if (!fs.existsSync(dir)) return fileList;
  const entries = fs.readdirSync(dir);

  for (const entry of entries) {
    const fullPath = path.join(dir, entry);
    const relativePath = path.relative(baseDir, fullPath);
    
    // Check if the current file/folder should be excluded
    if (excludes.has(entry) || excludes.has(relativePath)) {
      continue;
    }

    const stats = fs.statSync(fullPath);
    if (stats.isDirectory()) {
      fileList = fileList.concat(scanFiles(fullPath, baseDir, excludes));
    } else if (stats.isFile()) {
      fileList.push(fullPath);
    }
  }

  return fileList;
}

/**
 * Packages a single repo folder into archive.zip
 */
function archiveRepo(repoDir: string, repoName: string) {
  const archivePath = path.join(repoDir, 'archive.zip');
  
  // If an old archive exists, delete it first to ensure a completely fresh start
  if (fs.existsSync(archivePath)) {
    try {
      fs.unlinkSync(archivePath);
    } catch (err) {
      console.error(`⚠️ Warning: Could not delete existing archive at ${archivePath}:`, err);
    }
  }

  console.log(`📦 Packaging [${repoName}]...`);
  const zip = new AdmZip();
  const files = scanFiles(repoDir, repoDir, EXCLUDE_PATTERNS);

  if (files.length === 0) {
    console.log(`⚠️ No source files found to package for [${repoName}]. Skipping.`);
    return;
  }

  // Add each file to its corresponding relative path in the zip
  for (const file of files) {
    const relativePath = path.relative(repoDir, file);
    const fileEntryDir = path.dirname(relativePath);
    const targetPathInZip = fileEntryDir === '.' ? '' : fileEntryDir;
    
    zip.addLocalFile(file, targetPathInZip);
  }

  // Save the ZIP archive
  zip.writeZip(archivePath);
  
  const stats = fs.statSync(archivePath);
  const sizeInMB = (stats.size / (1024 * 1024)).toFixed(3);
  console.log(`✅ Compressed ${files.length} files. Saved to: ${archivePath} (${sizeInMB} MB)\n`);
}

/**
 * Main routine
 */
function main() {
  console.log('\n======================================================');
  console.log('🍓 SOVEREIGN REPO COMPACTOR AND PACKAGING UTILITY');
  console.log('======================================================\n');

  const rootDir = process.cwd();
  const entries = fs.readdirSync(rootDir, { withFileTypes: true });

  const repoNames = entries
    .filter(dirent => dirent.isDirectory() && dirent.name.startsWith('repo-'))
    .map(dirent => dirent.name);

  if (repoNames.length === 0) {
    console.log('❌ No "repo-*" directories found in the root partition.');
    process.exit(1);
  }

  console.log(`🔍 Detected ${repoNames.length} modular repo partitions:`);
  repoNames.forEach(name => console.log(`  • ${name}/`));
  console.log('');

  for (const name of repoNames) {
    const repoPath = path.join(rootDir, name);
    try {
      archiveRepo(repoPath, name);
    } catch (err) {
      console.error(`❌ Critical failure archiving repo ${name}:`, err);
    }
  }

  console.log('======================================================');
  console.log('🎉 Packaging operation finished successfully.');
  console.log('Use unzipping routines in target Docker builds now.');
  console.log('======================================================\n');
}

main();
