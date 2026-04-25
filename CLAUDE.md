# CLAUDE.md

You are a contributor to JMXAuditor, a JMeter 5.6.3 GUI-mode plugin that performs **static analysis** on `.jmx` test
plans and surfaces findings in six categories (Correctness, Security, Scalability, Realism, Maintainability,
Observability). Read-only (never modifies `.jmx`), zero runtime impact, GUI-only. Stability over novelty, correctness
over features.

## Rules

**Behavioral**

- Never assume — ask if in doubt.
- Never edit code until the user confirms.
- Never expand scope beyond what was confirmed.
- Recommend alternatives only when there is a concrete risk or significant benefit.
- On conflicting requirements: flag, pause, wait for decision.
- On obstacles: fix the root cause, not the symptom. Never bypass safety checks (`--no-verify`, `git reset --hard`,
  disabling tests).
- Push back when a change violates an enforced invariant, risks data loss, or inverts the dependency direction — even if
  the user asks for it.

**Technical**

- Target JMeter 5.6.3 exclusively. Verify every API against installed 5.6.3 source JARs under
  `~/.m2/repository/org/apache/jmeter/` — never from training memory. `MenuCreator` lives at
  `org.apache.jmeter.gui.plugin.MenuCreator`, not `gui.util`.
- Java 17, Maven 3.9+. Do not change these targets.
- Do not rewrite git history.
- Decision priority: **Correctness → Security → Performance → Readability → Simplicity**.
- Before proposing changes, trace impact along the dependency direction (see Architecture).
- PRD (`JMXAuditor-PRD-v1.0.docx`) is product source of truth; ATD (`JMXAuditor-ATD-v1.0.docx`) is engineering source of
  truth. On conflict, ATD wins. Both at `D:\Learning\Plugin Development\JMXAuditor\`.

## Workflow & Communication

- Interactive — one decision at a time. Independent tool calls (reads, greps, builds) bundled in parallel.
- Multi-file changes: present all files together, note dependency order.
- Rollback: revert to the last explicitly approved file set, then ask.
- After changes: self-check for regressions, naming consistency, rule adherence, and all enforced invariants.
- Summarize confirmed state if context grows large; suggest `/compact` proactively.
- Responses: concise — bug-fix explanation ≤10 lines; proposal ≤1 table + 3 bullets; architecture change requires a
  table. No filler, no restating the request.
- Feedback: direct, not diplomatic. Flag concrete concerns even when not asked.
- For non-trivial decisions (≥2 options with materially different risk/effort/impact), present a table
  (`Option | Risk | Effort | Impact | Recommendation`) and highlight the recommendation. Trivial choices use prose.

## Environment

- JDK 17, Maven 3.9+. All runtime deps `provided` (JMeter + Jackson + SLF4J on JMeter classpath). Thin JAR, no shading
  beyond own classes.
- Test stack: JUnit Jupiter 5.10.2 + AssertJ 3.25.3. No Mockito unless genuinely required.
- Jackson 2.15.2 (matches JMeter 5.6.3's bundled version).
- Shell: bash on Windows (Unix syntax — `/dev/null`, forward slashes). `find`/`grep` via Bash tool are fork-unstable;
  use Glob/Grep tools instead. PowerShell is available for `.m2` / Windows-path inspection.
- UI changes cannot be exercised without a live JMeter runtime — say so explicitly rather than claiming success.

## Build & Coverage

```bash
mvn clean verify                          # Build + tests + SpotBugs
mvn clean package -DskipTests             # Build only
mvn test -Dtest=RuleRegistryTest          # Single test class
mvn test -Dtest=ModelTest#scanOutcomeBanner  # Single test method
mvn clean deploy -Prelease                # Release to Maven Central (GPG + central-publishing)
```

- **SpotBugs gate**: `verify` phase, threshold High, `failOnError=false`. Suppressions in `spotbugs-exclude.xml` with
  per-pattern rationale (Swing references, singleton session, redact sentinel, defensive null guards).
- **No JaCoCo coverage gate yet** — rules cannot be fixture-tested until `.jmx` fixtures exist (see Testing below).
- **JAR size gate**: < 5 MB, enforced by CI shell check. Target: ~200–500 KB.

## Definition of Done

- `mvn clean verify` passes (tests + SpotBugs); no new compiler/deprecation warnings.
- No `## Enforced invariants` violated; dependency direction preserved.
- Fat JAR < 5 MB; scan of 150-sampler plan < 500 ms (hand-verified).
- CLAUDE.md / README.md updated when their respective ownership scopes change.

## Architecture

