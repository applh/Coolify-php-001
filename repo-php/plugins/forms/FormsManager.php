<?php

class FormsManager {
    private static $contextPath;

    public static function setContext($path) {
        self::$contextPath = $path;
    }

    private static function getFormsFile($site) {
        $dir = self::$contextPath . '/' . $site . '/forms';
        if (!is_dir($dir)) {
            @mkdir($dir, 0777, true);
        }
        return $dir . '/forms.json';
    }

    private static function getSubmissionsFile($site, $formId) {
        $dir = self::$contextPath . '/' . $site . '/forms';
        if (!is_dir($dir)) {
            @mkdir($dir, 0777, true);
        }
        return $dir . '/submissions_' . preg_replace('/[^a-zA-Z0-9_-]/', '', $formId) . '.json';
    }

    public static function getForms($site) {
        $file = self::getFormsFile($site);
        if (!file_exists($file)) {
            return [];
        }
        $data = json_decode(file_get_contents($file), true);
        return $data['forms'] ?? [];
    }

    public static function saveForm($site, $form) {
        $forms = self::getForms($site);
        if (!isset($form['id'])) {
            $form['id'] = uniqid('form_');
            $form['created_at'] = date('Y-m-d H:i:s');
        }
        $form['updated_at'] = date('Y-m-d H:i:s');

        $found = false;
        foreach ($forms as &$f) {
            if ($f['id'] === $form['id']) {
                $f = $form;
                $found = true;
                break;
            }
        }
        if (!$found) {
            $forms[] = $form;
        }

        file_put_contents(self::getFormsFile($site), json_encode(['forms' => $forms], JSON_PRETTY_PRINT));
        return $form;
    }

    public static function deleteForm($site, $formId) {
        $forms = self::getForms($site);
        $newForms = array_filter($forms, function($f) use ($formId) {
            return $f['id'] !== $formId;
        });
        file_put_contents(self::getFormsFile($site), json_encode(['forms' => array_values($newForms)], JSON_PRETTY_PRINT));
        
        // Optionally delete submissions file
        $subFile = self::getSubmissionsFile($site, $formId);
        if (file_exists($subFile)) {
            @unlink($subFile);
        }
    }

    public static function addSubmission($site, $formId, $data) {
        $file = self::getSubmissionsFile($site, $formId);
        $submissions = [];
        if (file_exists($file)) {
            $json = json_decode(file_get_contents($file), true);
            $submissions = $json['submissions'] ?? [];
        }
        
        $submission = [
            'id' => uniqid('sub_'),
            'data' => $data,
            'submitted_at' => date('Y-m-d H:i:s'),
            'ip' => $_SERVER['REMOTE_ADDR'] ?? 'unknown'
        ];
        
        $submissions[] = $submission;
        file_put_contents($file, json_encode(['submissions' => $submissions], JSON_PRETTY_PRINT));
        return $submission;
    }

    public static function getSubmissions($site, $formId) {
        $file = self::getSubmissionsFile($site, $formId);
        if (!file_exists($file)) {
            return [];
        }
        $data = json_decode(file_get_contents($file), true);
        return $data['submissions'] ?? [];
    }

    public static function renderForm($site, $formId) {
        $forms = self::getForms($site);
        $form = null;
        foreach ($forms as $f) {
            if ($f['id'] === $formId || (isset($f['slug']) && $f['slug'] === $formId)) {
                $form = $f;
                break;
            }
        }

        if (!$form) {
            return "<p class='text-red-500'>Form not found: $formId</p>";
        }

        ob_start();
        ?>
        <form method="POST" action="" class="space-y-6 bg-white/50 p-8 border border-black/5 rounded-sm">
            <input type="hidden" name="cms_form_id" value="<?php echo htmlspecialchars($form['id']); ?>">
            <h3 class="text-2xl serif italic mb-4"><?php echo htmlspecialchars($form['title']); ?></h3>
            <?php if (isset($form['description'])): ?>
                <p class="text-sm opacity-60 mb-6"><?php echo htmlspecialchars($form['description']); ?></p>
            <?php endif; ?>

            <?php foreach ($form['fields'] as $field): ?>
                <div class="space-y-2">
                    <label class="block text-[10px] uppercase tracking-widest opacity-40 font-mono">
                        <?php echo htmlspecialchars($field['label']); ?>
                        <?php if (isset($field['required']) && $field['required']): ?><span class="text-red-500">*</span><?php endif; ?>
                    </label>
                    <?php if ($field['type'] === 'textarea'): ?>
                        <textarea 
                            name="f_<?php echo htmlspecialchars($field['name']); ?>" 
                            class="w-full bg-white border border-black/10 p-3 text-sm focus:outline-none focus:border-black/30 min-h-[120px]"
                            <?php echo (isset($field['required']) && $field['required']) ? 'required' : ''; ?>
                        ></textarea>
                    <?php elseif ($field['type'] === 'select'): ?>
                        <select 
                            name="f_<?php echo htmlspecialchars($field['name']); ?>" 
                            class="w-full bg-white border border-black/10 p-3 text-sm focus:outline-none focus:border-black/30"
                            <?php echo (isset($field['required']) && $field['required']) ? 'required' : ''; ?>
                        >
                            <?php foreach ($field['options'] as $option): ?>
                                <option value="<?php echo htmlspecialchars($option); ?>"><?php echo htmlspecialchars($option); ?></option>
                            <?php endforeach; ?>
                        </select>
                    <?php else: ?>
                        <input 
                            type="<?php echo htmlspecialchars($field['type']); ?>" 
                            name="f_<?php echo htmlspecialchars($field['name']); ?>" 
                            class="w-full bg-white border border-black/10 p-3 text-sm focus:outline-none focus:border-black/30"
                            <?php echo (isset($field['required']) && $field['required']) ? 'required' : ''; ?>
                        >
                    <?php endif; ?>
                </div>
            <?php endforeach; ?>

            <button type="submit" class="w-full bg-black text-white text-[10px] uppercase font-bold tracking-widest py-4 hover:bg-black/80 transition-all">
                <?php echo htmlspecialchars($form['submit_label'] ?? 'Submit'); ?>
            </button>
        </form>
        <?php
        return ob_get_clean();
    }
}
