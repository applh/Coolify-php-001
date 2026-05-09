<?php

namespace CMS;

class Router {
    private string $sitePath;

    public function __construct(string $sitePath) {
        $this->sitePath = $sitePath;
    }

    public function resolvePage(): string {
        $uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
        $uri = trim($uri, '/');

        if (empty($uri)) {
            $uri = 'index';
        }

        $file = "{$this->sitePath}/{$uri}.md";

        if (file_exists($file)) {
            return $file;
        }

        return ''; // 404
    }
}
