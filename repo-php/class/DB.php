<?php

class DB {
    private static $instance = null;
    private $pdo;

    private function __construct() {
        // For flexibility, this could load from an environment variable or config file
        $dataDir = realpath(__DIR__ . '/..') . '/data';
        
        // Ensure data directory exists
        if (!file_exists($dataDir)) {
            mkdir($dataDir, 0777, true);
        }
        
        $dbPath = $dataDir . '/cms.sqlite';
        
        // SQLite DSN
        $dsn = "sqlite:" . $dbPath;
        
        // Example for MySQL/MariaDB:
        // $dsn = "mysql:host=127.0.0.1;dbname=cmsdb;charset=utf8mb4";
        
        // Example for PostgreSQL:
        // $dsn = "pgsql:host=127.0.0.1;dbname=cmsdb";
        
        try {
            // If username/password is needed for MySQL/PgSQL, pass them as 2nd and 3rd parameters
            $this->pdo = new PDO($dsn);
            $this->pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        } catch (PDOException $e) {
            die("Database Connection failed: " . $e->getMessage());
        }
    }

    /**
     * Get the Singleton instance
     */
    public static function getInstance() {
        if (self::$instance === null) {
            self::$instance = new DB();
        }
        return self::$instance;
    }

    /**
     * Get the PDO connection
     */
    public static function getConnection() {
        return self::getInstance()->pdo;
    }

    /**
     * Execute a prepared statement
     */
    public static function query($sql, $params = []) {
        $stmt = self::getConnection()->prepare($sql);
        $stmt->execute($params);
        return $stmt;
    }
}
