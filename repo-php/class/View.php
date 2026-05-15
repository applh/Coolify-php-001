<?php

class View {
    /**
     * Render a view file
     * 
     * @param string $path Path to the view file relative to the views directory
     * @param array $data Data to be extracted and made available in the view
     */
    public static function render($path, $data = []) {
        echo self::renderToString($path, $data);
    }

    /**
     * Render a view file and return the result as a string
     */
    public static function renderToString($path, $data = []) {
        // Extract variables to the scope of the view
        extract($data);
        
        $rootPath = realpath(__DIR__ . '/..');
        $fullPath = $rootPath . '/views/' . $path . '.php';
        
        if (file_exists($fullPath)) {
            ob_start();
            require $fullPath;
            return ob_get_clean();
        } else {
            return "View not found: " . htmlspecialchars($path);
        }
    }
}
