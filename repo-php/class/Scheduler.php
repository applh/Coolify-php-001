<?php

class Scheduler {
    /**
     * Parse a basic cron expression and check if it matches the current time.
     * Note: This is a basic implementation for '*' and specific numbers.
     */
    private static function isCronMathing($cronExpression, $time = null) {
        if ($time === null) {
            $time = time();
        }
        $cronParts = explode(' ', $cronExpression);
        if (count($cronParts) !== 5) {
            return false;
        }

        list($min, $hour, $day, $month, $dow) = $cronParts;
        $current = explode(' ', date('i G j n w', $time));
        
        return self::matchPart($min, $current[0]) &&
               self::matchPart($hour, $current[1]) &&
               self::matchPart($day, $current[2]) &&
               self::matchPart($month, $current[3]) &&
               self::matchPart($dow, $current[4]);
    }

    private static function matchPart($cronPart, $currentPart) {
        if ($cronPart === '*') {
            return true;
        }
        return $cronPart === $currentPart;
    }

    public static function runDueTasks($contentPath) {
        $tasks = AITaskManager::getTasks($contentPath);
        $dueTasks = [];

        foreach ($tasks as $task) {
            if (!empty($task['is_recurring']) && !empty($task['cron_expression'])) {
                // If the task is pending, it hasn't been processed yet, just skip to avoid duplicates
                if ($task['status'] === 'pending') {
                    continue;
                }
                
                // Allow a task to run once per minute
                $lastRunTime = $task['last_run_at'] ? strtotime($task['last_run_at']) : 0;
                $now = time();
                
                if (($now - $lastRunTime) >= 60 && self::isCronMathing($task['cron_expression'], $now)) {
                    // Task is due, create a new pending task
                    $dueTasks[] = $task;
                }
            }
        }

        foreach ($dueTasks as $task) {
            AITaskManager::updateTask($contentPath, $task['id'], [
                'last_run_at' => date('Y-m-d H:i:s')
            ]);
            
            // Re-add as pending to execute
            AITaskManager::addTask($contentPath, $task['site'], $task['type'], $task['payload']);
        }
        
        return count($dueTasks);
    }
}
