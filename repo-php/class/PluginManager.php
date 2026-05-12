<?php

/**
 * PluginManager handles discovery, activation, and hooks (actions/filters)
 */
class PluginManager {
    private static $actions = [];
    private static $filters = [];
    private static $activePlugins = [];
    private static $pluginDir;

    /**
     * Initialize the Plugin Manager for a specific site
     */
    public static function init($siteDir) {
        self::$pluginDir = realpath(__DIR__ . '/../plugins');
        
        // Ensure plugins directory exists
        if (!is_dir(self::$pluginDir)) {
            @mkdir(self::$pluginDir, 0777, true);
        }

        $configPath = $siteDir . '/config.json';
        if (file_exists($configPath)) {
            $config = json_decode(file_get_contents($configPath), true);
            if (isset($config['active_plugins']) && is_array($config['active_plugins'])) {
                self::loadPlugins($config['active_plugins']);
            }
        }
    }

    /**
     * Load specified plugins
     */
    private static function loadPlugins($plugins) {
        foreach ($plugins as $pluginName) {
            // Sanitize plugin name
            $pluginName = preg_replace('/[^a-zA-Z0-9_-]/', '', $pluginName);
            $pluginFile = self::$pluginDir . '/' . $pluginName . '/plugin.php';

            if (file_exists($pluginFile)) {
                self::$activePlugins[] = $pluginName;
                include_once $pluginFile;
            }
        }
    }

    /**
     * ACTIONS: Register a callback for a hook
     */
    public static function addAction($hook, $callback, $priority = 10) {
        if (!isset(self::$actions[$hook])) {
            self::$actions[$hook] = [];
        }
        self::$actions[$hook][] = ['callback' => $callback, 'priority' => $priority];
        
        // Sort by priority
        usort(self::$actions[$hook], function($a, $b) {
            return $a['priority'] <=> $b['priority'];
        });
    }

    /**
     * ACTIONS: Trigger all callbacks for a hook
     */
    public static function doAction($hook, ...$args) {
        if (!isset(self::$actions[$hook])) return;
        
        foreach (self::$actions[$hook] as $action) {
            call_user_func_array($action['callback'], $args);
        }
    }

    /**
     * FILTERS: Register a callback to modify data
     */
    public static function addFilter($hook, $callback, $priority = 10) {
        if (!isset(self::$filters[$hook])) {
            self::$filters[$hook] = [];
        }
        self::$filters[$hook][] = ['callback' => $callback, 'priority' => $priority];
        
        // Sort by priority
        usort(self::$filters[$hook], function($a, $b) {
            return $a['priority'] <=> $b['priority'];
        });
    }

    /**
     * FILTERS: Run data through all callbacks for a hook
     */
    public static function applyFilters($hook, $value, ...$args) {
        if (!isset(self::$filters[$hook])) return $value;
        
        foreach (self::$filters[$hook] as $filter) {
            $value = call_user_func_array($filter['callback'], array_merge([$value], $args));
        }
        return $value;
    }

    /**
     * Get list of active plugins
     */
    public static function getActivePlugins() {
        return self::$activePlugins;
    }

    /**
     * Check if a plugin is active
     */
    public static function isPluginActive($pluginName) {
        return in_array($pluginName, self::$activePlugins);
    }
}
