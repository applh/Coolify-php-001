<?php
/**
 * Plugin Name: Simple Analytics
 * Description: Injects a simple tracking script in the footer.
 */

PluginManager::addAction('footer', function() {
    echo "\n<!-- Simple Analytics Plugin -->\n";
    echo "<script>\n";
    echo "  console.log('Analytics tracking enabled for this site.');\n";
    echo "</script>\n";
});