JMeter GUI-mode plugin performing static analysis on the currently-loaded test plan. 25 rules executed over a single
iterative DFS of `JMeterTreeNode`, results rendered in a modeless dialog with HTML/JSON export. Pure observer — no
`SampleListener`, no `TestStateListener`, no `.jmx` writes, no network. CLI-mode (`-n`) inert.

**Dependency direction (strict, no cycles):** `ui.action → ui.dialog → scan → engine → rules → model`;
`engine → util`; `export.html + export.json → export.ReportAggregates → model + rules`; `rules → engine.ScanContext`.

### Class inventory

| Class                                                                                                                          | Package       | Responsibility                                                                                                                                                                                                                                         |
|--------------------------------------------------------------------------------------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `JMXAuditorPlugin`                                                                                                             | (root)        | Plugin identity constants. `version()` 3-tier fallback (filtered properties → JAR manifest → `"dev"`) defends against unfiltered `${project.version}` reaching the UI.                                                                                 |
| `Severity`, `Category`, `ScanOutcome`                                                                                          | model         | Public-facing enums. `ScanOutcome.bannerMessage` is the single source of truth for truncation banners.                                                                                                                                                 |
| `NodePath`                                                                                                                     | model         | Immutable list of segments with breadcrumb rendering.                                                                                                                                                                                                  |
| `Finding`                                                                                                                      | model         | Record. Never holds `JMeterTreeNode` or `WeakReference`. `ruleFailure` factory produces INFO findings on Tier 1 exceptions.                                                                                                                            |
| `ScanResult`                                                                                                                   | model         | Record. Defensively copies findings + suppressedRuleIds. `navigation` map of `Finding → WeakReference<JMeterTreeNode>` is Jackson-ignored via mixin.                                                                                                   |
| `ScanLimits`                                                                                                                   | engine        | Single source: `MAX_NODES=10_000`, `MAX_FINDINGS=2000`, `MAX_SCAN_MILLIS=10_000`.                                                                                                                                                                      |
| `Deadline`                                                                                                                     | engine        | Clock-driven wall-clock expiration check. Uses injected `Clock` for testability.                                                                                                                                                                       |
| `ScanStats`                                                                                                                    | engine        | Nodes/rules/findings counters. Mutated from scan thread only.                                                                                                                                                                                          |
| `ScanContext`                                                                                                                  | engine        | Per-scan shared state. Owns memo map, node→`NodePath` cache, node→(class→bool) descendant cache. Scan-thread-local.                                                                                                                                    |
| `TreeWalker`                                                                                                                   | engine        | Iterative DFS via `ArrayDeque`. Checks interrupt, deadline, and node/finding caps at every node boundary. Returns `WalkResult` with an `AbortReason`.                                                                                                  |
| `RuleEngine`                                                                                                                   | engine        | Partitions rules into active/suppressed, walks tree, memoizes `concrete class → matching rules` via `IdentityHashMap`. Tier 1 catch → `Finding.ruleFailure` + WARN log.                                                                                |
| `Rule`                                                                                                                         | rules         | Stateless interface: `id`, `category`, `severity`, `description`, `appliesTo()`, `check(node, ctx)`. Whole-tree rules declare `appliesTo() = Set.of(TestPlan.class)`.                                                                                  |
| `AbstractRule`                                                                                                                 | rules         | Shared helpers for property lookups, JMeter variable detection, tree traversal, and finding construction.                                                                                                                                              |
| `RuleRegistry`                                                                                                                 | rules         | Immutable `List<Rule>` in PRD §7 order. Whole-tree rules placed first within category to populate shared memos.                                                                                                                                        |
| 25 rule classes                                                                                                                | rules.*       | Final, package-private ctor. One rule per class. IDs UPPER_SNAKE_CASE (PRD §7). Severities fixed.                                                                                                                                                      |
| `ScanWorker`                                                                                                                   | scan          | `SwingWorker<ScanResult, Finding>` named `"JMXAuditor-Scan"`. Delegates to `RuleEngine.scan`.                                                                                                                                                          |
| `JMXAuditorMenuCreator`                                                                                                        | ui.action     | JMeter `MenuCreator` SPI. Holds static `AuditCommand` instance. Triggers `ToolbarButtonInstaller.installAsync()` once. `bootstrap()` is Tier 4.                                                                                                        |
| `AuditCommand`                                                                                                                 | ui.action     | Extends JMeter `AbstractAction`. `doAction`: GUI guard → empty-plan dialog → toFront-or-new-`AuditDialog` → `startScan`. Holds static `currentDialog` (one-per-JMeter).                                                                                |
| `ToolbarButtonInstaller`                                                                                                       | ui.action     | EDT-async install of "Audit" button. Walks `MainFrame` container tree for `JToolBar` (no reflection). Graceful fallback to menu + shortcut on failure.                                                                                                 |
| `AuditDialog`                                                                                                                  | ui.dialog     | Modeless `JDialog`. Owns `DialogState`. `PropertyChangeListener` on `SwingWorker.state` (not `Timer` polling). `CardLayout` with TABLE / EMPTY cards.                                                                                                  |
| `DialogState`                                                                                                                  | ui.dialog     | `IDLE`, `SCANNING`, `CANCELLING`, `DONE`. All button enablement derives from state via `HeaderBar.applyState`.                                                                                                                                         |
| `HeaderBar`, `KpiCardPanel`, `SeverityTabs`, `FooterBar`, `FindingsTableModel`, `FindingsTableRenderer`, `FindingsContextMenu` | ui.dialog     | Sub-components. `FindingsTableModel` composes severity × category filters (counts respect category filter). `SeverityTabs` labels are `All / High / Medium / Low` — UI display only; `Severity` enum + JSON stays `error / warn / info` (invariant 1). |
| `TreeNavigator`                                                                                                                | ui.navigation | Consumes `Map<Finding, WeakReference<JMeterTreeNode>>` from `ScanResult.navigation`. Parent-check validation → "no longer exists" dialog.                                                                                                              |
| `ThemeColors`, `ThemeDetector`                                                                                                 | ui.theme      | UIManager-based luminance detection, two `EnumMap<Category, Color>` palettes (LIGHT/DARK). WCAG AA contrast.                                                                                                                                           |
| `JMXAuditorSession`                                                                                                            | ui.session    | Singleton. EDT-only setters (`EdtAssertions.assertEdt()`). Holds dialog geometry, export dirs, suppress-unsaved flag, `hiddenRuleIds`, `currentFindings`.                                                                                              |
| `ReportAggregates`                                                                                                             | export        | Record. Single pass over findings + `RuleRegistry` → `byCategory`, `bySeverity`, `findingsByCategory`, `rules`. Shared by HTML + JSON writers.                                                                                                         |
| `HtmlReportWriter`, `HtmlTemplate`, `HtmlEscaper`                                                                              | export.html   | Token + sentinel substitution on `report-template.html`. Findings sharing a rule id collapse into `grp-head` + hidden `grp-member` rows when count ≥ `HtmlReportWriter.GROUP_THRESHOLD`.                                                               |
| `JMXAuditorObjectMapper`, `JsonReportWriter`                                                                                   | export.json   | Jackson `ObjectMapper` with `NON_NULL`, `INDENT_OUTPUT`. Mixin `@JsonIgnore`s `ScanResult.navigation`. Schema 1.0 wire format. Write-only.                                                                                                             |
| `Clock`, `EdtAssertions`, `GuiGuard`, `JMXAuditorLog`                                                                          | util          | Injected-time clock, EDT assertion helper, null-`GuiPackage` guard, SLF4J wrapper with `"JMXAuditor: "` prefix and `redact()` sentinel.                                                                                                                |

