<?php

namespace CMS;

class MarkdownParser {
    public static function parse(string $markdown): array {
        $data = [
            'meta' => [],
            'content' => ''
        ];

        // Basic Front Matter Extraction
        if (preg_match('/^---\s*\n(.*?)\n---\s*\n/s', $markdown, $matches)) {
            $lines = explode("\n", trim($matches[1]));
            foreach ($lines as $line) {
                if (str_contains($line, ':')) {
                    [$key, $value] = explode(':', $line, 2);
                    $data['meta'][trim($key)] = trim($value);
                }
            }
            $markdown = substr($markdown, strlen($matches[0]));
        }

        // Very Simple Markdown to HTML Conversion
        $html = $markdown;
        
        // Headers
        $html = preg_replace('/^# (.*)$/m', '<h1>$1</h1>', $html);
        $html = preg_replace('/^## (.*)$/m', '<h2>$1</h2>', $html);
        $html = preg_replace('/^### (.*)$/m', '<h3>$1</h3>', $html);
        
        // Bold
        $html = preg_replace('/\*\*(.*?)\*\*/', '<strong>$1</strong>', $html);
        
        // Lists
        $html = preg_replace('/^- (.*)$/m', '<li>$1</li>', $html);
        $html = preg_replace('/(<li>.*<\/li>)/s', '<ul>$1</ul>', $html);
        
        // Paragraphs (Simple split)
        $paragraphs = explode("\n\n", trim($html));
        foreach ($paragraphs as &$p) {
            if (!str_starts_with($p, '<')) {
                $p = "<p>{$p}</p>";
            }
        }
        $data['content'] = implode('', $paragraphs);

        return $data;
    }
}
