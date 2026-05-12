<?php

class AITaskManager {
    private static $tasksFile;

    private static function init($contentPath) {
        self::$tasksFile = $contentPath . '/ai_tasks.json';
        if (!file_exists(self::$tasksFile)) {
            file_put_contents(self::$tasksFile, json_encode(['tasks' => []]));
        }
    }

    public static function getTasks($contentPath) {
        self::init($contentPath);
        $data = json_decode(file_get_contents(self::$tasksFile), true);
        return $data['tasks'] ?? [];
    }

    public static function addTask($contentPath, $site, $type, $payload) {
        self::init($contentPath);
        $tasks = self::getTasks($contentPath);
        
        $newTask = [
            'id' => uniqid('task_'),
            'site' => $site,
            'type' => $type,
            'status' => 'pending',
            'payload' => $payload,
            'result' => null,
            'error' => null,
            'created_at' => date('Y-m-d H:i:s'),
            'updated_at' => date('Y-m-d H:i:s')
        ];
        
        $tasks[] = $newTask;
        self::saveTasks($tasks);
        return $newTask;
    }

    public static function getNextPendingTask($contentPath) {
        $tasks = self::getTasks($contentPath);
        foreach ($tasks as $task) {
            if ($task['status'] === 'pending') {
                return $task;
            }
        }
        return null;
    }

    public static function updateTask($contentPath, $taskId, $updates) {
        $tasks = self::getTasks($contentPath);
        $updated = false;
        foreach ($tasks as &$task) {
            if ($task['id'] === $taskId) {
                foreach ($updates as $key => $value) {
                    $task[$key] = $value;
                }
                $task['updated_at'] = date('Y-m-d H:i:s');
                $updated = true;
                break;
            }
        }
        if ($updated) {
            self::saveTasks($tasks);
        }
        return $updated;
    }

    private static function saveTasks($tasks) {
        file_put_contents(self::$tasksFile, json_encode(['tasks' => $tasks], JSON_PRETTY_PRINT));
    }
}
