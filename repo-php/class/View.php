<?php

class View {
    /**
     * Render a view file
     * 
     * @param string $path Path to the view file relative to the views directory
     * @param array $data Data to be extracted and made available in the view
     */
    public static function render($path, $data = []) {
        // Extract variables to the scope of the view
        extract($data);
        
        $rootPath = realpath(__DIR__ . '/..');
        // Views can be stored in a dedicated directory like /repo-php/views
        $fullPath = $rootPath . '/views/' . $path . '.php';
        
        if (file_exists($fullPath)) {
            require $fullPath;
        } else {
            echo "View not found: " . htmlspecialchars($path);
        }
    }
}
