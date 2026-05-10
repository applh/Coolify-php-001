<?php

class CMS {
    public static function validateSetup($contentPath) {
        $results = [
            'status' => 'success',
            'checks' => []
        ];

        // Check PHP Version (require >= 7.4)
        $phpVersion = phpversion();
        $results['checks']['php_version'] = [
            'status' => version_compare($phpVersion, '7.4.0', '>=') ? 'pass' : 'fail',
            'message' => "PHP Version: " . $phpVersion
        ];

        // Check if content path exists and is readable
        $results['checks']['content_dir'] = [
            'status' => is_dir($contentPath) && is_readable($contentPath) ? 'pass' : 'fail',
            'message' => "Content Directory ($contentPath) readable"
        ];

        // Check if config.php exists in content path (optional)
        $configFile = $contentPath . '/config.php';
        if (file_exists($configFile)) {
            $results['checks']['config_file'] = [
                'status' => 'pass',
                'message' => "Config file exists"
            ];
        }

        foreach ($results['checks'] as $check) {
            if ($check['status'] === 'fail') {
                $results['status'] = 'fail';
            }
        }

        return $results;
    }
}
