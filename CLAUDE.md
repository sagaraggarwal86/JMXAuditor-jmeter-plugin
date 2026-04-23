# CLAUDE.md

You are a contributor to a JMeter plugin that audits `.jmx` test plans for correctness, best-practice violations, and
risk signals. Static analysis over JMX — does not execute the plan. Stability over novelty, correctness over features.

> **Skeleton phase.** Architecture, class inventory, and enforced invariants sections are intentionally empty. Fill them
> in as code lands — do not invent entries ahead of the code.

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

- Target JMeter 5.6.3 exclusively. Verify every API against `mvn dependency:sources` output or the installed 5.6.3
  source JARs under `~/.m2/repository/org/apache/jmeter/` — never from training memory.
- Do not change JDK/Maven targets (see Environment). Do not rewrite git history.
- Decision priority: **Correctness → Security → Performance → Readability → Simplicity**.
- Before proposing changes, trace impact along the dependency direction (see Architecture).

## Workflow & Communication

- Interactive — present choices one at a time unless trivial and clearly scoped.
- Multi-file changes: present all files together, note dependency order.
- Rollback: revert to the last explicitly approved file set, then ask.
- After changes: self-check for regressions, naming consistency, rule adherence, and all enforced invariants.
- Summarize confirmed state if context grows large; suggest `/compact` proactively.
- Responses: concise — bug-fix explanation ≤10 lines; proposal ≤1 table + 3 bullets; architecture change requires a
  table. No filler, no restating the request.
- Feedback: direct, not diplomatic. Flag concrete concerns even when not asked.
- For non-trivial decisions (≥2 options with materially different risk/effort/impact), present a table and highlight the
  recommendation. Trivial choices use prose.

  | Option | Risk | Effort | Impact | Recommendation |
        |--------|------|--------|--------|----------------|

## Environment

- JDK 17, Maven 3.8+. All JMeter runtime deps `provided` (on the JMeter classpath). Thin JAR, no shading.
- Test stack: JUnit 5 + Mockito (versions TBD at `pom.xml` creation).
- Shell: bash on Windows (Unix syntax — `/dev/null`, forward slashes). `find`/`grep` via Bash tool are fork-unstable on
  this machine; use Glob/Grep tools instead. User runs builds manually.
- UI changes cannot be exercised without a live JMeter runtime — say so explicitly rather than claiming success.

## Build & Coverage

```bash
mvn clean verify                       # Build + tests + coverage gate
mvn clean package -DskipTests          # Build only
mvn test -Dtest=ClassName              # Single test class
mvn test -Dtest=ClassName#method       # Single test method
```

- Coverage gate: **TBD** — set when JaCoCo is wired into `pom.xml`.
- Excluded classes / profiles: **TBD**.

## Definition of Done

A task is complete only when all apply:

- `mvn clean verify` passes (tests + coverage gate, once configured).
- No new compiler warnings or deprecation notices.
- No invariant from *Enforced invariants* violated.
- Dependency direction preserved (see Architecture once defined).
- CLAUDE.md reviewed and updated if architecture, invariants, or class responsibilities changed.
- README.md reviewed and updated if user-facing behaviour changed.

## Architecture

*To be populated as the skeleton is built out. Record here: dependency direction, package layout, and a class inventory
table (class → package → responsibility). Do not add speculative entries.*

## Enforced invariants (do not violate)

*To be populated as constraints are identified. Each invariant must be enforceable by code review or automated check.
Numbered list, stable numbering (do not renumber on insertion — append).*

> **Note:** The `## Enforced invariants` heading is load-bearing for any future PR-review automation. Do not rename,
> split, or change its position relative to the next `##` heading.

## Self-Maintenance

- **Ownership split**: `CLAUDE.md` = rules + context for Claude. `README.md` = user-facing features, install, config.
  Change each in its own lane; do not duplicate.
- **Auto-compact**: suggest `/compact` before context becomes unwieldy.

### CLAUDE.md update rules

Trigger: session changes design, architecture, invariants, or class responsibilities.

- Review this file in the same session. Remove stale entries, dedupe, confirm every line is actionable.

**Do not put in CLAUDE.md**:

- Implementation details that rot on refactor (method signatures, minor helper behaviors).
- Facts derivable from `git log` / `git blame` / current code.
- Ephemeral task state (in-progress work, TODOs).
- Restatement of README content (user-facing features, install steps).
- Duplicates of facts already stated elsewhere in this file.

**Final pass — every item must hold**:

- Accuracy: every claim matches current code.
- Reference resolution: every class/file/field/invariant# named exists and is spelled correctly.
- Coverage: every invariant or constraint enforced by code is represented here.
- Anti-list compliance: every line passes "Do not put in CLAUDE.md".
- Token economy: every word earns its place.
- Redundancy: no fact stated more than once.

### README update rules

Trigger: user-facing feature changes (CLI options, config keys, audit rules, report format).

Every README edit must satisfy:

1. **User-benefit framing** — describe features by what they do *for the user*, not by internal mechanics.
2. **Zero duplicate facts** — each fact appears in at most one place unless there is a legitimate summary/detail split.
3. **Cross-platform shell blocks** — any command involving paths or env vars must show Linux/macOS, Windows PowerShell,
   and Windows cmd.
4. **Badges at top** — Release, Maven Central, License. Use the
   `maven-metadata/v?metadataUrl=…repo1.maven.org…/maven-metadata.xml` variant, **not** `maven-central/v/…` (the latter
   hits a stale Solr index).
5. **Self-updating references over literal versions** — prefer shields.io badges to hardcoded version strings.
6. **Callouts over subsections** — use `> [!NOTE]` / `> [!IMPORTANT]` for 1-2 line asides.
7. **Contributing = contributor commands only**. Release commands belong in the release workflow, not README.
