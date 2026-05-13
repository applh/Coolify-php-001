# Vue Async Forms Implementation & Setup

This document describes how the Vue-powered asynchronous forms are implemented and how to set them up for a new site.

## Architecture

The system consists of three main parts:

1.  **PHP Plugin Extension**: Located in `/repo-php/plugins/forms/plugin.php`, it provides a JSON API to fetch form metadata and receive submissions.
2.  **Vue Frontend Component**: Located in `/repo-php/public/js/FormsComponent.js`, it's a plain JavaScript Vue 3 component that handles data fetching, state management, and form rendering.
3.  **Template Helper**: The `Forms::renderVue($formId)` PHP helper which initializes the necessary DOM and auto-enqueues the required scripts.

## How to Set Up

### 1. Plugin Activation
Ensure the `forms` plugin is active for your site. Edit `content/{your-site}/config.json`:

```json
{
  "active_plugins": ["forms"]
}
```

### 2. Form Definition
Define your forms in `content/{your-site}/forms/forms.json`.
Example:
```json
{
  "forms": [
    {
      "id": "form_1",
      "slug": "contact",
      "title": "Contact Us",
      "fields": [
        { "label": "Email", "name": "email", "type": "email", "required": true }
      ]
    }
  ]
}
```

### 3. Usage in Templates
In your site's template (e.g., `index.php`), use the `renderVue` method:

```php
<?php Forms::renderVue('contact'); ?>
```

This method will:
-   Insert a `<div data-vue-form="contact"></div>` placeholder.
-   Hook into the `footer` action to load Vue.js from a CDN and the `FormsComponent.js` script.
-   The component will auto-mount on all elements with `data-vue-form`.

## API Endpoints

The forms plugin adds the following internal API endpoints accessible via the site root:

-   **GET `/?cms_api=get_form&id={slug}`**: Returns form metadata (fields, title, etc.) as JSON.
-   **POST `/?cms_api=submit_form`**: Accepts a JSON payload with `cms_form_id` and `data` objects. Stores the submission and returns a success/error message.

## Customization

### Styling
The component uses Tailwind CSS classes that match the CMS design language (Instrument Serif fonts, minimalist borders, monochrome palette). You can override these by editing the `template` string in `/repo-php/public/js/FormsComponent.js`.

### Loading States
The component includes a built-in "skeleton" loading state that appears while the form metadata is being fetched from the API.

### Success Message
The success state is handled entirely in Vue, providing immediate feedback without a full page reload or alert box.
