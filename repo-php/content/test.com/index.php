<?php

$page = new Hierarchy();

// Level 0: Global Shell
$page->wrap('shell', [
    'title' => 'Hierarchy Demo'
]);

// Level 1: Sidebar Layout
$page->wrap('layouts/sidebar');

// Level 2: Page View
$page->wrap('views/home', [
    'heading' => 'The New Hierarchy System'
]);

echo $page->render();
