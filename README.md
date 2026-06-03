# Acadex PDF Library

Acadex is a local JavaFX desktop application for organizing PDFs in a strict academic hierarchy:

`Subject > SubSubject > Chapter > Topic > Subtopic`

It stores metadata in SQLite, keeps PDF files on the local filesystem, and renders cached PDF thumbnails for a modern library-style interface.

## Features

- Local-only desktop app built with JavaFX
- SQLite metadata storage
- Hierarchical TreeView navigation
- Cascading filters for type, subject, chapter, topic, and subtopic
- Instant search across PDF name, type, upload date, and full hierarchy path
- Drag-and-drop PDF upload
- Windows FileChooser upload flow
- Content-hash file storage to avoid duplicate physical PDF copies
- Cached first-page thumbnails using PDFBox
- Recent activity panel
- Dynamic Library Coverage pie chart for Quiz vs Theory files

## Requirements

- Windows
- PowerShell
- Internet access on first run if `.tools` has not been provisioned yet

The included `run.ps1` script downloads a local JDK 17 and Maven into `.tools/`, so no system-wide Java or Maven installation is required.

## Run

From the project root:

```powershell
.\run.ps1
```

The app data is stored locally:

- SQLite database: `data/acadex.sqlite`
- Uploaded PDFs: `uploads/`
- Cached thumbnails: `uploads/.thumbnails/`

These runtime folders are intentionally ignored by Git.

## Project Structure

```text
src/main/java/com/acadex/fx
  AcadexFxApplication.java      JavaFX entry point
  controller/                  UI orchestration and app actions
  db/                          SQLite schema and repository queries
  model/                       Plain data models
  service/                     File hashing and thumbnail generation
  view/                        JavaFX UI and upload dialog

src/main/resources/fx
  styles.css                   Material-inspired desktop styling
```

## Production Notes

- SQLite foreign keys are enabled for every connection.
- Hierarchy and PDF lookup columns are indexed for responsive filtering.
- Thumbnail rendering runs in a bounded background executor and avoids duplicate work for the same PDF.
- Local user data and generated build files are excluded from version control.
