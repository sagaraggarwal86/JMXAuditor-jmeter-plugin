# CLAUDE.md

You are a contributor to JAuditor, a JMeter 5.6.3 GUI-mode plugin that performs **static analysis** on `.jmx` test plans
and surfaces findings in six categories (Correctness, Security, Scalability, Realism, Maintainability, Observability).
Read-only (never modifies `.jmx`), zero runtime impact, GUI-only. Stability over novelty, correctness over features.

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
- PRD (`JAuditor-PRD-v1.0.docx`) is product source of truth; ATD (`JAuditor-ATD-v1.0.docx`) is engineering source of
  truth. On conflict, ATD wins. Both at `D:\Learning\Plugin Development\JAuditor\`.

## Workflow & Communication

- Interactive — one decision at a time. Independent tool calls (reads, greps, builds) bundled in parallel.
- Multi-file changes: present all files together, note dependency order.
- Rollback: revert to the last explicitly approved file set, then ask.
- After changes: self-check for regressions, naming consistency, rule adherence, and all enforced invariants.
- Summarize confirmed state if context grows large; suggest `/compact` proactively.
- Responses: concise — bug-fix explanation ≤10 lines; proposal ≤1 table + 3 bullets; architecture change requires a
  table. No filler, no restating the request.
- Feedback: direct, not diplomatic. Flag concrete concerns even when not asked.
- For non-trivial decisions (≥2 options with materially different risk/effort/impact), present a table and highlight
  the recommendation. Trivial choices use prose.

  | Option | Risk | Effort | Impact | Recommendation |
  |--------|------|--------|--------|----------------|

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

A task is complete only when all apply:

- `mvn clean verify` passes (tests + SpotBugs).
- No new compiler warnings or deprecation notices.
- No invariant from *Enforced invariants* violated.
- Dependency direction preserved (`ui → scan → engine → rules → model`; `engine → util`; `export → model + rules`).
- Fat JAR < 5 MB (`target/jauditor-jmeter-plugin-*.jar`).
- Scan of a 150-sampler test plan < 500 ms (hand-verified; no automated perf gate yet).
- CLAUDE.md reviewed and updated if architecture, invariants, or class responsibilities changed.
- README.md reviewed and updated if user-facing behavior changed.

## Architecture

JMeter GUI-mode plugin performing static analysis on the currently-loaded test plan. 25 rules executed over a single
iterative DFS of `JMeterTreeNode`, results rendered in a modeless dialog with HTML/JSON export. Pure observer — no
`SampleListener`, no `TestStateListener`, no `.jmx` writes, no network. CLI-mode (`-n`) inert.

**Dependency direction (strict, no cycles):** `ui.action → ui.dialog → scan → engine → rules → model`;
`engine → util`; `export.html + export.json → export.ReportAggregates → model + rules`; `rules → engine.ScanContext`.

### Class inventory

| Class                                                                                                  | Package              | Responsibility                                                                                                                                                          |
|--------------------------------------------------------------------------------------------------------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `JAuditorPlugin`                                                                                       | (root)               | Plugin identity constants. `NAME`, `ACTION_ID = "jauditor.audit"`. `version()` resolves in 3 tiers: filtered `version.properties` → JAR manifest `Implementation-Version` → `"dev"` (defends against unfiltered `${project.version}` reaching the UI when built outside Maven). |
| `Severity`, `Category`, `ScanOutcome`                                                                  | model                | Enums serialized lowercase via `@JsonValue`. `ScanOutcome.bannerMessage(int)` single source of truth for truncation banners.                                            |
| `NodePath`                                                                                             | model                | Immutable list of segments. `breadcrumb()` joins with U+203A (›).                                                                                                       |
| `Finding`                                                                                              | model                | Record. Never holds `JMeterTreeNode` or `WeakReference`. `ruleFailure(ruleId, category, path, cause)` produces INFO finding on Tier 1 exception.                        |
| `ScanResult`                                                                                           | model                | Record. Defensively copies findings + suppressedRuleIds. `navigation` map of `Finding → WeakReference<JMeterTreeNode>` is Jackson-ignored via mixin.                    |
| `ScanLimits`                                                                                           | engine               | Single source: `MAX_NODES=10_000`, `MAX_FINDINGS=500`, `MAX_SCAN_MILLIS=10_000`.                                                                                        |
| `Deadline`                                                                                             | engine               | Clock-driven wall-clock check. `expired()` compares `clock.now()` against stored deadline.                                                                              |
| `ScanStats`                                                                                            | engine               | Nodes/rules/findings counters. Mutated from scan thread only.                                                                                                           |
| `ScanContext`                                                                                          | engine               | Per-scan shared state. Owns: memo map, node→`NodePath` cache, node→(class→bool) descendant cache. Never touched concurrently — scan-thread-local.                       |
| `TreeWalker`                                                                                           | engine               | Iterative DFS via `ArrayDeque`. Checks `Thread.interrupted()`, `deadline.expired()`, node/finding caps at every node boundary. Returns `WalkResult(nodesVisited, abortReason)`. |
| `RuleEngine`                                                                                           | engine               | Partitions rules into active/suppressed, walks tree, memoizes `concrete class → matching rules` via `IdentityHashMap`. Tier 1 catch → `Finding.ruleFailure` + WARN log. |
| `Rule`                                                                                                 | rules                | Interface: `id`, `category`, `severity`, `description`, `appliesTo()`, `check(node, ctx)`. Stateless. Whole-tree rules declare `appliesTo() = Set.of(TestPlan.class)`.   |
| `AbstractRule`                                                                                         | rules                | Shared helpers: `propString`/`propBlank`/`propBool`/`propInt`, `hasJMeterVar`, `allNodes`, `make(path, title, desc, sugg)`.                                             |
| `RuleRegistry`                                                                                         | rules                | Immutable `List<Rule>` in PRD §7 order. Whole-tree rules placed first within category to populate shared memos.                                                         |
| 25 rule classes (`rules.correctness`, `security`, `scalability`, `realism`, `maintainability`, `observability`) | rules.*       | Final, package-private ctor. One rule per class. IDs UPPER_SNAKE_CASE (PRD §7). Severities fixed.                                                                      |
| `ScanWorker`                                                                                           | scan                 | `SwingWorker<ScanResult, Finding>`. Thread name `"JAuditor-Scan"`. `doInBackground` delegates to `RuleEngine.scan`. `progress` ctor param currently unused.             |
| `ScanCancellation`                                                                                     | scan                 | Reason enum wrapper (currently unused; `TreeWalker.AbortReason` is the live signal).                                                                                    |
| `JAuditorMenuCreator`                                                                                  | ui.action            | JMeter `MenuCreator` SPI. Holds static `AuditCommand` instance. Triggers `ToolbarButtonInstaller.installAsync()` once. `bootstrap()` wrapped in `try(Throwable)` — Tier 4. |
| `AuditCommand`                                                                                         | ui.action            | Extends JMeter `AbstractAction`. `doAction`: GUI guard → empty-plan dialog → toFront-or-new-`AuditDialog` → `startScan`. Holds static `currentDialog` (one-per-JMeter). |
| `ToolbarButtonInstaller`                                                                               | ui.action            | `SwingUtilities.invokeLater` install of "Audit" button. Walks `MainFrame` container tree for `JToolBar` (no reflection). Graceful fallback to menu + shortcut on failure. |
| `AuditDialog`                                                                                          | ui.dialog            | Modeless `JDialog`. Owns `DialogState`. `PropertyChangeListener` on `SwingWorker.state` (not `Timer` polling). Export via `doExport(prefix, ext, getDir, setDir, writer)` helper. Center is a `CardLayout` of `TABLE` (scroll pane, `VERTICAL_SCROLLBAR_ALWAYS`) and `EMPTY` (state-aware message from `updateEmptyState()`: idle / scanning / no-findings / filter-empties). Key bindings: `Esc` close, `F5` / `Ctrl+R` rescan, `Enter` navigate, `1`–`4` severity (All/Error/Warn/Info via `SeverityTabs.selectFilter`), `Alt+1`..`Alt+6` toggle category in `Category` enum order (via `KpiCardPanel.toggleCategory`). |
| `DialogState`                                                                                          | ui.dialog            | `IDLE`, `SCANNING`, `CANCELLING`, `DONE`. All button enablement derives from state via `HeaderBar.applyState`.                                                          |
| `HeaderBar`, `KpiCardPanel`, `SeverityTabs`, `FooterBar`, `FindingsTableModel`, `FindingsTableRenderer`, `FindingsContextMenu` | ui.dialog | Sub-components. `SeverityTabs.setFilterListener(Consumer)` and `KpiCardPanel.setSelectionListener(Consumer<Set<Category>>)` both drive `FindingsTableModel`, which composes severity × category filters in `rebuildView`; `countAll`/`countSeverity` also respect the category filter. `KpiCardPanel` cards are `JToggleButton`s (Tab-traversable, Space/Enter toggle, `AccessibleRole.TOGGLE_BUTTON`, focus ring via dashed inner border with 1-px padding compensation to prevent size jitter); `toggleCategory(Category)` is the programmatic hook driven by `Alt+1..6`. `HeaderBar` exports via a single split-button opening a `JPopupMenu` (HTML/JSON). `FindingsTableRenderer` sets `toolTipText` to the breadcrumb so long paths are readable. All explicit fonts derive from `UIManager.getFont("Label.font")` (no hard-coded pt, no `MONOSPACED`) so JMeter zoom/scale propagates. |
| `TreeNavigator`                                                                                        | ui.navigation        | Consumes `Map<Finding, WeakReference<JMeterTreeNode>>` from `ScanResult.navigation`. Parent-check validation → "This element no longer exists. Rescan." dialog.         |
| `ThemeColors`, `ThemeDetector`                                                                         | ui.theme             | UIManager-based luminance detection, two `EnumMap<Category, Color>` palettes (LIGHT/DARK). WCAG AA contrast.                                                            |
| `JAuditorSession`                                                                                      | ui.session           | Singleton. EDT-only setters (`EdtAssertions.assertEdt()`). Holds dialog geometry, export dirs, suppress-unsaved flag, `hiddenRuleIds` set, `currentFindings`.           |
| `ReportAggregates`                                                                                     | export               | Record. Single pass over findings + `RuleRegistry` → `byCategory`, `bySeverity`, `findingsByCategory`, `rules`. Shared by HTML + JSON writers.                         |
| `HtmlReportWriter`, `HtmlTemplate`, `HtmlEscaper`                                                      | export.html          | Token substitution on `report-template.html`; `{{styles}}` replaced with `report-styles.css` contents. Escaper handles `< > & " '`. Always light-themed.               |
| `JAuditorObjectMapper`, `JsonReportWriter`                                                             | export.json          | Jackson `ObjectMapper` with `ParameterNamesModule`, `NON_NULL`, `INDENT_OUTPUT`. Mixin `@JsonIgnore`s `ScanResult.navigation`. Schema 1.0 wire format.                  |
| `Clock`, `EdtAssertions`, `GuiGuard`, `JAuditorLog`                                                    | util                 | `Clock.system()`, `assertEdt()`, `GuiPackage.getInstance() == null` check, SLF4J wrapper with `"JAuditor: "` prefix and `redact() → "****"`.                            |

