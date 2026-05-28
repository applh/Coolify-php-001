# Implementation Plan: Small-Business Features Pack for PHP CMS

This document outlines the architectural design and step-by-step development roadmap for introducing core small-business website features inside the PHP CMS.

---

## 1. Architectural Strategy

To keep the PHP CMS ultra-portable, easy to deploy, and resilient, we will implement these features using a **hybrid flat-file and SQLite architecture**:
1. **Forms System (Multi-tenant Flat-File & SQLite)**: Keep form descriptions in easy-to-sync, human-readable JSON files (`forms.json` per site), but optionally allow submission logging to SQLite for querying and aggregation.
2. **Visits Analytics (SQLite)**: Core analytical tracking demands quick queries and aggregations. Storing page views in a centralized `cms.sqlite` database using SQL allows instant grouping, sorting, and timeframe filtering.
3. **Security Monitoring (SQLite)**: Chronological events such as login attempts and 404 captures will write to high-frequency tables in SQLite, avoiding flat-file locks and maximizing performance.

---

## 2. SQLite Database Schemas
We will expand `/repo-php/class/DB.php` to include an automated table initializer. On application boot, the system checks for and builds the following tables:

### A. Table: `cms_analytics_visits`
Logs tracking data for every page view, optimized for fast insertion and light weight.
```sql
CREATE TABLE IF NOT EXISTS cms_analytics_visits (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    site VARCHAR(255) NOT NULL,
    path VARCHAR(255) NOT NULL,
    ip VARCHAR(45) NOT NULL,
    user_agent TEXT,
    is_bot INTEGER DEFAULT 0, -- 1 = search crawl/robot, 0 = real user
    referrer TEXT,
    visited_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_visits_site_date ON cms_analytics_visits(site, visited_at);
CREATE INDEX IF NOT EXISTS idx_visits_is_bot ON cms_analytics_visits(is_bot);
```

### B. Table: `cms_security_events`
Logs login failures, login successes, and 404 resource errors.
```sql
CREATE TABLE IF NOT EXISTS cms_security_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    site VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- 'login_attempt', '404_error'
    ip VARCHAR(45) NOT NULL,
    uri_or_username VARCHAR(255),    -- Requested URL block for 404, or Username field for Login
    status VARCHAR(20) NOT NULL,     -- 'success', 'failed'
    details TEXT,                    -- Additional info (e.g., browser agent, referrer)
    logged_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_security_site_type ON cms_security_events(site, event_type);
```

---

## 3. High-Quality Coding Blueprints

### A. Visits Analytics (Identify Humans vs. Bots)
We will develop a standard bot regex classifier inside the `Simple Analytics` plugin (`/repo-php/plugins/analytics/plugin.php`). On hook initialization, the plugin grabs headers, Classifies the visitor, and registers the database log record.

```php
<?php
/**
 * Plugin Name: Analytics & Visited Tracker
 * Description: Logs visits to the centralized SQLite archive, identifying human users vs crawling bots.
 */

PluginManager::addAction('head', function() {
    $activeSite = Forms::getActiveSitePublic(); // Standard helper
    $requestUri = parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH);
    $ip = $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1';
    $userAgent = $_SERVER['HTTP_USER_AGENT'] ?? '';
    $referrer = $_SERVER['HTTP_REFERER'] ?? '';

    // Ignore assets & Admin panel routes from general analytics
    if (strpos($requestUri, '/admin') === 0 || preg_match('/\.(js|css|png|jpg|jpeg|gif|ico|svg|zip)$/i', $requestUri)) {
        return;
    }

    // Bot detection regex
    $botRegex = '/(bot|google|bing|yandex|baidu|slurp|crawler|spider|curl|wget|python|guzzle|heritrix|facebot|ia_archiver)/i';
    $isBot = preg_match($botRegex, $userAgent) ? 1 : 0;

    try {
        DB::query(
            "INSERT INTO cms_analytics_visits (site, path, ip, user_agent, is_bot, referrer) VALUES (?, ?, ?, ?, ?, ?)",
            [$activeSite, $requestUri, $ip, $userAgent, $isBot, $referrer]
        );
    } catch (\Exception $e) {
        // Fail silently during template rendering
        error_log("Analytics error: " . $e->getMessage());
    }
});
```

---

### B. Security Tracking Plugin
To capture login failures and 404 page loads cleanly:

#### 1. In `AdminRouter::checkAuth()`:
Inject log triggers right where verification is performed.
```php
// Success Login Case
if ($providedPass === $envPass) {
    if ($providedPass !== '') {
        self::logSecurityEvent($activeSite, 'login_attempt', $_SERVER['REMOTE_ADDR'] ?? 'unknown', 'admin_user', 'success');
    }
    return true;
} else {
    // Failed Login Case
    self::logSecurityEvent($activeSite, 'login_attempt', $_SERVER['REMOTE_ADDR'] ?? 'unknown', 'admin_user', 'failed');
    return false;
}
```

#### 2. In `Router::dispatch()` (404 Fallback):
If index directory doesn't include template or we output 404 headers:
```php
// When 404 occurs in dispatcher:
try {
    DB::query(
        "INSERT INTO cms_security_events (site, event_type, ip, uri_or_username, status, details) VALUES (?, ?, ?, ?, ?, ?)",
        [$activeSite, '404_error', $_SERVER['REMOTE_ADDR'] ?? '127.0.0.1', $requestUri, 'failed', $_SERVER['HTTP_USER_AGENT'] ?? '']
    );
} catch (\Exception $e) {
    error_log($e->getMessage());
}
```

---

