# JMXAuditor

[![Release](https://img.shields.io/github/v/release/sagaraggarwal86/JMXAuditor-jmeter-plugin?label=release&sort=semver&cacheSeconds=300)](https://github.com/sagaraggarwal86/JMXAuditor-jmeter-plugin/releases/latest)
[![Maven Central](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fgithub%2Fsagaraggarwal86%2Fjmxauditor-jmeter-plugin%2Fmaven-metadata.xml&label=Maven%20Central&cacheSeconds=300)](https://central.sonatype.com/artifact/io.github.sagaraggarwal86/jmxauditor-jmeter-plugin)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

**Static analysis for JMeter scripts** — find scalability, correctness, and maintainability issues before the load test
runs.

JMXAuditor scans your `.jmx` test plan inside JMeter and surfaces findings across six quality categories: Correctness,
Security, Scalability, Realism, Maintainability, Observability. It is **read-only** — it never touches your `.jmx` file
and has zero impact on test execution.

## 1. Installation

1. Download `jmxauditor-jmeter-plugin-*.jar` from
   the [Releases page](https://github.com/sagaraggarwal86/JMXAuditor-jmeter-plugin/releases) or
   [Maven Central](https://central.sonatype.com/artifact/io.github.sagaraggarwal86/jmxauditor-jmeter-plugin).
2. Drop it into `<JMETER_HOME>/lib/ext/`:

    - **Linux / macOS**: `cp jmxauditor-jmeter-plugin-*.jar "$JMETER_HOME/lib/ext/"`
    - **Windows (PowerShell)**: `Copy-Item jmxauditor-jmeter-plugin-*.jar "$env:JMETER_HOME\lib\ext\"`
    - **Windows (cmd)**: `copy jmxauditor-jmeter-plugin-*.jar "%JMETER_HOME%\lib\ext\"`

3. Restart JMeter. The **Tools → Audit Script** menu item and toolbar button appear automatically.

Requirements: JMeter 5.6.3, Java 17+.

### Build from source

```bash
git clone https://github.com/sagaraggarwal86/JMXAuditor-jmeter-plugin.git
cd JMXAuditor-jmeter-plugin
mvn clean verify
```

The plugin JAR lands at `target/jmxauditor-jmeter-plugin-*.jar`.

## 2. Quick start

Open a `.jmx`, press **Ctrl+Shift+A** (Cmd+Shift+A on macOS), review findings, and double-click (or select and press
Enter) any finding to jump to the offending element. Filter by severity via the tabs or press **1**–**4** (All, High,
Medium, Low); click any of the six category buttons to toggle that category in or out of the view, or use **Alt+1**–
**Alt+6** from the keyboard. **F5** or **Ctrl+R** rescans; **Esc** closes the dialog.

Export:

- **Export HTML** — single self-contained report for PR attachments or archival. Inside the report, an **Export Excel**
  toolbar button writes a styled `.xlsx` with **Test Info**, **Rule Reference**, and one sheet per category with
  findings.
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

Full keyboard navigation (Tab, Arrow keys, Enter, Esc, Ctrl+R/F5). WCAG AA contrast in both light and dark L&F. Color
is never the sole signal — severity is also communicated via sort order and text. Not formally audited.

## 6. Dark mode note

JMXAuditor adapts its in-JMeter palette to the active Look & Feel. Tested against Metal and FlatLaf Dark. The HTML
report
ships with a tri-state theme toggle (auto → dark → light) — default is `auto`, which follows the reader's
`prefers-color-scheme`; state lives on `documentElement.dataset.theme`. When embedding the report in PR descriptions
or email, pick a theme explicitly to avoid reader-local variation.

## 7. Troubleshooting

- **No menu item / toolbar button appears.** Check `jmeter.log` for lines prefixed `JMXAuditor:`. Initialization errors
  are logged and JMeter continues.
- **"Open a test plan first to audit it."** Load a `.jmx` before clicking Audit.
- **"This element no longer exists. Rescan."** The tree changed between scan and click. Click Rescan.
- **Partial results banner.** Scan hit the 10 s / 10 000 node / 2000 finding cap. Narrow the tree or split the plan.

## 8. Contributing

[`mvn clean verify`](#build-from-source) is the gate: all tests must pass and the JaCoCo line-coverage threshold
(≥98% on the testable bundle) must hold. Swing UI classes and the JMeter SPI bootstrap are excluded from the gate;
everything else is in scope.

Architecture and engineering rules live in [CLAUDE.md](./CLAUDE.md). Rule specs live
in [rules-spec.md](./rules-spec.md).

## 9. License

Apache 2.0 — see [LICENSE](./LICENSE).
