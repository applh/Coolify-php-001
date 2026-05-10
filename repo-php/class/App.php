<?php

class App {
    private static $rootPath;
    private static $contentPath;

    /**
     * Main entry point for the application
     */
    public static function bootstrap() {
        self::initErrors();
        self::initPaths();
        self::initAutoloader();
        self::handleDebug();
        
        // Dispatch the request via the Front Controller / Router
        Router::dispatch(self::$contentPath);
    }

    private static function initErrors() {
        ini_set('display_errors', 1);
        ini_set('display_startup_errors', 1);
        error_reporting(E_ALL);
    }

    private static function initPaths() {
        self::$rootPath = realpath(__DIR__ . '/..');
        
        self::$contentPath = self::$rootPath . '/my-data';
        if (!is_dir(self::$contentPath)) {
            self::$contentPath = self::$rootPath . '/content';
        }
    }

    private static function initAutoloader() {
        spl_autoload_register(function ($class_name) {
            $file = self::$rootPath . '/class/' . str_replace('\\', '/', $class_name) . '.php';
            if (file_exists($file)) {
                require_once $file;
            }
        });
    }

    private static function handleDebug() {
        if (isset($_GET['cms_debug'])) {
            header('Content-Type: application/json');
            echo json_encode(CMS::validateSetup(self::$contentPath));
            exit;
        }
    }
}
