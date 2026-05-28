# Architectural Plan: Multi-Driver DB Persistence & Access Layers (SQLite & PostgreSQL)

This document presents the architectural design, implementation strategy, and database access layer evaluation for adding **PostgreSQL** persistence to the PHP CMS while keeping the existing lightweight **SQLite** database fully supported.

---

## 1. Database Access Layers: Evaluation & Advice

To bridge SQLite and PostgreSQL successfully, the system needs to abstract database connections and optionally SQL generation. Below is a comparative evaluation of the four primary Database Access Layer (DAL) patterns in modern PHP, contextualized for this lightweight CMS:

| Pattern | Detail / Libraries | Pros | Cons | Recommendation for PHP CMS |
| :--- | :--- | :--- | :--- | :--- |
| **1. Raw Custom PDO Wrapper (Current)** | Custom `DB.php` Singleton around native PHP `PDO`. | • Near-zero overhead<br>• Zero composer dependecies<br>• Fully portable and highly resilient | • No help in handling SQL dialect differences<br>• Manual binding of inputs | **Strong Candidate with Repository Pattern.** If queries remain simple, a Repository Pattern abstracts the SQL dialect differences cleanly without library bloat. |
| **2. Micro-Query Builder** | e.g., **USlim**, **Pixie**, or custom query compiler. | • Fluent interface (`$db->table('visits')->insert(...)`) | • Additional dependencies<br>• Limited support for advanced DB-specific queries | **Alternative.** Handy if complex filtering is dynamic. |
| **3. ActiveRecord (Micro-ORM)** | e.g., **Eloquent (Slim)** or customized thin wrappers. | • Highly expressive schema CRUD<br>• Wide developer familiarity | • Pulls in 20+ Illuminate sub-packages<br>• Degrades request bootstrapping time | **Not Recommended.** Destroys the CMS's core design goal of high speed, low dependency footprint, and fast Docker loading times. |
| **4. Repository Pattern / Data Access Object (DAO)** | Business logic calls dynamic driver class implementations. | • Complete isolation of database-specific SQL queries (SQLite vs PostgreSQL)<br>• Easy unit-testing with mocks | • Requires writing specialized repositories for different models | **Highly Recommended Choice.** This is the cleanest architectural pattern to support multiple databases without performance penalties or dependency bloat. |

### Architectural Decision
**We recommend the Repository Pattern with a Light Abstraction Layer.**
Instead of introducing a heavy ORM (like Eloquent) that would slow down the application and bloat `/vendor`, we should introduce **Repositories** (e.g., `VisitsRepository`, `SecurityEventRepository`) interacting on top of an upgraded dynamic `DB` wrapper. 

Under this design, the `DB` class handles connection pooling and multi-driver syntax translations, while repositories isolate SQL queries that contain database-specific syntax (such as SQLite's string concatenation vs. PostgreSQL's, or UPSERT dialects).

---

## 2. Updated Dynamic DB Driver Engine (`DB.php`)

