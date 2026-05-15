<?php

class Router {
    /**
     * Dispatch the request statically
     */
    public static function dispatch($contentPath) {
        // 1. Resolve Site
        $activeSite = getenv('ACTIVE_SITE_OVERRIDE');
        $httpHostRaw = $_SERVER['HTTP_HOST'] ?? '';
        $httpHost = explode(':', $httpHostRaw)[0]; // Remove port
        
        // Allow query param override for development
        if (isset($_GET['__site'])) {
            $activeSite = $_GET['__site'];
        }

        if (!$activeSite && $httpHost) {
            $configFile = $contentPath . '/config.php';
            if (file_exists($configFile)) {
                $domainMap = require $configFile;
                // Try exact match first
                if (is_array($domainMap) && isset($domainMap[$httpHost])) {
                    $activeSite = $domainMap[$httpHost];
                } else {
                    // Try case-insensitive folder match from domain map
                    foreach ($domainMap as $domain => $folder) {
                        if (strcasecmp($domain, $httpHost) === 0) {
                            $activeSite = $folder;
                            break;
                        }
                    }
                }
            }
            
            // If still not resolved, check if host matches any site name exactly or as a prefix/suffix
            if (!$activeSite) {
                $availableSites = CMS::getSites($contentPath);
                
                // Try exact case-insensitive match first for all
                foreach ($availableSites as $site) {
                    if (strcasecmp($httpHost, $site) === 0) {
                        $activeSite = $site;
                        break;
                    }
                }
                
                // If still not resolved, try substring match (longer names first to avoid "site1" matching "longsite1.com")
                if (!$activeSite) {
                    usort($availableSites, function($a, $b) {
                        return strlen($b) <=> strlen($a);
                    });
                    foreach ($availableSites as $site) {
                        if (stripos($httpHost, $site) !== false) {
                            $activeSite = $site;
                            break;
                        }
                    }
                }
            }
        }

        if (!$activeSite) {
            $activeSite = $httpHost ?: 'site1.com';
        }

        // Final attempt to match existing folders case-insensitively if not found directly
        $sanitized = preg_replace('/[^a-zA-Z0-9.-]/', '', $activeSite);
        if (!is_dir($contentPath . '/' . $sanitized)) {
             $availableSites = CMS::getSites($contentPath);
             foreach ($availableSites as $site) {
                 if (strcasecmp($sanitized, $site) === 0) {
                     $activeSite = $site;
                     break;
                 }
             }
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
                // Check if individual zip version exists
                $zipFile = $siteDir . urldecode($requestUri) . '.zip';
                
                if (file_exists($zipFile)) {
                    $zip = new ZipArchive();
                    if ($zip->open($zipFile) === TRUE) {
                        // The file inside the zip is expected to have the same basename
                        $entryName = basename(urldecode($requestUri));
                        $binary = $zip->getFromName($entryName);
                        
                        if ($binary !== false) {
                            // Attempt to restore file for future requests
                            $mediaPathToCreate = $siteDir . urldecode($requestUri);
                            $dirToCreate = dirname($mediaPathToCreate);
                            if (!is_dir($dirToCreate)) {
                                @mkdir($dirToCreate, 0777, true);
                            }
                            @file_put_contents($mediaPathToCreate, $binary);
                            
                            $mimeType = self::getMimeType($extension);
                            header("Content-Type: $mimeType");
                            echo $binary;
                            $zip->close();
                            exit;
                        }
                        $zip->close();
                    }
                }
                
                if (in_array($extension, ['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp'])) {
                    self::serveDynamicPlaceholder($requestUri, $activeSite);
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
            // Initialize Plugins for this site
            PluginManager::init($siteDir);

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

    private static function serveDynamicPlaceholder($requestUri, $activeSite) {
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
        (Task added to AI generation queue)
    </text>
</svg>';
        echo $svg;
    }
}