### Threading & lifecycle

- **EDT**: all Swing construction, mutation, event handling. All reads/writes of `JMXAuditorSession`.
- **`JMXAuditor-Scan` thread**: `RuleEngine.scan`, `TreeWalker`, `Rule.check`. Never touches Swing or
  `JMXAuditorSession`.
- **Scan lifecycle**: `IDLE → SCANNING → (CANCELLING → IDLE) | DONE`. `PropertyChangeListener` on
  `worker.state == DONE` fires `onWorkerDone()` on EDT.
- **Cancel vs timeout**: `isCancelled() == true` → outcome `CANCELLED`, findings discarded; `isCancelled() == false` +
  `ScanOutcome.TIMEOUT` → partial findings retained + banner.
- **One dialog per JMeter**: `AuditCommand.currentDialog` is static; second invocation = `toFront() + startScan()`.
- **Dialog close**: persists size/location to `JMXAuditorSession`; calls `clearOnDialogClose()`.

### JMeter integration

- **SPI registration**: `META-INF/services/org.apache.jmeter.gui.plugin.MenuCreator` → `JMXAuditorMenuCreator`.
  Discovered by JMeter's ServiceLoader at GUI startup.
- **Menu item**: `Tools → Audit Script` (Ctrl+Shift+A / Cmd+Shift+A via `Toolkit.getMenuShortcutKeyMaskEx()`).
- **Toolbar button**: "Audit" button appended to `MainFrame`'s `JToolBar` (located by walking container tree; no
  reflection, no `getToolBar()` API call).
