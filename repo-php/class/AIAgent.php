<?php

class AIAgent {
    private $apiKey;
    private $endpoint = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent';

    public function __construct() {
        $this->apiKey = getenv('GEMINI_API_KEY') ?: getenv('gemini_api_key');
    }

    public function isAvailable() {
        return !empty($this->apiKey);
    }

    public function executeTask($task) {
        if (!$this->isAvailable()) {
            throw new Exception("Gemini API key is not configured.");
        }

        $type = $task['type'];
        $payload = $task['payload'];

        switch ($type) {
            case 'improve_text':
                return $this->improveText($payload['text'], $payload['context'] ?? '');
            case 'generate_meta':
                return $this->generateMeta($payload['content']);
            case 'suggest_improvements':
                return $this->suggestImprovements($payload['site_data']);
            default:
                throw new Exception("Unknown task type: $type");
        }
    }

    private function improveText($text, $context) {
        $prompt = "Improve the following text for a professional website.\nContext: $context\n\nText: \"$text\"\n\nProvide only the improved text without any preamble.";
        return $this->askGemini($prompt);
    }

    private function generateMeta($content) {
        $prompt = "Generate an SEO title and meta description for the following page content.\n\nContent: \"$content\"\n\nFormat your response as valid JSON: {\"title\": \"...\", \"description\": \"...\"}";
        $result = $this->askGemini($prompt);
        // Basic cleaning to ensure we only have JSON
        if (preg_match('/\{.*\}/s', $result, $matches)) {
            return json_decode($matches[0], true);
        }
        return ['error' => 'Failed to parse JSON response', 'raw' => $result];
    }

    private function suggestImprovements($siteData) {
        $prompt = "Analyze this website landing page content and suggest 3 specific improvements to increase conversion.\n\nContent: \"$siteData\"\n\nProvide suggestions as a list.";
        return $this->askGemini($prompt);
    }

    private function askGemini($prompt) {
        $url = $this->endpoint . '?key=' . $this->apiKey;
        $data = [
            'contents' => [
                [
                    'parts' => [
                        ['text' => $prompt]
                    ]
                ]
            ]
        ];

        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
        
        $response = curl_exec($ch);
        if (curl_errno($ch)) {
            $error = curl_error($ch);
            curl_close($ch);
            throw new Exception("Curl error: $error");
        }
        
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode !== 200) {
            throw new Exception("Gemini API error (HTTP $httpCode): " . $response);
        }

        $result = json_decode($response, true);
        if (!isset($result['candidates'][0]['content']['parts'][0]['text'])) {
            throw new Exception("Unexpected Gemini response format: " . $response);
        }

        return $result['candidates'][0]['content']['parts'][0]['text'];
    }
}
