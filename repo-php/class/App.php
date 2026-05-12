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
        
        $requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
        if ($requestUri === '/api/heartbeat') {
            self::handleHeartbeat();
            exit;
        }

        if (strpos($requestUri, '/admin') === 0) {
            AdminRouter::dispatch(self::$contentPath);
            exit;
        }

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

    private static function handleHeartbeat() {
        header('Content-Type: application/json');
        
        $aiAgent = new AIAgent();
        if (!$aiAgent->isAvailable()) {
            http_response_code(503);
            echo json_encode(['status' => 'error', 'message' => 'AI Agent not available (No API Key)']);
            return;
        }

        $task = AITaskManager::getNextPendingTask(self::$contentPath);
        if (!$task) {
            echo json_encode(['status' => 'success', 'message' => 'No pending tasks']);
            return;
        }

        // Mark as running
        AITaskManager::updateTask(self::$contentPath, $task['id'], ['status' => 'running']);

        try {
            $result = $aiAgent->executeTask($task);
            AITaskManager::updateTask(self::$contentPath, $task['id'], [
                'status' => 'completed',
                'result' => $result
            ]);
            echo json_encode([
                'status' => 'success',
                'task_id' => $task['id'],
                'type' => $task['type'],
                'message' => 'Task completed successfully'
            ]);
        } catch (Exception $e) {
            AITaskManager::updateTask(self::$contentPath, $task['id'], [
                'status' => 'failed',
                'error' => $e->getMessage()
            ]);
            http_response_code(500);
            echo json_encode([
                'status' => 'error',
                'task_id' => $task['id'],
                'message' => $e->getMessage()
            ]);
        }
    }
}