### C. Forms Builder & Multi-type Managers
We will add standard multi-type layouts to custom form templates. We support **Newsletter Signups**, **Contact Queries**, and **Poll Responses**:

1. **Newsletter Field Schemas** (Uses a simplified inline UI with custom checkbox toggles).
2. **Poll Form Field Schemas** (Adds `radio` and `checkbox` options to field templates).
3. **Form Manager Actions**:
   - Provide email notifications via internal PHP `mail()` wrapper.
   - Guard against Spam with a hidden Honeypot Input:
     ```html
     <div style="display:none;">
         <input type="text" name="cms_website_honeypot_field" value="">
     </div>
     ```
     If the field comes back populated on POST, the submission is silently dropped as a spam entity.

---

## 4. UI Layout Specifications for PHP Admin

We will expand the Vue 3 application inside `AdminRouter.serveApp()` by:
- Adding non-blocking **"Analytics"** and **"Security"** navigation views to the Admin Panel.
- Extending visual widgets with modern aesthetic details (using deep charcoal layout lines, micro hover-triggers, and subtle layouts).

### Visual Layout Diagram
```
+-------------------------------------------------------------------------+
|  PHP CMS ADMIN  [ Sites ] [ Active Forms ] [ Analytics ] [ Security ]   |
+-------------------------------------------------------------------------+
|                                                                         |
|  [ ANALYTICS DASHBOARD - site1.com ]                                    |
|                                                                         |
|  +-------------------+  +-------------------+  +------------------+     |
|  |  HUMAN VISITORS   |  |  BOT PAGEVIEWS    |  |  CONVERSION RATE |     |
|  |  842              |  |  1,402            |  |  4.8% (Newsletter|     |
|  |  (Unique IPs)     |  |  (Crawl Indices)  |  +------------------+     |
|  +-------------------+  +-------------------+                           |
|                                                                         |
|  [ PAGE VIEWS OVER TIME ]                                               |
|  (Renders a beautiful, scalable SVG lines graph based on database records)|
|                                                                         |
|  +-------------------------------------+  +---------------------------+ |
|  | TOP REQUESTED PATHS                 |  | BOT VS HUMAN SPLIT        | |
|  | 1. /services        (342 views)     |  | Human: [=====  ] 38%      | |
|  | 2. /contact         (211 views)     |  | Robots: [========] 62%      | |
|  | 3. /pricing         (94  views)     |  +---------------------------+ |
|  +-------------------------------------+                                |
|                                                                         |
+-------------------------------------------------------------------------+
|                                                                         |
|  [ SECURITY MONITORING ]                                                |
|                                                                         |
|  +--------------------------------------------------------------------+ |
|  | BRUTE-FORCE PROTECTION (FAILED ADMIN LOGINS)                       | |
|  | IP: 198.51.100.41   Attacked: 'admin'  Logged: 2026-05-28 08:34:20 | |
|  | IP: 198.51.100.41   Attacked: 'admin'  Logged: 2026-05-28 08:34:10 | |
|  +--------------------------------------------------------------------+ |
|                                                                         |
|  +--------------------------------------------------------------------+ |
|  | BROKEN LINKS AND SHELL PROBES (RECENT 404s)                        | |
|  | URL: /wp-admin.php  IP: 203.0.113.84   Logged: 2026-05-28 07:11:00 | |
|  | URL: /team-member   IP: 73.109.20.12   Logged: 2026-05-28 04:32:15 | |
|  +--------------------------------------------------------------------+ |
|                                                                         |
+-------------------------------------------------------------------------+
```

---

## 5. Phased Implementation Roadmap

### Phase 1: Database Migration Blueprint & Core Bootstrapping
- Hook the automatic DB preparation table builders into `/repo-php/class/App.php` or `DB.php`.
- Build diagnostic health validations inside `/repo-php/class/CMS.php` to prove SQLite writable status.

### Phase 2: Analytics Tracking and Security Logging
- Implement `/repo-php/plugins/analytics/plugin.php` using human vs. bot Regex filters.
- Inject telemetry log actions in `Router.php` (for 404 logs) and `AdminRouter.php` (for login monitoring).

### Phase 3: Forms Extension
- Add Poll & Newsletter schema field models in `FormsManager.php`.
- Implement honeypot anti-spam check to safely avoid bot junk.

### Phase 4: Full Admin Vue UI Integration
- Develop two new async components `/repo-php/public/js/components/AdminAnalytics.js` and `/repo-php/public/js/components/AdminSecurity.js`.
- Incorporate these components into `AdminRouter::serveApp()` to display rich KPI stats, SVG graphs, broken-link finders, and suspicious access highlights.

---

## 6. Recommendations & Best-Practice Adjustments

1. **Email Alerts for Lead Submissions**: When contact forms are captured, instantly dispatch an operational alert email using PHP `mail()` (if system configurations support sendmail) or provide an optional Webhook URL field in `forms.json` for Slack/Discord payload integrations.
2. **Honeypot Decoy Techniques**: Adding simple honey-trap fields like `<input name="email_fake_verify" style="display:none">` reduces 99% of bot form submissions without making real users solve complex CAPTCHA riddles.
3. **Privacy and IP Masking (GDPR Standardized)**: To protect local data compliance without legal hazards, implement optional IP masking where the last octet is anonymized (e.g., `192.168.1.134` stored as `192.168.1.xxx`) before writing to the database.
4. **Automated Log Pruning**: SQLite log files can grow quickly on highly crawled sites. Set up a lightweight cron task or background hook that automatically prunes logs older than 90 days (`DELETE FROM cms_analytics_visits WHERE visited_at < DATE('now', '-90 days')`).
