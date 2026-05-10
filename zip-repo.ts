import AdmZip from 'adm-zip';
import path from 'path';
import fs from 'fs';

async function createZip() {
    const zip = new AdmZip();
    const repoPath = path.join(process.cwd(), 'repo-php');
    const zipPath = path.join(process.cwd(), 'repo-php.zip');

    console.log(`Zipping ${repoPath}...`);
    
    // Add the local folder repo-php to the zip
    zip.addLocalFolder(repoPath);
    
    // Write the zip file
    zip.writeZip(zipPath);
    
    console.log(`Successfully created ${zipPath}`);
}

createZip().catch(console.error);
