<?php

/**
 * Central State Store for PHP Components
 * Inspired by global state managers like Pinia or Vuex
 */
class Store {
    private static $state = [];

    /**
     * Set a value in the store
     */
    public static function set($key, $value) {
        self::$state[$key] = $value;
    }

    /**
     * Get a value from the store
     */
    public static function get($key, $default = null) {
        return self::$state[$key] ?? $default;
    }

    /**
     * Get the entire state
     */
    public static function all() {
        return self::$state;
    }

    /**
     * Check if a key exists
     */
    public static function has($key) {
        return isset(self::$state[$key]);
    }

    /**
     * Clear the store
     */
    public static function clear() {
        self::$state = [];
    }
}
