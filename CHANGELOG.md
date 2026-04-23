# Changelog

All notable changes to JAuditor will be documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.0] — 2026-04-23

Initial release.

### Added

- 25 static-analysis rules across six categories (Correctness, Security, Scalability, Realism, Maintainability,
  Observability).
- GUI-mode plugin for JMeter 5.6.3 with three entry points: Tools menu, toolbar button, `Ctrl+Shift+A` shortcut.
- Modeless results dialog with KPI cards, severity tabs, sortable findings table, and click-to-navigate.
- HTML report export (single self-contained file, no external dependencies).
- JSON export (schema version 1.0).
- Session-only rule suppression via right-click context menu.
- Zero-impact contract: read-only scan, no network, no persistence, no `.jmx` modification.
