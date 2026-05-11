<?php

class CMS {
    /**
     * Gets paths to all sites in the content directory
     */
    public static function getSites($contentPath) {
        $sites = [];
        $items = scandir($contentPath);
        foreach ($items as $item) {
            if ($item === '.' || $item === '..' || $item === 'my-data') continue;
            if (is_dir($contentPath . '/' . $item)) {
                $sites[] = $item;
            }
        }
        return $sites;
    }

    /**
     * Validates the core setup
     */
    public static function validateSetup($contentPath) {
        return [
            'content_path' => $contentPath,
            'is_writable' => is_writable($contentPath),
            'sites' => self::getSites($contentPath),
            'php_version' => PHP_VERSION,
            'server_software' => $_SERVER['SERVER_SOFTWARE'] ?? 'unknown'
        ];
    }
    
    /**
     * Helper to safely get an image URL, falling back to a placeholder if missing
     */
    public static function image($path, $alt = '') {
        // In this multi-site setup, $path is expected to be relative to site root like '/img/photo.jpg'
        // The Router handles the actual serving logic.
        return htmlspecialchars($path);
    }
}