### Threading & lifecycle

- **EDT**: all Swing construction, mutation, event handling. All reads/writes of `JAuditorSession`.
- **`JAuditor-Scan` thread**: `RuleEngine.scan`, `TreeWalker`, `Rule.check`. Never touches Swing or `JAuditorSession`.
- **Scan lifecycle**: `IDLE → SCANNING → (CANCELLING → IDLE) | DONE`. `PropertyChangeListener` on
  `worker.state == DONE` fires `onWorkerDone()` on EDT.
- **Cancel vs timeout**: `isCancelled() == true` → outcome `CANCELLED`, findings discarded; `isCancelled() == false` +
  `ScanOutcome.TIMEOUT` → partial findings retained + banner.
- **One dialog per JMeter**: `AuditCommand.currentDialog` is static; second invocation = `toFront() + startScan()`.
- **Dialog close**: persists size/location to `JAuditorSession`; calls `clearOnDialogClose()`.

### JMeter integration

- **SPI registration**: `META-INF/services/org.apache.jmeter.gui.plugin.MenuCreator` → `JAuditorMenuCreator`.
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
- **Per-node `NodePath`**: `ScanContext.pathFor(node)` memoized in `IdentityHashMap`. Rules call `ctx.pathFor(node)`;
  `AbstractRule.pathOf` no longer exists.
