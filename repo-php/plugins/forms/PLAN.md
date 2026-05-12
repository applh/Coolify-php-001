# Forms Plugin Implementation Plan

## Goal
Add a flexible Forms plugin to the PHP CMS that allows site owners to create custom forms and collect submissions.

## Architecture
1. **Plugin Core**: Located in `/repo-php/plugins/forms/`.
2. **Data Model**:
   - `forms.json`: Stores form definitions (fields, settings) per site.
   - `submissions_{id}.json`: Stores form submissions per form.
3. **Admin Backend**:
   - Extended `AdminRouter` to handle form CRUD and submission viewing.
   - Integrated Vue.js components in the Admin SPA.
4. **Visitor Frontend**:
   - `Forms::render($formId)` helper to embed forms in templates.
   - Automatic submission handling via POST interception.

## Implementation Steps
1. **Create Plugin Skeleton**:
   - `repo-php/plugins/forms/plugin.php`
   - `repo-php/plugins/forms/FormsManager.php`
2. **Develop FormsManager Class**:
   - Methods for listing forms, creating/updating/deleting forms.
   - Methods for saving submissions.
   - Methods for rendering form HTML.
3. **Extend Admin Panel**:
   - Update `AdminRouter.php` to add new API endpoints.
   - Update the Vue.js app in `AdminRouter.php` to include:
     - Forms list view.
     - Form editor (JSON or basic field builder).
     - Submissions list view.
4. **Frontend Integration**:
   - Add POST request listener in `plugin.php`.
   - Implement basic validation and success/error feedback.
5. **Documentation**:
   - Create `docs/forms-plugin-guide.md`.
6. **Testing**:
   - Verify form creation.
   - Verify form rendering.
   - Verify submission storage and viewing.
