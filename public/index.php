<?php
/**
 * CMS Multi-site Entry Point
 */

// Initialize CMS components
$currentDir = __DIR__;
// Check if we are in public/ or root
$rootPath = file_exists($currentDir . '/src') ? $currentDir : dirname($currentDir);
// Fallback if dirname doesn't reveal src (e.g. nested deeper or root is wrong)
if (!file_exists($rootPath . '/src')) {
    $rootPath = realpath($currentDir . '/..') ?: $currentDir;
}

// Manual fallback for classes if composer isn't run
if (!file_exists($rootPath . '/vendor/autoload.php')) {
    spl_autoload_register(function ($class) use ($rootPath) {
        $prefix = 'CMS\\';
        $base_dir = $rootPath . '/src/';
        $len = strlen($prefix);
        if (strncmp($prefix, $class, $len) !== 0) return;
        $relative_class = substr($class, $len);
        $file = $base_dir . str_replace('\\', '/', $relative_class) . '.php';
        if (file_exists($file)) require_once $file;
    });
} else {
    require_once $rootPath . '/vendor/autoload.php';
}

use CMS\SiteManager;
use CMS\Router;

$contentPath = $rootPath . '/content';
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