- **Subtree-type queries**: `ScanContext.hasDescendantOfType(node, Class<?>)` memoized per `(node, class)`. Used by
  `NoThinkTimesRule` (2 queries per ThreadGroup). `AbstractRule.hasDescendantOfType` no longer exists.
- **Whole-tree rules**: first within each category in `RuleRegistry` to populate memo keys (`anyHttpSampler`,
  `anyCookieManager`) before per-node rules consume them.

### Exception topology (four tiers)

| Tier | Location                                 | Catch       | Action                                                                                                                |
|------|------------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------|
| 1    | `RuleEngine` per-rule loop               | `Exception` | WARN + `Finding.ruleFailure(...)` INFO appended. Scan continues.                                                      |
| 2    | `ScanWorker.doInBackground`              | (re-thrown) | `ExecutionException` surfaces in `AuditDialog.onWorkerDone` → error dialog + ERROR log.                               |
| 3    | `AuditCommand.doAction`, export handlers | `Exception` | ERROR log + "An unexpected error occurred. Check jmeter.log." dialog.                                                 |
| 4    | `JAuditorMenuCreator.bootstrap`, `ToolbarButtonInstaller.installAsync` | `Throwable` | ERROR with stack trace. Silent skip. JMeter continues normally. **Only Tier 4 catches `Throwable`.** |

