<?php
/**
 * Simplified Multi-site Router
 */

$rootPath = dirname(__DIR__);
$contentPath = $rootPath . '/content';

// 1. Resolve Site
$activeSite = getenv('ACTIVE_SITE_OVERRIDE');
if (!$activeSite) {
    $activeSite = $_SERVER['HTTP_HOST'] ?? 'site1.com';
}

$siteDir = $contentPath . '/' . $activeSite;

// 2. Fallback to site1.com if resolved site doesn't exist
if (!is_dir($siteDir)) {
    $activeSite = 'site1.com';
    $siteDir = $contentPath . '/' . $activeSite;
}

// 3. Load the site's index.php
$siteIndex = $siteDir . '/index.php';

if (file_exists($siteIndex)) {
    require_once $siteIndex;
} else {
    http_response_code(404);
    echo "<h1>404 - Site Not Configured</h1>";
    echo "<p>Could not find index.php for site: " . htmlspecialchars($activeSite) . "</p>";
}
