<?php
/**
 * CMS Multi-site Entry Point
 */

// Attempt to load composer autoloader if it exists
if (file_exists(__DIR__ . '/../vendor/autoload.php')) {
    require_once __DIR__ . '/../vendor/autoload.php';
} else {
    // Manual fallback for classes if composer isn't run
    spl_autoload_register(function ($class) {
        $prefix = 'CMS\\';
        $base_dir = __DIR__ . '/../src/';
        $len = strlen($prefix);
        if (strncmp($prefix, $class, $len) !== 0) return;
        $relative_class = substr($class, $len);
        $file = $base_dir . str_replace('\\', '/', $relative_class) . '.php';
        if (file_exists($file)) require $file;
    });
}

use CMS\SiteManager;
use CMS\Router;

// Initialize CMS components
$contentPath = __DIR__ . '/../content';
$siteManager = new SiteManager($contentPath);

// Check if site exists
if (!$siteManager->siteExists()) {
    // Default fallback site if none matches
    putenv("ACTIVE_SITE_OVERRIDE=site1.com");
    $siteManager = new SiteManager($contentPath);
}

$router = new Router($siteManager->getSitePath());
$markdownFile = $router->resolvePage();

if (!$markdownFile) {
    http_response_code(404);
    die("Page not found");
}

// Parse Content
$rawContent = file_get_contents($markdownFile);
$patterns = [
    '/^---\s*\n(.*?)\n---\s*\n(.*)$/s', // Standard format
    '/^---\s*\r\n(.*?)\r\n---\s*\r\n(.*)$/s' // Windows format
];

$frontMatter = [];
$content = $rawContent;

foreach ($patterns as $pattern) {
    if (preg_match($pattern, $rawContent, $matches)) {
        $yaml = $matches[1];
        $content = $matches[2];
        
        // Simple YAML parser
        $lines = explode("\n", $yaml);
        foreach ($lines as $line) {
            $parts = explode(":", $line, 2);
            if (count($parts) === 2) {
                $frontMatter[trim($parts[0])] = trim($parts[1]);
            }
        }
        break;
    }
}

// Markdown to HTML
if (class_exists('Parsedown')) {
    $parsedown = new Parsedown();
    $htmlContent = $parsedown->text($content);
} else {
    // Basic fallback parser for preview if Parsedown is missing
    $htmlContent = nl2br(htmlspecialchars($content));
    $htmlContent = preg_replace('/^# (.*)$/m', '<h1>$1</h1>', $htmlContent);
    $htmlContent = preg_replace('/^## (.*)$/m', '<h2>$1</h2>', $htmlContent);
    $htmlContent = preg_replace('/\*\*(.*)\*\*/U', '<strong>$1</strong>', $htmlContent);
}

// Site Details
$siteName = $siteManager->getConfig('title', $siteManager->getActiveSite());
$siteDescription = $siteManager->getConfig('description', 'A flat-file powered website.');
$title = $frontMatter['title'] ?? $siteName;

// Theme Layout
$theme = $siteManager->getConfig('theme', 'default');
$layoutFile = __DIR__ . "/../themes/{$theme}/layout.php";

if (file_exists($layoutFile)) {
    $content = $htmlContent; // Map to layout variable
    include $layoutFile;
} else {
    echo $htmlContent;
}