### Output formats

- **HTML** (`report-template.html` + `report-styles.css`, classpath). Single self-contained file, inline CSS, vanilla
  JS for column sorting. No CDN. Always light-themed. Tokens: `{{styles}}`, `{{title}}`, `{{jmxFileName}}`,
  `{{jmxFilePath}}`, `{{jmeterVersion}}`, `{{scanTimestamp}}`, `{{pluginVersion}}`, `{{unsavedBanner}}`,
  `{{truncationBanner}}`, `{{categoryCards}}`, `{{findingsByCategory}}`, `{{ruleAppendix}}`.
- **JSON** schema 1.0. Pretty-printed, UTF-8, ISO-8601 timestamps with offset, camelCase keys, lowercase enum values,
  `NON_NULL` omission. `jmxFile` omitted when `null`. `ScanResult.navigation` ignored via Jackson mixin.
- **Default filenames**: `jauditor-report-<stem>-<yyyyMMdd-HHmmss>.html`,
  `jauditor-findings-<stem>-<yyyyMMdd-HHmmss>.json`.

### Resources

| File                                                                 | Purpose                                                 |
|----------------------------------------------------------------------|---------------------------------------------------------|
| `META-INF/services/org.apache.jmeter.gui.plugin.MenuCreator`         | JMeter SPI discovery                                    |
| `io/.../jauditor/report/report-template.html`                        | HTML report skeleton with `{{…}}` tokens                |
| `io/.../jauditor/report/report-styles.css`                           | HTML report CSS (inlined via `{{styles}}`)              |
| `io/.../jauditor/version.properties`                                 | Maven-filtered: `version`, `buildTimestamp`             |
| `spotbugs-exclude.xml`                                               | SpotBugs suppressions with rationale per pattern        |

### Testing

| Fixture                             | Status                                                                                                |
|-------------------------------------|-------------------------------------------------------------------------------------------------------|
| Tier 1 synthetic `.jmx` (25 pos + 25 neg) | **Not yet produced.** Requires `JmxTestHarness` + `JMeterUtils.setJMeterHome` + `saveservice.properties` unpack. Per-rule detection is not yet fixture-tested. |
| Tier 2 real-world `.jmx`            | **Not yet produced.**                                                                                 |
| Engine tests                        | `DeadlineTest`, `ScanContextTest` (memoize), `ModelTest` (records + banners).                         |
| Rule catalogue contract             | `RuleRegistryTest`: count = 25, IDs UPPER_SNAKE_CASE, unique, category counts 4/3/5/3/6/4, enum order. |
| Export                              | `HtmlEscaperTest`. HTML/JSON golden-file tests deferred until deterministic `ScanResult` fixture.     |

## Enforced invariants (do not violate)

1. **JSON schema is public** — `Finding` / `ScanResult` fields + `Severity` / `Category` / `ScanOutcome` JSON values
   are backward-compatible. Renames bump `schemaVersion`. `ScanResult.navigation` is always ignored in output.
2. **`.jmx` never written** — scan is strictly read via `GuiPackage.getTreeModel()`. No `File` writes outside
   user-triggered export dialogs.
3. **No test-tree elements** — no `JMeterGUIComponent`, no `SampleListener`, no `TestStateListener`, no
   `ConfigTestElement`. Plugin is invisible to the test tree.
4. **No network** — no HTTP client in dependency graph. No telemetry, no update checks.
5. **No persistent state** — `JAuditorSession` is in-memory only. No prefs files, no config files.
6. **No reflection against JMeter API** — pure typed public-API consumer.
7. **Only Tier 4 catches `Throwable`** — every other catch uses `Exception`. Plugin init is the single site allowed to
   defend against `NoClassDefFoundError` and friends.
8. **EDT discipline** — `JAuditorSession` setters assert EDT; scan work on `JAuditor-Scan` thread; UI mutation via
   `SwingUtilities.invokeLater` or direct EDT.
9. **Credentials redacted** — any log/finding field touching a credential value passes through `JAuditorLog.redact()`
   (returns `"****"`). Applies to `CREDENTIALS_IN_UDV`, `PLAINTEXT_PASSWORD_IN_BODY`, `PLAINTEXT_TOKEN_IN_HEADER`.
