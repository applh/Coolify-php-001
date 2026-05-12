<?php

class Component {
    private static $componentDir = __DIR__ . '/../views/components/';

    /**
     * Renders a component with properties and slots
     * 
     * @param string $name Name of the component file (e.g., 'Card')
     * @param array $props Associative array of data passed to the component
     * @param mixed $slots Either a closure for the default slot, or an array of closures for named slots
     */
    public static function render($name, $props = [], $slots = null) {
        $fullPath = self::$componentDir . $name . '.php';
        
        if (!file_exists($fullPath)) {
            echo "<!-- Component not found: " . htmlspecialchars($name) . " -->";
            return;
        }

        // Prepare context
        extract(Store::all());
        extract($props);
        
        // Define a helper for rendering slots inside the component file
        $slot = function($slotName = 'default') use ($slots) {
            if ($slots === null) return;
            
            if (is_callable($slots) && $slotName === 'default') {
                $slots();
            } elseif (is_array($slots) && isset($slots[$slotName]) && is_callable($slots[$slotName])) {
                $slots[$slotName]();
            }
        };

        // If 'async' prop is set, we could handle it here or in a wrapper
        if (isset($props['async']) && $props['async'] === true) {
            self::renderAsyncPlaceholder($name, $props);
            return;
        }

        require $fullPath;
    }

    /**
     * Renders a placeholder for a component that will be loaded via AJAX
     */
    private static function renderAsyncPlaceholder($name, $props) {
        $compId = 'comp-' . uniqid();
        $encodedProps = base64_encode(json_encode($props));
        echo "<div id='{$compId}' data-component='{$name}' data-props='{$encodedProps}' class='component-async-loading'>";
        echo "<div class='animate-pulse bg-gray-200 h-24 rounded-lg flex items-center justify-center text-xs text-gray-400'>Loading {$name}...</div>";
        echo "</div>";
    }

    /**
     * Helper to render a nested component (aliasing for cleaner syntax)
     */
    public static function child($name, $props = [], $slots = null) {
        self::render($name, $props, $slots);
    }
}
