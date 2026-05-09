<?php

namespace CMS;

class SiteManager {
    private string $contentPath;
    private ?string $activeSite = null;
    private array $config = [];

    public function __construct(string $contentPath) {
        $this->contentPath = rtrim($contentPath, '/');
        $this->resolveSite();
    }

    private function resolveSite(): void {
        // 1. Check for Environment Override (Debug/Dev)
        $override = getenv('ACTIVE_SITE_OVERRIDE');
        if ($override) {
            $this->activeSite = $override;
        } else {
            // 2. Resolve via Hostname
            $this->activeSite = $_SERVER['HTTP_HOST'] ?? 'default';
        }

        // 3. Load Config
        $configFile = "{$this->contentPath}/{$this->activeSite}/config.json";
        if (file_exists($configFile)) {
            $this->config = json_decode(file_get_contents($configFile), true) ?? [];
        }
    }

    public function getSitePath(): string {
        return "{$this->contentPath}/{$this->activeSite}";
    }

    public function getActiveSite(): string {
        return $this->activeSite ?? 'unknown';
    }

    public function getConfig(string $key, $default = null) {
        return $this->config[$key] ?? $default;
    }

    public function siteExists(): bool {
        return is_dir($this->getSitePath());
    }
}