To support both PostgreSQL and SQLite, we will rewrite the system DB class to read from environment variables (`DB_DRIVER`, `DB_HOST`, `DB_PORT`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD`).

Here is the blueprint for `/repo-php/class/DB.php`:

```php
<?php

class DB {
    private static $instance = null;
    private $pdo;
    private $driver;

    private function __construct() {
        // Fetch configurations with environment fallbacks
        $this->driver = getenv('DB_DRIVER') ?: 'sqlite';
        
        try {
            if ($this->driver === 'pgsql') {
                $host = getenv('DB_HOST') ?: '127.0.0.1';
                $port = getenv('DB_PORT') ?: '5432';
                $dbname = getenv('DB_DATABASE') ?: 'cmsdb';
                $user = getenv('DB_USERNAME') ?: 'postgres';
                $password = getenv('DB_PASSWORD') ?: '';

                $dsn = "pgsql:host={$host};port={$port};dbname={$dbname}";
                $this->pdo = new PDO($dsn, $user, $password);
            } else {
                // Default fallback to SQLite
                $dataDir = realpath(__DIR__ . '/..') . '/data';
                if (!file_exists($dataDir)) {
                    mkdir($dataDir, 0777, true);
                }
                $dbPath = $dataDir . '/cms.sqlite';
                $dsn = "sqlite:" . $dbPath;
                $this->pdo = new PDO($dsn);
            }

            $this->pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            $this->pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        } catch (PDOException $e) {
            die("Database connection failed [" . $this->driver . "]: " . $e->getMessage());
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
     * Get the active PDO connection
     */
    public static function getConnection() {
        return self::getInstance()->pdo;
    }

    /**
     * Get the name of current driver ('sqlite' or 'pgsql')
     */
    public static function getDriver() {
        return self::getInstance()->driver;
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
```

---

## 3. SQLite vs PostgreSQL Schema Compatibility

To handle the structural differences between SQLite and PostgreSQL, we will build a dynamic migration utility that translates typical schemas automatically on initialization:

| Feature | SQLite Type | PostgreSQL Type | SQLite Schema Helper | PostgreSQL Schema Helper |
| :--- | :--- | :--- | :--- | :--- |
| **Auto-Increment PK** | `INTEGER PRIMARY KEY AUTOINCREMENT` | `SERIAL PRIMARY KEY` | Handled via pattern detection / mapping | Handled via pattern detection / mapping |
| **Booleans** | `INTEGER DEFAULT 0` | `BOOLEAN DEFAULT FALSE` | Represented as `0` or `1` | Represented as `true` or `false` |
| **Dates** | `DATETIME DEFAULT CURRENT_TIMESTAMP` | `TIMESTAMP DEFAULT CURRENT_TIMESTAMP` | Standard string format | Standard native timestamp |

### Unified Auto-Migrator Design (`class/DatabaseMigrator.php`)
This class will be responsible for defining the tables abstractly or choosing the correct SQL block based on the active driver:

```php
<?php

class DatabaseMigrator {
    
    public static function initTables() {
        $driver = DB::getDriver();
        
        if ($driver === 'pgsql') {
            self::runPostgreSqlMigrations();
        } else {
            self::runSqliteMigrations();
        }
    }

    private static function runSqliteMigrations() {
        // Analytics Table
        DB::query("CREATE TABLE IF NOT EXISTS cms_analytics_visits (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            site VARCHAR(255) NOT NULL,
            path VARCHAR(255) NOT NULL,
            ip VARCHAR(45) NOT NULL,
            user_agent TEXT,
            is_bot INTEGER DEFAULT 0,
            referrer TEXT,
            visited_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )");
        DB::query("CREATE INDEX IF NOT EXISTS idx_visits_site_date ON cms_analytics_visits(site, visited_at)");

        // Security Table
        DB::query("CREATE TABLE IF NOT EXISTS cms_security_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            site VARCHAR(255) NOT NULL,
            event_type VARCHAR(50) NOT NULL,
            ip VARCHAR(45) NOT NULL,
            uri_or_username VARCHAR(255),
            status VARCHAR(20) NOT NULL,
            details TEXT,
            logged_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )");
        DB::query("CREATE INDEX IF NOT EXISTS idx_security_site_type ON cms_security_events(site, event_type)");
    }

    private static function runPostgreSqlMigrations() {
        // Analytics Table in PG
        DB::query("CREATE TABLE IF NOT EXISTS cms_analytics_visits (
            id SERIAL PRIMARY KEY,
            site VARCHAR(255) NOT NULL,
            path VARCHAR(255) NOT NULL,
            ip VARCHAR(45) NOT NULL,
            user_agent TEXT,
            is_bot BOOLEAN DEFAULT FALSE,
            referrer TEXT,
            visited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )");
        DB::query("CREATE INDEX IF NOT EXISTS idx_visits_site_date ON cms_analytics_visits(site, visited_at)");

        // Security Table in PG
        DB::query("CREATE TABLE IF NOT EXISTS cms_security_events (
            id SERIAL PRIMARY KEY,
            site VARCHAR(255) NOT NULL,
            event_type VARCHAR(50) NOT NULL,
            ip VARCHAR(45) NOT NULL,
            uri_or_username VARCHAR(255),
            status VARCHAR(20) NOT NULL,
            details TEXT,
            logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )");
        DB::query("CREATE INDEX IF NOT EXISTS idx_security_site_type ON cms_security_events(site, event_type)");
    }
}
```

---

## 4. Phase-by-Phase Integration Strategy

```
  +------------------+     +------------------+     +------------------+
  |     PHASE 1      |     |     PHASE 2      |     |     PHASE 3      |
  | Update DB Config | --> | Build Migrations | --> | Repositories Ref |
  |   (DB.php Env)   |     | (Auto Schema)    |     | (SQL Isolation)  |
  +------------------+     +------------------+     +------------------+
```

### Phase 1: Environment Orchestration
- Create environment defaults in `.env.example` in both `/` and `/repo-php/`.
- Provide PostgreSQL database container initialization inside `docker-compose.yml` to support unified local development setups.

### Phase 2: Schema Migration Execution
- Call `DatabaseMigrator::initTables()` during `/repo-php/class/App.php` initialization block.
- Create automated testing verification scripts in `/repo-php/tests` to confirm successful table structures and indexes are created under PostgreSQL.

### Phase 3: Abstract DB Operations (Repositories)
- Build `/repo-php/class/repository/VisitsRepository.php`.
- Build `/repo-php/class/repository/SecurityEventRepository.php`.
- Abstract database inserts inside `Simple Analytics` plugin:
  ```php
  // Instead of direct raw SQL querying, plugins now interact via Repositories
  VisitsRepository::logVisit($site, $path, $ip, $userAgent, $isBot, $referrer);
  ```

---

## 5. Deployment & Local Dev Sandbox

To make development zero-friction, we will introduce a PostgreSQL service to `/repo-php/docker-compose.yml`:

```yaml
version: '3.8'

services:
  # Existing Web PHP CMS Service
  web:
    build: .
    ports:
      - "3000:3000"
    environment:
      - DB_DRIVER=pgsql
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_DATABASE=cmsdb
      - DB_USERNAME=postgres
      - DB_PASSWORD=secret_postgres_pass
    depends_on:
      - postgres

  # Dedicated Postgres Service for persistent sandboxing
  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=cmsdb
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=secret_postgres_pass
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  pgdata:
```
This configuration keeps SQLite active when `DB_DRIVER=sqlite` is specified (with zero external container dependencies needed), whilst providing immediate developer playground support for high-traffic PostgreSQL configurations.
