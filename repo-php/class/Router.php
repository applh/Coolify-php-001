<?php

class Router {
    /**
     * Dispatch the request statically
     */
    public static function dispatch($contentPath) {
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

        // Intercept Media Files
        $requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
        $extension = strtolower(pathinfo($requestUri, PATHINFO_EXTENSION));
        $mediaExtensions = ['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp', 'mp4', 'webm', 'mp3', 'wav'];

        if (in_array($extension, $mediaExtensions)) {
            $mediaPath = realpath($siteDir . urldecode($requestUri));
            $siteDirReal = realpath($siteDir);

            if ($mediaPath && $siteDirReal && strpos($mediaPath, $siteDirReal) === 0 && file_exists($mediaPath)) {
                $mimeType = self::getMimeType($extension);
                header("Content-Type: $mimeType");
                readfile($mediaPath);
                exit;
            } else {
                // Check if base64 version exists
                $relativeUri = ltrim(urldecode($requestUri), '/');
                $b64File = $siteDir . '/b64/' . base64_encode($relativeUri) . '.txt';
                
                if (file_exists($b64File)) {
                    $base64Str = file_get_contents($b64File);
                    $binary = base64_decode($base64Str);
                    
                    // Attempt to restore file for future requests
                    $mediaPathToCreate = $siteDir . '/' . $relativeUri;
                    $dirToCreate = dirname($mediaPathToCreate);
                    if (!is_dir($dirToCreate)) {
                        mkdir($dirToCreate, 0777, true);
                    }
                    file_put_contents($mediaPathToCreate, $binary);
                    
                    $mimeType = self::getMimeType($extension);
                    header("Content-Type: $mimeType");
                    echo $binary;
                    exit;
                } elseif (in_array($extension, ['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp'])) {
                    self::serveDynamicPlaceholder($requestUri);
                    exit;
                } else {
                    http_response_code(404);
                    echo "Media not found";
                    exit;
                }
            }
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
    }

    private static function getMimeType($extension) {
        $mimeTypes = [
            'jpg' => 'image/jpeg',
            'jpeg' => 'image/jpeg',
            'png' => 'image/png',
            'gif' => 'image/gif',
            'svg' => 'image/svg+xml',
            'webp' => 'image/webp',
            'mp4' => 'video/mp4',
            'webm' => 'video/webm',
            'mp3' => 'audio/mpeg',
            'wav' => 'audio/wav',
        ];
        return $mimeTypes[$extension] ?? 'application/octet-stream';
    }

    private static function serveDynamicPlaceholder($requestUri) {
        header("Content-Type: image/svg+xml");
        $filename = basename($requestUri);
        $text = htmlspecialchars($filename);
        
        $svg = '<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">
    <rect width="100%" height="100%" fill="#eee"/>
    <text x="50%" y="50%" font-family="sans-serif" font-size="24" fill="#aaa" text-anchor="middle" dominant-baseline="middle">
        Missing Media: ' . $text . '
    </text>
    <text x="50%" y="58%" font-family="sans-serif" font-size="16" fill="#ccc" text-anchor="middle" dominant-baseline="middle">
        (Queue to generate via AI)
    </text>
</svg>';
        echo $svg;
    }
}
