# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 0.2.x   | Yes       |
| < 0.2   | No        |

## Threat Surface

JMXAuditor reads the loaded `.jmx` via `GuiPackage.getTreeModel()` (GUI-mode only; CLI mode `-n`
is inert) and renders findings in a modeless dialog.

- **`.jmx` I/O** — Read-only via `GuiPackage.getTreeModel()`. No `File` writes anywhere in the plugin.
- **Test-tree integrity** — No `JMeterGUIComponent`, `SampleListener`, `TestStateListener`, or `ConfigTestElement`.
  Invisible to the test tree and to execution.
- **Network** — No HTTP client in the dependency graph; no telemetry, no update checks. The HTML report inlines its
  CSS + xlsx-js-style bundle — single self-contained file, no CDN calls at view time.
- **Persistent state** — `JMXAuditorSession` lives in JVM memory only; dialog geometry and session-hidden rule IDs
  evaporate with the process. Only on-disk artifacts are user-initiated HTML/JSON/XLSX exports.
- **Credential handling** — `PLAINTEXT_PASSWORD_IN_BODY`, `PLAINTEXT_TOKEN_IN_HEADER`, and `CREDENTIALS_IN_UDV` route
  every captured value through `JMXAuditorLog.redact()` → `"****"`. Raw values never reach logs, findings, or exports.
- **Reflection** — None against the JMeter API. The toolbar installer walks the `MainFrame` container tree for
  `JToolBar` via direct API calls.
- **Serialization** — Jackson with `NON_NULL` + `INDENT_OUTPUT`, no polymorphic types. Plugin is write-only for JSON.
  `ScanResult.navigation` is suppressed via mixin so `WeakReference<JMeterTreeNode>` never leaks.
- **Failure containment** — Only plugin init + toolbar install catch `Throwable`; every other catch site uses
  `Exception`. A rule that throws emits a `Finding.ruleFailure` INFO and the scan continues.
- **DoS bounds** — Scan caps: 10 000 nodes, 2 000 findings, 10 s wall clock. A pathological or hostile `.jmx` cannot
  exhaust memory or freeze the GUI.

## Out of Scope

- **Plaintext secrets in the source `.jmx`** — the scan flags them in findings but never alters the file. Scrub before
  committing.
- **Exported report sharing** — HTML/JSON exports carry `NodePath` breadcrumbs that reveal the element hierarchy. Review
  before publishing.
- **Local-filesystem attackers** — anyone with write access to `<JMETER_HOME>/lib/ext/` can replace the plugin JAR.
  JMXAuditor operates inside the user's filesystem trust boundary.
- **Co-resident plugins** — if another GUI-mode plugin mutates elements outside JMeter's public API contract, findings
  reflect the tampered state.

## Reporting a Vulnerability

Do **not** open a public issue. Use
GitHub's [private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
on this repository.

## Dependencies

All runtime deps are `provided` (resolved from JMeter 5.6.3's classpath): `ApacheJMeter_core`,
`_components`, `_http`, `_java`; `jackson-databind`; `slf4j-api`. Fat JAR ships only plugin classes
via `maven-shade-plugin` with an explicit artifact include — no transitive bundling. JAR-size gate
< 5 MB enforced in CI.

### Dependabot alerts on transitive JMeter dependencies

GitHub's Dependabot raises CVE alerts against the *transitive graph* of the JMeter 5.6.3 artifacts
(`ApacheJMeter_*`) — typical examples: `commons-collections`, `commons-beanutils`, the `log4j 1.2`
bridge, `httpclient` 4.x, Groovy, BeanShell. These alerts are reviewed and treated as follows:

- **Not shipped by this plugin.** All such dependencies are `provided` scope and are *excluded* from
  the published fat JAR. The plugin distribution itself contains only JMXAuditor classes.
- **Not loaded from this plugin's POM at runtime.** The user's JMeter installation supplies these
  classes from `<JMETER_HOME>/lib/`. Bumping the version in our `pom.xml` would not change what is
  actually loaded into the JVM.
- **Cannot be remediated here.** Forcing a transitive override would either be ignored at runtime
  (JMeter's bundled jars win on the classloader) or violate the `provided`-only / no-shading
  invariants documented in `CLAUDE.md`. The genuine remediation path is an upstream JMeter release.
- **Triage rule.** Alerts whose advisory affects only `provided`-scope transitives of `ApacheJMeter_*`
  are dismissed as **"Vulnerable code is not actually used"** with a link back to this section. A
  CVE in a dependency that is *direct* (e.g. `jackson-databind`, `slf4j-api`) or that the plugin's
  own code path exercises is patched normally — see PR #15 (`jackson-databind` 2.15.2 → 2.21.2).

If you believe a flagged advisory does affect a code path the plugin actually executes, please
report it via private vulnerability reporting (above) rather than dismissing the alert.
