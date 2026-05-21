# Android SQLite Database Explorer & Manager Applet Plan

**Objective**: Introduce a powerful local SQLite database management applet (`DBScreen.kt` and `DBViewModel.kt`) to the Android Multi-App Hub. This tool will empower developers and power users to create, browse, and perform comprehensive schema and data operations (CRUD) directly on device-stored SQLite files.

---

## 1. Architectural Highlights & Tech Stack

To support editing *any* arbitrary SQLite database files on the file system (rather than just predefined entity-mapped Room databases), the SQLite manager must operate on **raw SQLite APIs** dynamically.

- **Storage Handling**: Standard Android `File` APIs for internal database detection combined with **Storage Access Framework (SAF)** to let users load/create `.db` or `.sqlite` files from any location (such as external SD Cards or Downloads folders).
- **Core Database Engine**: Python-style or standard Android `android.database.sqlite.SQLiteDatabase` instance opened dynamically using custom paths and matching read/write flags.
- **UI Architecture**: Model-View-ViewModel (MVVM) pattern.
  - `DBViewModel` exposes state flows wrapping connection lists, schema queries, table profiles, and paginated dynamic data frames.
  - `DBScreen` presents a responsive multi-pane layout (collapsible index/tree on the left, full work area on the right for tablets/expanded screens).
- **Data Rendering**: Dynamic JSON-driven data grids built via custom Jetpack Compose layouts allowing frictionless vertical and horizontal scrolling through arbitrary column sizes.

---

## 2. Core Features (The Requested Scope)

### A. Database File Management
- **Scan & Create**: Scan the application's local `/databases/` directory automatically. Allow creation of empty SQLite `.db` or `.sqlite` files with a single tap.
- **System-Wide Access**: Integrate SAF file-picker intents allowing users to import existing DBs or select databases from other apps (provided they have read/write access).
- **Database Backups**: Clone and duplicate files seamlessly, saving custom timestamped `.bak.db` copies.

### B. Table & Column Management (Schema CRUD)
- **Table Creator Wizard**: A visual form to define Table names and build out Column definitions (Type, Nullability, Primary Key constraints, and Auto-increment behaviors).
- **Interactive Schema Inspector**: Inspect table DDL, schema columns, indexes, and existing views directly.
- **Schema Modifications (Alter & Drop)**:
  - Drop (Delete) existing tables with schema safety warnings.
  - Dynamically add new Columns running standard SQLite `ALTER TABLE {name} ADD COLUMN ...` statements.

### C. Row Data Management (Record CRUD)
- **Table Data Grid View**: Load table records into an interactive grid with limits and offsets for efficient pagination, ensuring the app handles millions of records without UI hitches.
- **Dynamic Search & Filters**: Perform search filters using standard `LIKE %search%` expressions or apply exact-value filters with relational operators (`=`, `>`, `<`, etc.).
- **Row Insert Wizard**: Present a dynamic modal form matching the table’s column types (e.g., numbers for `INTEGER`, sliders or checks for boolean/bit, files/byte arrays or text strings for `BLOB` fields).
- **Row Editor & Deletion**: Let users single-tap any record within a row to edit specific fields or multi-select records for batch deletion.

---

## 3. Recommended Professional Features

To elevate this database manager to a top-tier utility suited for administrative, development, or academic purposes, we propose the following advanced integrations:

### 1. Advanced Ad-Hoc SQL Runner
- **Freeform Terminal**: Include a fully interactive text compiler panel supporting custom SQL runner queries.
- **Formatting & Syntax Hints**: Standard code formatting hints for keyword inputs (`SELECT`, `JOIN`, `WHERE`, `GROUP BY`, `ORDER BY`).
- **Execution Log**: Track statement performance, execution time (in milliseconds), and the physical row-count affected by each arbitrary transaction.

### 2. CSV/JSON Schema & Data Portability
- **Import Wizard**: Import data from standard external `.csv` or `.json` payloads directly into an existing database table with diagnostic column alignment validation.
- **Export Formats**: Export structured table data safely to `.csv`, `.json`, or direct `.sql` dump sequences (SQL scripts loaded into system sharing intents).

### 3. Integrated Diagnostics & Integrity Panel
- **Pragma Diagnostics**: Execute standard SQLite system utilities via visual buttons:
  - `PRAGMA integrity_check` / `quick_check` to audit database health.
  - `PRAGMA foreign_key_check` to verify references.
- **Storage Sweeper (VACUUM)**: Optimize and rebuild database structures with a visual `VACUUM` toggle to clean up empty physical pages.

### 4. Binary/LOB Interactive Inspector
- **Hex/Text/Image Swapper**: If a cell contains binary stream fields (BLOBs), provide an interactive pop-up viewer to attempt decoding the field as a plain-text string, Hex code array, or a direct Bitmap image.

---

## 4. DB Applet Navigation and Screen Flow

```
[ Hub Launcher ] 
       │
       ▼
 [ DB Applet Launcher ] 
       │
       ├──► Recent Files Drawer / File Explorer Selector (DB Selection)
       │
       ▼
 [ DB Workspace Dashboard ] (Tabs: Schema, Grid Viewer, SQL Runner, Diagnostics)
       │
       ├──► [ Schema Tab ] ──► Table DDL ──► Alter/Add Column Modal
       │                               ├──► Create Table Wizard
       │
       ├──► [ Data Grid Tab ] ──► Filter Panel ──► Row Insert Dynamic Form
       │                                     ──► Inline Cell Editor
       │
       └──► [ SQL Terminal ] ──► Dynamic Result Grid & History Drawer
```

---

## 5. Rollout Phases

1. **Phase 1: Local Connection & SQLite Dynamic Drivers**
   - Implement deep-binding state controllers (`DBViewModel`) and the raw dynamic file reader engine.
   - Design standard empty files generation logic.
2. **Phase 2: Visual Schema Design & Table Creator Assembly**
   - Build out the custom Table Creator Wizard in Jetpack Compose.
   - Set up table and column listing dashboards.
3. **Phase 3: The Record Engine (Grid CRUD)**
   - Create the horizontal/vertical paginated LazyGrid to support displaying variable datasets.
   - Integrate modal CRUD forms mapping column metadata directly.
4. **Phase 4: Pro SQL Console & Diagnostics integration**
   - Implement freeform SQL queries execution layout.
   - Wire up CSV/JSON export/import handlers and Pragma diagnostic checks.
