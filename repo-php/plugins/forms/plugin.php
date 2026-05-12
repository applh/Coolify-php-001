<?php
/**
 * Plugin Name: Forms
 * Description: Build and manage custom forms for your sites.
 */

require_once __DIR__ . '/FormsManager.php';

// Initialize context
FormsManager::setContext(realpath(__DIR__ . '/../../content'));

/**
 * Global helper class for templates
 */
class Forms {
    public static function render($formId) {
        $activeSite = self::getActiveSite();
        echo FormsManager::renderForm($activeSite, $formId);
    }

    private static function getActiveSite() {
        // Simple logic to get current site, mirroring Router.php logic
        $activeSite = getenv('ACTIVE_SITE_OVERRIDE');
        $httpHost = $_SERVER['HTTP_HOST'] ?? 'site1.com';
        if (!$activeSite) {
             // We don't have easy access to domain map here without re-reading it
             // but usually $httpHost is what we want for content subfolder
             $activeSite = $httpHost;
        }
        return preg_replace('/[^a-zA-Z0-9.-]/', '', $activeSite);
    }
}

// Handle Form Submissions
PluginManager::addAction('head', function() {
    if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['cms_form_id'])) {
        $formId = $_POST['cms_form_id'];
        $formData = [];
        foreach ($_POST as $key => $value) {
            if (strpos($key, 'f_') === 0) {
                $fieldName = substr($key, 2);
                $formData[$fieldName] = $value;
            }
        }

        $activeSite = getenv('ACTIVE_SITE_OVERRIDE') ?: ($_SERVER['HTTP_HOST'] ?? 'site1.com');
        $activeSite = preg_replace('/[^a-zA-Z0-9.-]/', '', $activeSite);

        FormsManager::addSubmission($activeSite, $formId, $formData);
        
        // Store success message in session or simple global for this request
        $GLOBALS['cms_form_success'] = true;
    }
});

// Show success message if submitted
PluginManager::addAction('head', function() {
    if (isset($GLOBALS['cms_form_success'])) {
        echo "<script>alert('Thank you! Your submission has been received.');</script>";
    }
});
