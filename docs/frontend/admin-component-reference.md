# Admin Component Reference

This document provides a detailed technical reference for the reusable Vue 3 components used in the PHP CMS Admin Area.

## `CmsButton`
A highly stylized button component following the "Artful Minimalist" aesthetic.

### Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `variant` | `String` | `'primary'` | Visual style: `primary`, `secondary`, `outline`, `danger`. |
| `size` | `String` | `'md'` | Size: `sm`, `md`, `lg`. |
| `disabled` | `Boolean` | `false` | Disables interaction. |
| `loading` | `Boolean` | `false` | Shows a spinner and disables interaction. |
| `icon` | `String` | `null` | Name of the Lucide icon to display. |

### Slots
- `default`: The button text or content.

---

## `CmsCard`
A container for grouped information with hover interactions and decorative icons.

### Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `title` | `String` | `null` | Main heading of the card. |
| `subtitle` | `String` | `null` | Small decorative subtitle below the title. |
| `icon` | `String` | `null` | Lucide icon name for the large background decoration. |

### Slots
- `default`: Main content area.
- `actions`: Footer area for buttons or links.

---

## `CmsTable`
A data table designed for high contrast and readability.

### Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `items` | `Array` | `[]` | Data rows to display. |
| `columns` | `Array` | `[]` | Column definitions: `[{ key: 'id', label: 'ID' }]`. |
| `loading` | `Boolean` | `false` | Shows a pulse animation while loading. |
| `emptyMessage` | `String` | `'No items found.'` | Text shown when items array is empty. |

### Slots
- `col-[key]`: Custom renderer for a specific column. Recieves `{ item, value }`.
- `actions`: Right-aligned actions column. Recieves `{ item }`.

---

## `CmsModal`
A full-screen overlay for complex interactions (Forms, Editors).

### Props
| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `show` | `Boolean` | `false` | Controls visibility. |
| `title` | `String` | `''` | Header title. |
| `size` | `String` | `'md'` | Width: `sm`, `md`, `lg`, `xl`. |

### Slots
- `default`: Main scrollable body.
- `footer`: Action buttons at the bottom.

### Events
- `@close`: Triggered when clicking the 'X' or backdrop.