- **Action dispatch**: `AuditCommand.doAction(ActionEvent)` is the single entry point. All three triggers call it
  directly (no `ActionRouter` registration).

### Scan engine

- **DFS**: `TreeWalker.walk` iterative via `ArrayDeque`. Checks abort conditions at every node boundary: `interrupted`,
  `deadline.expired`, `stats.nodesVisited >= MAX_NODES`, `stats.findingsEmitted >= MAX_FINDINGS`.
- **Rule dispatch**: `RuleEngine` caches `Map<Class<?>, List<Rule>>` keyed by concrete `TestElement` class — each class
  pays the `isAssignableFrom` cost once per scan, not once per node.
- **Per-node `NodePath`**: `ScanContext.pathFor(node)` memoized in `IdentityHashMap`. Rules call `ctx.pathFor(node)`.
- **Subtree-type queries**: `ScanContext.hasDescendantOfType(node, Class<?>)` memoized per `(node, class)`. Used by
  `NoThinkTimesRule` (2 queries per ThreadGroup).
- **Whole-tree rules**: first within each category in `RuleRegistry` to populate memo keys (`anyHttpSampler`,
  `anyCookieManager`) before per-node rules consume them.

### Exception topology (four tiers)

| Tier | Location                                                                 | Catch       | Action                                                                                               |
|------|--------------------------------------------------------------------------|-------------|------------------------------------------------------------------------------------------------------|
| 1    | `RuleEngine` per-rule loop                                               | `Exception` | WARN + `Finding.ruleFailure(...)` INFO appended. Scan continues.                                     |
| 2    | `ScanWorker.doInBackground`                                              | (re-thrown) | `ExecutionException` surfaces in `AuditDialog.onWorkerDone` → error dialog + ERROR log.              |
| 3    | `AuditCommand.doAction`, export handlers                                 | `Exception` | ERROR log + "An unexpected error occurred. Check jmeter.log." dialog.                                |
| 4    | `JMXAuditorMenuCreator.bootstrap`, `ToolbarButtonInstaller.installAsync` | `Throwable` | ERROR with stack trace. Silent skip. JMeter continues normally. **Only Tier 4 catches `Throwable`.** |

### Output formats

- **HTML** — single self-contained file built from `report-template.html` + `report-styles.css` + bundled
  `xlsx-style.bundle.js` (no CDN). Sentinel tokens `/*__STYLES__*/` and `/*__XLSX__*/` carry the inlined CSS / JS;
  writer tokens use the standard `{{name}}` form. Header exposes Excel export (xlsx-js-style, one sheet per panel) and
  a Dark Mode toggle. Findings sharing a rule id within a panel collapse when count ≥
  `HtmlReportWriter.GROUP_THRESHOLD`.
  Severity is rendered as High / Medium / Low; CSS class + JSON value stay `error / warn / info` (invariant 1).
- **JSON** schema 1.0. Pretty-printed, UTF-8, ISO-8601 timestamps with offset, camelCase keys, lowercase enum values,
  `NON_NULL` omission. `ScanResult.navigation` ignored via Jackson mixin.
- **Default filenames**: `jmxauditor-report-<yyyyMMddHHmmss>.{html,json,xlsx}`. HTML/JSON via `AuditDialog.FILE_TS`;
  xlsx built client-side from `Date.toISOString()` stripped to the same 14-digit shape.

### Resources

| File                                                         | Purpose                                           |
|--------------------------------------------------------------|---------------------------------------------------|
| `META-INF/services/org.apache.jmeter.gui.plugin.MenuCreator` | JMeter SPI discovery                              |
| `io/.../jmxauditor/report/report-template.html`              | HTML report skeleton with `{{…}}` tokens          |
| `io/.../jmxauditor/report/report-styles.css`                 | HTML report CSS (inlined via `/*__STYLES__*/`)    |
| `io/.../jmxauditor/report/xlsx-style.bundle.js`              | xlsx-js-style bundle (inlined via `/*__XLSX__*/`) |
| `io/.../jmxauditor/version.properties`                       | Maven-filtered: `version`, `buildTimestamp`       |
| `spotbugs-exclude.xml`                                       | SpotBugs suppressions with rationale per pattern  |

### Testing

