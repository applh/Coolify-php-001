<?php
/**
 * Plugin Name: SEO Optimizer
 * Description: Modifies the site title to include site name.
 */

PluginManager::addFilter('site_title', function($title) {
    return $title . " | " . $_SERVER['HTTP_HOST'];
});