10. **Rule catalogue locked** — 25 rules, IDs exactly as in PRD §7. Severities fixed. No enable/disable UI; session-hide
    via right-click is the only suppression mechanism (→ `JAuditorSession.hiddenRuleIds`).
11. **Rule contract stateless** — `Rule.check` returns `List<Finding>` with no side effects. Mutable state lives only in
    `ScanContext` (per-scan) or `JAuditorSession` (per-session, EDT-only).
12. **Scan limits are hard** — `ScanLimits` is the single source for `MAX_NODES=10_000`, `MAX_FINDINGS=500`,
    `MAX_SCAN_MILLIS=10_000`. Banner strings live in `ScanOutcome.bannerMessage()`.
13. **JMeter API package paths** — `MenuCreator` is `org.apache.jmeter.gui.plugin.MenuCreator`; the SPI file name must
    match. `ApacheJMeter_core` contains both `MenuCreator` and `GuiPackage` — do not add a dependency on a bogus
    `ApacheJMeter` artifact.
14. **All runtime deps `provided`** — fat JAR contains only JAuditor classes via `maven-shade-plugin` with explicit
    include of the plugin's own artifact. No transitive bundling.
15. **`## Enforced invariants` heading is load-bearing** — reserved for future PR-review automation. Do not rename,
    split, or change its position relative to the next `##` heading.

## Self-Maintenance

- **Ownership split**: `CLAUDE.md` = rules and context for Claude. `README.md` = user-facing features, install,
  configuration. `rules-spec.md` = per-rule detection logic and message strings (PRD §15 template). Change each in its
  own lane; do not duplicate across files.
- **Auto-compact**: suggest `/compact` proactively before context becomes unwieldy.

### CLAUDE.md update rules

Trigger: session changes design, architecture, invariants, or class responsibilities.

- Review this file in the same session. Remove stale entries.

**Do not put in CLAUDE.md**:

- Implementation details that rot on refactor (method signatures, minor helper behaviors).
- Facts derivable from `git log` / `git blame` / current code.
- Ephemeral task state (in-progress work, TODOs).
- Restatement of README or `rules-spec.md` content.
- Duplicates of facts already stated elsewhere in this file.

**Final pass — every item must hold**:

- **Accuracy** — every claim matches current code; terms used consistently across sections.
- **Completeness** — every class, invariant, integration point, and lifecycle hook that affects decisions is
  documented.
- **Precision** — vague terms replaced with concrete ones (exact file paths, API package paths, token budgets).
- **Density** — every line earns its tokens; no filler, no hedging.
- **Single source of truth** — each fact lives in one section; others cross-reference.

### README update rules

Trigger: user-facing feature changes (rule catalogue summary, zero-impact claims, export behavior, keyboard shortcuts).

1. **User-benefit framing** — describe features by what they do *for the user*, not by internal mechanics.
   Architectural terms ("pure observer", "tier 4 catch", "IdentityHashMap memo") stay in CLAUDE.md.
2. **9 canonical sections per PRD §13** — Installation, Quick start, The 25 rules, Zero-impact commitment,
   Accessibility posture, Dark mode note, Troubleshooting, Contributing, License. Summary only; full rule detection
   logic lives in `rules-spec.md`.
3. **Zero duplicate facts** — each fact appears in at most one place unless there is a legitimate summary/detail split
   (README rule table → `rules-spec.md`).
4. **Cross-platform shell blocks** — any command involving paths or env vars must show Linux/macOS, Windows PowerShell,
   and Windows cmd.
5. **Link CLAUDE.md, do not duplicate** — architecture and invariants live only in CLAUDE.md. Contributing section
   links to it.
6. **Badges at top** — License, Maven Central. Prefer the
   `maven-metadata/v?metadataUrl=…repo1.maven.org…/maven-metadata.xml` variant over `maven-central/v/…` (the latter
   hits a stale Solr index).
7. **Callouts over subsections** — `> [!NOTE]` / `> [!IMPORTANT]` for 1-2 line asides.

### rules-spec.md update rules

Trigger: new rule, changed detection logic, changed finding message, or new fixture.

- Entry per rule using PRD §15 template: `## RULE_ID` → Category · Severity, Detects, Detection logic, Title,
  Description, Suggestion, Known false positives. Optional: Example fixture path (once Tier 1 fixtures exist).
- Rule ID in heading must match `Rule.id()` exactly. Severity in heading must match `Rule.severity()`.
