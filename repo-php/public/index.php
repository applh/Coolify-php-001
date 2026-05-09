<?php
/**
 * Simplified Multi-site Router
 */
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

$rootPath = realpath(__DIR__ . '/..');
$contentPath = $rootPath . '/content';

// 1. Resolve Site
$activeSite = getenv('ACTIVE_SITE_OVERRIDE');
$httpHost = $_SERVER['HTTP_HOST'] ?? 'site1.com';

if (!$activeSite) {
    $configFile = $contentPath . '/config.php';
    if (file_exists($configFile)) {
        $domainMap = require $configFile;
        if (is_array($domainMap) && isset($domainMap[$httpHost])) {
            $activeSite = $domainMap[$httpHost];
        }
    }
}

if (!$activeSite) {
    $activeSite = $httpHost;
}

// Sanitize the site name (allow alphanumeric, dots, and dashes)
$activeSite = preg_replace('/[^a-zA-Z0-9.-]/', '', $activeSite);

$siteDir = $contentPath . '/' . $activeSite;

// 2. Fallback to site1.com if resolved site doesn't exist
if (!is_dir($siteDir)) {
    $activeSite = 'site1.com';
    $siteDir = $contentPath . '/' . $activeSite;
}

// 3. Load the site's index.php
$siteIndex = $siteDir . '/index.php';

if (file_exists($siteIndex)) {
    // Change directory to the site folder to support relative includes within site templates
    chdir($siteDir);
    require_once 'index.php';
} else {
    http_response_code(404);
    echo "<h1>404 - Site Not Configured</h1>";
    echo "<p>Could not find index.php for site: " . htmlspecialchars($activeSite) . "</p>";
}
