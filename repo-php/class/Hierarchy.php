<?php

/**
 * Hierarchy handles the building of templates by level.
 * It builds from the inside-out (innermost content first, then wraps it in parent levels).
 */
class Hierarchy {
    private $levels = [];

    /**
     * Add a level to the stack (ordered from outside to inside)
     * 
     * @param string $view Path to the view file
     * @param array $data Data for this level
     */
    public function wrap($view, $data = []) {
        $this->levels[] = [
            'view' => $view,
            'data' => $data
        ];
        return $this;
    }

    /**
     * Renders the hierarchy by building from the bottom (innermost) up.
     */
    public function render() {
        if (empty($this->levels)) {
            return '';
        }

        // Start with the innermost level
        // Since wrap() adds to the end, the last element is the "innermost" level.
        $stack = array_reverse($this->levels);
        
        $content = '';
        foreach ($stack as $index => $level) {
            // The result of the previous (inner) level is passed as $slot to the next (outer) level
            $levelData = $level['data'];
            if ($index > 0) {
                $levelData['slot'] = $content;
            }
            
            $content = View::renderToString($level['view'], $levelData);
        }

        return $content;
    }
}
