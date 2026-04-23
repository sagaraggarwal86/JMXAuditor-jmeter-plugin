# JAuditor

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Fsagaraggarwal86%2Fjauditor-jmeter-plugin%2Fmaven-metadata.xml)](https://central.sonatype.com/artifact/io.github.sagaraggarwal86/jauditor-jmeter-plugin)

**Static analysis for JMeter scripts** — find scalability, correctness, and maintainability issues before the load test
runs.

JAuditor scans your `.jmx` test plan inside JMeter and surfaces findings across six quality categories: Correctness,
Security, Scalability, Realism, Maintainability, Observability. It is **read-only** — it never touches your `.jmx` file
and has zero impact on test execution.

## 1. Installation

1. Download `jauditor-jmeter-plugin-0.1.0.jar` from
   the [Releases page](https://github.com/sagaraggarwal86/JAuditor-jmeter-plugin/releases).
2. Drop it into `<JMETER_HOME>/lib/ext/`.
3. Restart JMeter. The **Tools → Audit Script** menu item and toolbar button appear automatically.

Requirements: JMeter 5.6.3, Java 17+.

## 2. Quick start

Open a `.jmx`, press **Ctrl+Shift+A** (Cmd+Shift+A on macOS), review findings, and double-click any finding to jump to
the offending element.

Export:

- **Export HTML** — single self-contained report for PR attachments or archival.
- **Export JSON** — machine-readable findings (schema v1.0) for downstream tools.

## 3. The 25 rules

| Category        | # | Example rule IDs                                      |
|-----------------|---|-------------------------------------------------------|
| Correctness     | 4 | `EXTRACTOR_NO_DEFAULT`, `THREAD_GROUP_ZERO_DURATION`  |
| Security        | 3 | `PLAINTEXT_PASSWORD_IN_BODY`, `CREDENTIALS_IN_UDV`    |
| Scalability     | 5 | `GUI_LISTENER_IN_LOAD_PATH`, `THREAD_COUNT_EXCESSIVE` |
| Realism         | 3 | `NO_THINK_TIMES`, `MISSING_RAMP_UP`                   |
| Maintainability | 6 | `HARDCODED_HOST`, `DISABLED_ELEMENT_IN_TREE`          |
| Observability   | 4 | `HTTP_SAMPLER_NO_ASSERTION`, `JSR223_NO_CACHE_KEY`    |

Full detection logic, messages, and suggestions are in [rules-spec.md](./rules-spec.md).

## 4. Zero-impact commitment

- **Never writes** to your `.jmx` file.
- **Never adds** elements to the test tree.
- **Never makes** network calls — no telemetry, no update checks.
- **Never persists** state — preferences are session-only.
- **Never runs** during load tests — GUI-mode only; inert in CLI (`-n`) mode.

Measured budgets: JMeter startup delta < 200 ms · idle memory after scan < 20 MB · scan of a 150-sampler plan < 500 ms ·
fat JAR < 5 MB.

## 5. Accessibility posture

Level B + deliberate text. Full keyboard navigation (Tab, Arrow keys, Enter, Esc, Ctrl+R/F5). WCAG AA contrast in both
light and dark L&F. Color is never the sole signal — severity is also communicated via sort order and text. Not formally
audited.

## 6. Dark mode note

JAuditor adapts its palette to the active JMeter Look & Feel. Tested against Metal and FlatLaf Dark. The HTML report is
always light-themed — it's produced for PRs, email, print.

## 7. Troubleshooting

- **No menu item / toolbar button appears.** Check `jmeter.log` for lines prefixed `JAuditor:`. Initialization errors
  are logged and JMeter continues.
- **"Open a test plan first to audit it."** Load a `.jmx` before clicking Audit.
- **"This element no longer exists. Rescan."** The tree changed between scan and click. Click Rescan.
- **Partial results banner.** Scan hit the 10 s / 10 000 node / 500 finding cap. Narrow the tree or split the plan.

## 8. Contributing

```bash
mvn clean verify
```

Architecture and engineering rules live in [CLAUDE.md](./CLAUDE.md). Rule specs live
in [rules-spec.md](./rules-spec.md).

## 9. License

Apache 2.0 — see [LICENSE](./LICENSE).
