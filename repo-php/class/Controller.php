<?php

abstract class Controller {
    /**
     * Render a view with optional data
     */
    protected function view($path, $data = []) {
        View::render($path, $data);
    }
    
    /**
     * Return a JSON response
     */
    protected function json($data, $statusCode = 200) {
        http_response_code($statusCode);
        header('Content-Type: application/json');
        echo json_encode($data);
        exit;
    }
}
