# PHP MVC Architecture & Database Implementation

To separate concerns and handle data cleanly, we implemented a lightweight MVC architecture and a generic Database handler utilizing PDO.

## 1. Database Class (`DB.php`)

The `DB` class uses the **Singleton pattern** to ensure only one active connection to the database exists per request. It utilizes **PDO** which allows switching the underlying database driver easily.

Currently, it defaults to **SQLite**:
`$dsn = "sqlite:" . $dbPath;`

**Switching to MariaDB/MySQL or PostgreSQL:**
To switch engines, you simply provide the correct DSN inside `DB.php`:

```php
// MySQL / MariaDB
$dsn = "mysql:host=localhost;dbname=cmsdb;charset=utf8mb4";
$pdo = new PDO($dsn, "username", "password");

// PostgreSQL
$dsn = "pgsql:host=localhost;dbname=cmsdb";
$pdo = new PDO($dsn, "username", "password");
```

### Static Execution
Queries can be executed statically:
```php
$stmt = DB::query("SELECT * FROM users WHERE status = ?", ['active']);
$users = $stmt->fetchAll();
```

## 2. Base MVC Classes

### The `Controller` Class
Provides shared helper methods to easily render Views or return JSON (useful for internal APIs or asynchronous requests).

```php
class UserController extends Controller {
    public function index() {
        $users = User::all();
        $this->view('users/index', ['users' => $users]);
    }
    
    public function apiGet() {
        $this->json(['status' => 'success', 'data' => User::all()]);
    }
}
```

### The `Model` Class
An abstract class that implements Active-Record-style static methodologies. Child classes define their base table.

```php
class User extends Model {
    protected static $table = 'users';
}

// Automatically gets access to basic CRUD:
$user = User::find(1);
$allUsers = User::all();
```

### The `View` Class
A simple rendering engine that uses `extract()` to turn associative arrays into local variables for the view file. View files are expected to exist in `/repo-php/views/`.

```php
// In a controller: View::render('dashboard/main', ['title' => 'Dashboard']);
// Inside /repo-php/views/dashboard/main.php:
<h1><?= htmlspecialchars($title) ?></h1>
```