| Fixture                                   | Status                                                                                                                                                         |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Tier 1 synthetic `.jmx` (25 pos + 25 neg) | **Not yet produced.** Requires `JmxTestHarness` + `JMeterUtils.setJMeterHome` + `saveservice.properties` unpack. Per-rule detection is not yet fixture-tested. |
| Tier 2 real-world `.jmx`                  | **Not yet produced.**                                                                                                                                          |
| Engine tests                              | `DeadlineTest`, `ScanContextTest` (memoize), `ModelTest` (records + banners).                                                                                  |
| Rule catalogue contract                   | `RuleRegistryTest`: count = 25, IDs UPPER_SNAKE_CASE, unique, category counts 4/3/5/3/6/4, enum order.                                                         |
| Export                                    | `HtmlEscaperTest`. HTML/JSON golden-file tests deferred until deterministic `ScanResult` fixture.                                                              |

## Enforced invariants (do not violate)

1. **JSON schema is public** — `Finding` / `ScanResult` fields + `Severity` / `Category` / `ScanOutcome` JSON values
   are backward-compatible. Renames bump `schemaVersion`. `ScanResult.navigation` is always ignored in output.
2. **`.jmx` never written** — scan is strictly read via `GuiPackage.getTreeModel()`. No `File` writes outside
   user-triggered export dialogs.
3. **No test-tree elements** — no `JMeterGUIComponent`, no `SampleListener`, no `TestStateListener`, no
   `ConfigTestElement`. Plugin is invisible to the test tree.
4. **No network** — no HTTP client in dependency graph. No telemetry, no update checks.
5. **No persistent state** — `JMXAuditorSession` is in-memory only. No prefs files, no config files.
6. **No reflection against JMeter API** — pure typed public-API consumer.
7. **Only Tier 4 catches `Throwable`** — every other catch uses `Exception`. Plugin init is the single site allowed to
   defend against `NoClassDefFoundError` and friends.
8. **EDT discipline** — `JMXAuditorSession` setters assert EDT; scan work on `JMXAuditor-Scan` thread; UI mutation via
   `SwingUtilities.invokeLater` or direct EDT.
9. **Credentials redacted** — any log/finding field touching a credential value passes through `JMXAuditorLog.redact()`
   (returns `"****"`). Applies to `CREDENTIALS_IN_UDV`, `PLAINTEXT_PASSWORD_IN_BODY`, `PLAINTEXT_TOKEN_IN_HEADER`.
10. **Rule catalogue locked** — 25 rules, IDs exactly as in PRD §7. Severities fixed. No enable/disable UI; session-hide
    via right-click is the only suppression mechanism (→ `JMXAuditorSession.hiddenRuleIds`).
11. **Rule contract stateless** — `Rule.check` returns `List<Finding>` with no side effects. Mutable state lives only in
    `ScanContext` (per-scan) or `JMXAuditorSession` (per-session, EDT-only).
12. **Scan limits are hard** — `ScanLimits` is the single source for `MAX_NODES=10_000`, `MAX_FINDINGS=2000`,
    `MAX_SCAN_MILLIS=10_000`. Banner strings live in `ScanOutcome.bannerMessage()`.
13. **JMeter API package paths** — `MenuCreator` is `org.apache.jmeter.gui.plugin.MenuCreator`; the SPI file name must
    match. `ApacheJMeter_core` contains both `MenuCreator` and `GuiPackage` — do not add a dependency on a bogus
    `ApacheJMeter` artifact.
14. **All runtime deps `provided`** — fat JAR contains only JMXAuditor classes via `maven-shade-plugin` with explicit
    include of the plugin's own artifact. No transitive bundling.
15. **`## Enforced invariants` heading is load-bearing** — extracted verbatim by `.github/workflows/pr-review.yml`. Do
    not rename, split, or change its position relative to the next `##` heading.

## Self-Maintenance

- **Ownership split**: `CLAUDE.md` = rules and context for Claude. `README.md` = user-facing features, install,
  configuration. `rules-spec.md` = per-rule detection logic and message strings (PRD §15 template). Change each in its
  own lane; do not duplicate across files.
- **Triggers**: update CLAUDE.md when design / architecture / invariants / class responsibilities change; update
  README.md on user-facing changes; update rules-spec.md on new rule, changed detection logic, or changed message.
- **Auto-compact**: suggest `/compact` proactively before context becomes unwieldy.

**Do not put in CLAUDE.md**:

- Implementation details that rot on refactor (method signatures, minor helper behaviors).
- Facts derivable from `git log` / `git blame` / current code.
- Ephemeral task state (in-progress work, TODOs).
- Restatement of README or `rules-spec.md` content.
- Duplicates of facts already stated elsewhere in this file.
