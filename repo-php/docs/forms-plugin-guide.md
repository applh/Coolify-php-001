# Forms Plugin Guide

The Forms plugin allows you to build custom forms (Contact, Newsletter, etc.) for each of your sites and manage their submissions directly from the Admin Panel.

## Admin Setup

1. **Activate the Plugin**:
   Ensure the `forms` plugin is listed in your site's `config.json` under `active_plugins`.
   Example:
   ```json
   {
     "active_plugins": ["analytics", "seo-optimizer", "forms"]
   }
   ```

2. **Access Forms Manager**:
   - Go to the Admin Dashboard.
   - Every site in the list now has a **Forms** button.
   - Click it to view, create, or edit forms for that specific site.

3. **Building a Form**:
   - Click **Create New Form**.
   - Provide a **Title** (e.g., "Contact Us").
   - Provide a **Slug** (e.g., `contact-form`). This ID is used to embed the form.
   - Add fields like Text, Email, Textarea, or Select.
   - For Select fields, provide a comma-separated list of options.

## User Guide: Embedding a Form

To display a form on your site, use the `Forms::render()` helper in your site's `index.php` template.

```php
<?php
// content/mysite.com/index.php

Layout::header("Contact Us");
?>

<div class="max-w-xl mx-auto py-12">
    <h1 class="text-3xl serif italic mb-8">Get in touch</h1>
    
    <?php Forms::render('contact-form'); ?>
</div>

<?php
Layout::footer();
?>
```

## Advanced: Vue Async Form Component

For a smoother user experience without page reloads, you can use the Vue-powered async component. This version fetches form structure and submits data using AJAX.

### 1. Usage in PHP Template

Replace `Forms::render()` with `Forms::renderVue()`:

```php
<div class="max-w-xl mx-auto py-12">
    <h1 class="text-3xl serif italic mb-8">AJAX Contact Form</h1>
    
    <?php Forms::renderVue('contact-form'); ?>
</div>
```

### 2. Manual Integration (Outside of `Forms::renderVue`)

If you want to integrate the component manually or in a different way, you need to include the following scripts:

```html
<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
<script src="/js/FormsComponent.js"></script>
```

Then, place a `div` with `data-vue-form` attribute:

```html
<div data-vue-form="contact-form"></div>
```

### 3. Features of the Vue Component

- **Seamless Submissions**: No page refresh on submit.
- **Loading States**: Built-in skeleton/loading animations.
- **Dynamic Validation**: Real-time type checking based on the form definition.
- **Improved UX**: Customizable success/error messages within the component.

## Managing Submissions

- In the Admin Panel, click **Forms** on your site.
- Click **Submissions** next to the form you want to inspect.
- You can view all responses with submission metadata (date, IP).
- Use **Export JSON** to download all submissions for external processing.

## Technical Details

- **Storage**: definitions are stored in `/content/{site}/forms/forms.json`.
- **Submissions**: stored in `/content/{site}/forms/submissions_{formId}.json`.
- **Multi-site**: data is isolated per site.
