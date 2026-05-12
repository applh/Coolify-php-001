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

    /**
     * Renders a placeholder for the Vue Async Form component.
     * Use this for a more interactive, AJAX-based experience.
     */
    public static function renderVue($formId) {
        $activeSite = self::getActiveSite();
        echo "<div data-vue-form='" . htmlspecialchars($formId) . "' class='min-h-[200px]'></div>";
        
        // Auto-enqueue scripts in footer
        PluginManager::addAction('footer', function() {
            echo '<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>';
            echo '<script src="/js/FormsComponent.js"></script>';
        }, 5);
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

// JSON API for Vue Component
PluginManager::addAction('head', function() {
    if (isset($_GET['cms_api'])) {
        $api = $_GET['cms_api'];
        $activeSite = getenv('ACTIVE_SITE_OVERRIDE') ?: ($_SERVER['HTTP_HOST'] ?? 'site1.com');
        $activeSite = preg_replace('/[^a-zA-Z0-9.-]/', '', $activeSite);

        if ($api === 'get_form' && isset($_GET['id'])) {
            $formId = $_GET['id'];
            $forms = FormsManager::getForms($activeSite);
            $form = null;
            foreach ($forms as $f) {
                if ($f['id'] === $formId || (isset($f['slug']) && $f['slug'] === $formId)) {
                    $form = $f;
                    break;
                }
            }
            
            header('Content-Type: application/json');
            if ($form) {
                echo json_encode($form);
            } else {
                http_response_code(404);
                echo json_encode(['error' => 'Form not found']);
            }
            exit;
        }

        if ($api === 'submit_form' && $_SERVER['REQUEST_METHOD'] === 'POST') {
            $input = json_decode(file_get_contents('php://input'), true);
            $formId = $input['cms_form_id'] ?? null;
            $formData = $input['data'] ?? [];

            if (!$formId) {
                http_response_code(400);
                echo json_encode(['error' => 'Missing form ID']);
                exit;
            }

            FormsManager::addSubmission($activeSite, $formId, $formData);
            
            header('Content-Type: application/json');
            echo json_encode(['success' => true, 'message' => 'Thank you! Your submission has been received.']);
            exit;
        }
    }
});
