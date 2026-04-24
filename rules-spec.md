# JAuditor Rule Specification

Single source of truth for the 25 rules. Each entry mirrors what the rule class
returns from `id()`, `category()`, `severity()`, `appliesTo()`, `description()`,
and the `Title / Description / Suggestion` strings passed to `AbstractRule.make(...)`.
Keep this file in sync with the rule classes under
`src/main/java/io/github/sagaraggarwal86/jmeter/jauditor/rules/`.

## Related documents

- [CLAUDE.md](./CLAUDE.md) — architecture, dependency direction, enforced invariants, exception topology.
- [README.md](./README.md) — user-facing feature summary.
- Rule sources: `src/main/java/.../rules/<category>/<RuleId>Rule.java`
  (e.g., `rules.correctness.ExtractorNoDefaultRule`).

## Conventions

- **Severity** uses the `Severity` enum values — `ERROR`, `WARN`, `INFO`. They map to
  JSON `error` / `warn` / `info` (public wire format, invariant 1) and to HTML
  display labels `High` / `Medium` / `Low` (renderer-local; the enum is the source
  of truth).
- **Applies to** lists the JMeter `TestElement` subclasses each rule's `appliesTo()`
  registers. When it lists `TestElement`, the rule registers against every element
  and filters inside `check(...)` by class name.
- **Whole-tree** marks rules that register only on `TestPlan` but walk the whole
  tree via `ScanContext.memoize(...)` or `ScanContext.hasDescendantOfType(...)`.
  `RuleRegistry` places whole-tree rules first within their category so shared
  memo keys (`anyHttpSampler`, `anyCookieManager`) are populated before dependent
  per-node rules consume them.
- **Redaction** (invariant 9): `PLAINTEXT_PASSWORD_IN_BODY`, `PLAINTEXT_TOKEN_IN_HEADER`,
  and `CREDENTIALS_IN_UDV` pass credential values through `JAuditorLog.redact()`
  before inserting them into finding descriptions. The stored string is always `****`.
- **Property keys** are JMeter's internal string keys (e.g., `ThreadGroup.num_threads`),
  not the UI labels. Read via `AbstractRule.propString / propBool / propInt`.
  `propString` returns `""` (empty string, never `null`) when the property is missing.
- **`${…}` detection**: `AbstractRule.hasJMeterVar(s)` returns `true` when `s`
  contains the literal substring `${`. Values matching this are treated as
  non-literal and skipped by security / maintainability rules that would otherwise
  fire on them.
- **Description / Suggestion placeholders** like `{name}`, `{n}`, `{host}`,
  `{simpleName}`, `{path}` indicate values substituted at emission via string
  concatenation; they are not literal braces in the output.
- **Tone**: Description and Suggestion strings are written in plain English for
  performance testers and SREs, not for JMeter internals. They explain *what*
  and *why* (Description) and *how to fix* (Suggestion) at the level a
  mid-experience engineer can act on without further research.

## Rule catalogue

25 rules · 6 categories · counts **4 / 3 / 5 / 3 / 6 / 4**
(Correctness / Security / Scalability / Realism / Maintainability / Observability).
Order below is PRD §7. `RuleRegistry` execution order differs in Realism and
Maintainability (whole-tree first) — see CLAUDE.md for why.

| #  | Rule ID                                                       | Category        | Severity | Applies to                                       | Whole-tree |
|----|---------------------------------------------------------------|-----------------|----------|--------------------------------------------------|:----------:|
| 1  | [EXTRACTOR_NO_DEFAULT](#extractor_no_default)                 | Correctness     | ERROR    | RegexExtractor, JSONPostProcessor, BoundaryExtractor |        |
| 2  | [THREAD_GROUP_ZERO_DURATION](#thread_group_zero_duration)     | Correctness     | ERROR    | ThreadGroup                                      |            |
| 3  | [ASSERTION_SCOPE_MISMATCH](#assertion_scope_mismatch)         | Correctness     | WARN     | ResponseAssertion                                |            |
| 4  | [EXTRACTOR_NO_REFERENCE_NAME](#extractor_no_reference_name)   | Correctness     | ERROR    | RegexExtractor, JSONPostProcessor, BoundaryExtractor |        |
| 5  | [PLAINTEXT_PASSWORD_IN_BODY](#plaintext_password_in_body)     | Security        | ERROR    | HTTPSamplerBase                                  |            |
| 6  | [PLAINTEXT_TOKEN_IN_HEADER](#plaintext_token_in_header)       | Security        | WARN     | HeaderManager                                    |            |
| 7  | [CREDENTIALS_IN_UDV](#credentials_in_udv)                     | Security        | WARN     | Arguments                                        |            |
| 8  | [GUI_LISTENER_IN_LOAD_PATH](#gui_listener_in_load_path)       | Scalability     | ERROR    | ResultCollector                                  |            |
| 9  | [BEANSHELL_USAGE](#beanshell_usage)                           | Scalability     | WARN     | TestElement (filtered by class name)             |            |
| 10 | [SAVE_RESPONSE_DATA_ENABLED](#save_response_data_enabled)     | Scalability     | WARN     | ResultCollector                                  |            |
| 11 | [RETRIEVE_EMBEDDED_RESOURCES](#retrieve_embedded_resources)   | Scalability     | WARN     | HTTPSamplerBase                                  |            |
| 12 | [THREAD_COUNT_EXCESSIVE](#thread_count_excessive)             | Scalability     | WARN     | ThreadGroup                                      |            |
| 13 | [NO_THINK_TIMES](#no_think_times)                             | Realism         | WARN     | ThreadGroup                                      |            |
| 14 | [MISSING_RAMP_UP](#missing_ramp_up)                           | Realism         | INFO     | ThreadGroup                                      |            |
| 15 | [MISSING_COOKIE_MANAGER](#missing_cookie_manager)             | Realism         | INFO     | TestPlan                                         |     ✓      |
| 16 | [HARDCODED_HOST](#hardcoded_host)                             | Maintainability | WARN     | HTTPSamplerBase, ConfigTestElement               |            |
| 17 | [DEFAULT_SAMPLER_NAME](#default_sampler_name)                 | Maintainability | INFO     | Sampler                                          |            |
| 18 | [DISABLED_ELEMENT_IN_TREE](#disabled_element_in_tree)         | Maintainability | INFO     | TestElement (filtered by `isEnabled()`)          |            |
| 19 | [MISSING_TRANSACTION_CONTROLLER](#missing_transaction_controller) | Maintainability | INFO | ThreadGroup                                      |            |
| 20 | [CSV_ABSOLUTE_PATH](#csv_absolute_path)                       | Maintainability | WARN     | CSVDataSet                                       |            |
| 21 | [JTL_EXCESSIVE_SAVE_FIELDS](#jtl_excessive_save_fields)       | Maintainability | WARN     | TestPlan                                         |     ✓      |
| 22 | [HTTP_SAMPLER_NO_ASSERTION](#http_sampler_no_assertion)       | Observability   | WARN     | HTTPSamplerBase                                  |            |
| 23 | [UNNAMED_TRANSACTION_CONTROLLER](#unnamed_transaction_controller) | Observability | INFO   | TransactionController                            |            |
| 24 | [TRANSACTION_PARENT_SAMPLE](#transaction_parent_sample)       | Observability   | INFO     | TransactionController                            |            |
| 25 | [JSR223_NO_CACHE_KEY](#jsr223_no_cache_key)                   | Observability   | WARN     | TestElement (filtered by class name)             |            |

## Correctness

### EXTRACTOR_NO_DEFAULT

- **Category:** Correctness · **Severity:** ERROR
- **Applies to:** `RegexExtractor`, `JSONPostProcessor`, `BoundaryExtractor`
- **Detects:** Regex/JSON/Boundary Extractor with empty or missing default value.
- **Detection logic:** Primary key is the default-value text per type —
  `JSONPostProcessor.defaultValues` for `JSONPostProcessor`,
  `BoundaryExtractor.default` for `BoundaryExtractor`,
  `RegexExtractor.default` otherwise. Fires when the default text is blank **and**
  the extractor's `default_empty_value` boolean (RegexExtractor or BoundaryExtractor
  only — JSONPostProcessor has no such flag) is `false` or absent. The boolean means
  "assign empty string on no-match" and is read via `propBool`, not `propString`.
- **Title:** `Extractor missing default value`
- **Description:** `This extractor (Regex, JSON, or Boundary) has no default value configured. If the response ever doesn't match what the extractor is looking for — a different error page, a redirect, an empty body — the variable it was supposed to set just never gets assigned. Downstream samplers and assertions that rely on that variable won't fail loudly; they'll silently use a stale value from a previous iteration or an empty string, and the real bug becomes nearly impossible to spot.`
- **Suggestion:** `Fill in the Default Value field on the extractor with a sentinel string that obviously doesn't look like real data — something like NOT_FOUND or EXTRACTION_FAILED. Then add a Response Assertion a little further down that fails when the variable equals that sentinel. That way a missed extraction turns into a clear failing sample in the report instead of a silent corruption that you only notice days later when the numbers don't add up.`
- **Known false positives:** None.

### THREAD_GROUP_ZERO_DURATION

- **Category:** Correctness · **Severity:** ERROR
- **Applies to:** `ThreadGroup`
- **Detects:** Thread Group with scheduler enabled but duration = 0 or blank.
- **Detection logic:** Reads `ThreadGroup.scheduler` (boolean). Exits without firing
  when false. Otherwise reads `ThreadGroup.duration` (string); fires when the value
  is blank or equals `"0"` after trim.
- **Title:** `Thread Group scheduler enabled with zero duration`
- **Description:** `This Thread Group has its scheduler switched on but no duration filled in (the field is empty or set to 0). JMeter reads that as 'run for zero seconds' — so the moment the test starts, the scheduler tells the threads they're already out of time and they shut down before any meaningful work happens.`
- **Suggestion:** `Pick one of two fixes. If you want a time-boxed run, enter how long the test should last in the Duration field in seconds — for example, 300 for a five-minute run. If you'd rather end the test based on iterations instead of time, turn the scheduler off entirely and let the Loop Count drive when it stops. Leaving the scheduler on with no duration never does what anyone wants.`
- **Known false positives:** Test plans that override duration via a property at runtime.

### ASSERTION_SCOPE_MISMATCH

- **Category:** Correctness · **Severity:** WARN
- **Applies to:** `ResponseAssertion`
- **Detects:** Response Assertion with 'Main sample only' scope on sampler with sub-samples.
- **Detection logic:** Reads `Assertion.scope`. Exits without firing when scope is
  non-blank AND not `"parent"` (case-insensitive) — i.e. `"all"` (Main sample and
  sub-samples) already covers sub-samples, `"children"` scopes to them exclusively,
  and `"variable"` is user-defined; none of those conflict with the parent's
  `image_parser`. Otherwise (blank or `"parent"`, both meaning "Main sample only")
  walks to the parent tree node; fires when that parent is an `HTTPSamplerBase`
  with `HTTPSampler.image_parser == true`.
- **Title:** `Assertion scope may miss sub-samples`
- **Description:** `This Response Assertion is set to check only the main sample, but its parent HTTP sampler has 'Retrieve All Embedded Resources' turned on — which means every image, CSS file, and JS file the page pulls in becomes its own sub-sample. If any of those sub-samples fails (a broken image, a 404 on a stylesheet), the assertion can't see it, because it only ever looks at the main HTML response. The test reports success even when half the page didn't load.`
- **Suggestion:** `Open the Response Assertion and change the scope dropdown from 'Main sample only' (or blank, which means the same thing) to 'Main sample and sub-samples'. After the change, the assertion will evaluate the main page and every embedded resource, so a broken sub-request shows up as a test failure. If you genuinely only care about the main response — say, you're asserting HTML content and don't care about asset availability — leave the scope alone and disable this check for that sampler.`
- **Known false positives:** User intentionally asserts only the main HTML response.

### EXTRACTOR_NO_REFERENCE_NAME

- **Category:** Correctness · **Severity:** ERROR
- **Applies to:** `RegexExtractor`, `JSONPostProcessor`, `BoundaryExtractor`
- **Detects:** Extractor element with empty Reference Name field.
- **Detection logic:** Key per type — `JSONPostProcessor.referenceNames`,
  `BoundaryExtractor.refname`, or `RegexExtractor.refname`. Fires when the resolved
  value is blank.
- **Title:** `Extractor missing reference name`
- **Description:** `This extractor runs its extraction logic but has no reference name set, so whatever it pulls out of the response goes nowhere — there's no JMeter variable for later samplers, assertions, or scripts to read it from. Effectively the extractor is doing work that produces no usable output, and any downstream element that was expecting a variable will see it as undefined.`
- **Suggestion:** `Set the Reference Name field on the extractor to the variable name you want to use downstream — for example, authToken if a later sampler needs ${authToken} in its header. Pick a name that makes the value's purpose obvious at a glance, and make sure it matches exactly what the rest of the test plan references (JMeter variable names are case-sensitive).`
- **Known false positives:** None.

## Security

### PLAINTEXT_PASSWORD_IN_BODY

- **Category:** Security · **Severity:** ERROR
- **Applies to:** `HTTPSamplerBase`
- **Detects:** HTTP sampler body contains a literal credential value.
- **Detection logic:** Iterates the sampler's `Arguments` collection. For each
  argument, matches the name (trimmed) case-insensitively against regex
  `^(password|passwd|pwd|secret|token|apikey|api_key)$`. Skips the argument when
  name or value is null, when the value contains `${`, or when the value is blank.
  Finding description carries the value passed through `JAuditorLog.redact()` (invariant 9).
- **Title:** `Plaintext credential in request body`
- **Description:** `The HTTP request sends the field '{name}' with a hard-coded value. That value lives directly inside the .jmx file, so anyone who opens the test plan or checks it into version control can read the real credential. Passwords and tokens written into .jmx files are a common source of accidental leaks, especially when the file ends up in a CI log or a screenshot. Value redacted to **** — JAuditor never prints credential contents.`
- **Suggestion:** `Move the actual value out of the .jmx. Typical options: load it from a CSV file at runtime (useful when each thread needs a different credential), read it from an environment variable using ${__env(NAME)} inside a User Defined Variables block, or fetch it from a secrets manager via a JSR223 PreProcessor. Then replace the hard-coded value here with a JMeter variable reference like ${PASSWORD}, so the test plan can be shared and reviewed without exposing the real secret.`
- **Known false positives:** Test fixtures deliberately using throwaway credentials.

### PLAINTEXT_TOKEN_IN_HEADER

- **Category:** Security · **Severity:** WARN
- **Applies to:** `HeaderManager`
- **Detects:** Header Manager with Authorization header containing a literal bearer token.
- **Detection logic:** Iterates `HeaderManager.getHeaders()`. Matches name (trimmed)
  case-insensitively against `"Authorization"`. Skips when value contains `${`.
  Strips a leading case-insensitive `"bearer "` prefix; fires when the remaining
  trimmed value is non-empty. Value is redacted via `JAuditorLog.redact()` (invariant 9).
- **Title:** `Plaintext token in Authorization header`
- **Description:** `This Header Manager sends an Authorization header with a bearer token written directly into the .jmx file. Anyone who opens the test plan — teammates, reviewers, anyone with access to the source repository — can read the real token. Tokens committed into test plans have a habit of staying valid long after the author meant to rotate them, and they often end up leaking into screenshots, CI logs, or chat messages. Value redacted to **** — JAuditor never prints token contents.`
- **Suggestion:** `Take the token out of the .jmx and feed it in at runtime. The usual pattern: read an environment variable via ${__env(AUTH_TOKEN)} inside a User Defined Variables block, or load a line from a CSV file with a CSV Data Set Config element. Then change the header value here from the literal token to a variable reference like 'Bearer ${AUTH_TOKEN}'. The test runs exactly the same way, but the test plan no longer carries the secret with it.`
- **Known false positives:** Public demo APIs with documented test tokens.

### CREDENTIALS_IN_UDV

- **Category:** Security · **Severity:** WARN
- **Applies to:** `Arguments` (User Defined Variables)
- **Detects:** User Defined Variable with credential-like name containing a literal value.
- **Detection logic:** Iterates `Arguments`. Matches the variable name
  case-insensitively against the **substring** regex
  `.*(password|secret|token|apikey|api_key).*` — any name containing one of those
  tokens anywhere matches. Skips when value is blank or contains `${`. Value is
  redacted via `JAuditorLog.redact()` (invariant 9).
- **Title:** `Credential literal in User Defined Variables`
- **Description:** `The User Defined Variable '{name}' has a name that looks like a credential (password, token, secret, apikey) and holds a hard-coded value. Because User Defined Variables live inside the .jmx, this value travels with the test plan everywhere it goes — into git, into screenshots, into CI job logs. That's almost never what the author intends. Value redacted to **** — JAuditor never prints credential contents.`
- **Suggestion:** `Replace the literal value with something that resolves at runtime. Common options: ${__env(VAR_NAME)} to read from an environment variable, ${__P(prop.name)} to read from a JMeter property passed on the command line (jmeter -Jprop.name=value ...), or a CSV Data Set Config if every row needs its own credential. The variable name can stay exactly the same, so the rest of the test plan doesn't need to change — only the stored value moves out of the .jmx.`
- **Known false positives:** Variables whose names contain one of the credential
  substrings but hold harmless test-only data (e.g. `test_token_label`).

## Scalability

### GUI_LISTENER_IN_LOAD_PATH

- **Category:** Scalability · **Severity:** ERROR
- **Applies to:** `ResultCollector`
- **Detects:** GUI-heavy ResultCollector enabled on the load path (memory blow-up risk).
- **Detection logic:** Reads `TestElement.gui_class` (fully-qualified) and derives
  the simple name after the last `.`. Fires when the simple name is in the
  hard-coded set: `ViewResultsFullVisualizer`, `TableVisualizer`, `GraphVisualizer`,
  `StatVisualizer`, `SummaryReport`, `AssertionVisualizer`,
  `RespTimeGraphVisualizer`, `DistributionGraphVisualizer`. Disabled listeners
  (and any element beneath a disabled ancestor) are filtered out by
  `RuleEngine.effectivelyEnabled` before the rule runs — the rule itself does no
  enabled-check.
- **Title:** `GUI-heavy listener enabled on load path`
- **Description:** `The '{simpleName}' listener keeps every sample it sees in memory so it can render them in real time. That's fine when you're debugging a few requests, but on a sustained load test it means the heap grows linearly with the sample count. After a few hundred thousand samples, JMeter either slows to a crawl garbage-collecting or runs out of memory and crashes outright — usually at exactly the worst moment, several hours into the test.`
- **Suggestion:** `Two good fixes. For normal test runs, right-click the listener and disable it — results still go to the JTL file (if you have a Simple Data Writer present) and you can analyze them after the run. If you need a lightweight always-on writer, add a Simple Data Writer element pointing at a results.jtl file; it streams straight to disk without buffering in memory. Save the GUI-heavy listeners for quick smoke tests with a handful of samples, never for full-scale runs.`
- **Known false positives:** Short smoke tests where heap headroom isn't a concern.

### BEANSHELL_USAGE

- **Category:** Scalability · **Severity:** WARN
- **Applies to:** `TestElement` (registered against all; filtered by class name inside `check`)
- **Detects:** BeanShell Sampler/PreProcessor/PostProcessor/Assertion/Listener in use.
- **Detection logic:** Reads the element's fully-qualified class name via
  `te.getClass().getName()` and fires when the name contains the literal substring
  `beanshell` or `BeanShell`.
- **Title:** `BeanShell element in use`
- **Description:** `BeanShell is an older scripting engine that JMeter has officially deprecated. It's single-threaded internally, which means every BeanShell sampler or processor in the test plan becomes a bottleneck — threads have to queue up to execute the script one at a time, no matter how many CPU cores you have. On top of that, BeanShell is interpreted rather than compiled, so the raw per-call overhead is much higher than the modern alternatives.`
- **Suggestion:** `Swap this element for its JSR223 equivalent — JSR223 Sampler instead of BeanShell Sampler, JSR223 PreProcessor instead of BeanShell PreProcessor, and so on. In the JSR223 element, set the Language dropdown to 'groovy' (pre-installed with JMeter and much faster). The script syntax is almost identical to BeanShell, so most existing scripts copy across with minimal changes. Don't forget to set a Cache Key on each JSR223 element so Groovy compiles the script once instead of on every execution.`
- **Known false positives:** None.

### SAVE_RESPONSE_DATA_ENABLED

- **Category:** Scalability · **Severity:** WARN
- **Applies to:** `ResultCollector`
- **Detects:** Listener configured to save full response bodies into JTL output.
- **Detection logic:** Casts the element to `ResultCollector` and reads
  `getSaveConfig().saveResponseData()`; fires when that returns `true`. The flag
  lives on the listener's `SampleSaveConfiguration` (stored in the JMX as
  `<objProp name="saveConfig">` with a nested `<responseData>` boolean), not on
  any HTTP sampler property. Disabled listeners are filtered out by
  `RuleEngine.effectivelyEnabled` before the rule runs.
- **Title:** `Listener saves full response data`
- **Description:** `This listener is configured to save the full response body of every sample into its JTL output. Each response is potentially hundreds of kilobytes; on a sustained run the JTL file grows by gigabytes per minute, and JMeter buffers chunks of that in memory along the way. Disk fills up, heap pressure spikes, and the extra I/O slows the actual test down to where the reported response times aren't even representative of the system under test anymore.`
- **Suggestion:** `Turn off the 'Save Response Data (XML)' checkbox on the listener's Configure panel unless you specifically need the body for later inspection. If you only need bodies for failed requests (a reasonable debugging compromise), set the global property jmeter.save.saveservice.response_data.on_error=true in jmeter.properties — JMeter will then save bodies only when a sample fails. For full-body captures, run a targeted smoke test with a handful of iterations rather than saving every response on a 10,000-thread run.`
- **Known false positives:** Tests that intentionally capture bodies for validation.

### RETRIEVE_EMBEDDED_RESOURCES

- **Category:** Scalability · **Severity:** WARN
- **Applies to:** `HTTPSamplerBase`
- **Detects:** HTTP sampler retrieves all embedded resources without URL constraints.
- **Detection logic:** Fires when `HTTPSampler.image_parser == true` **AND**
  `HTTPSampler.embedded_url_re` is blank.
- **Title:** `Retrieve Embedded Resources without URL filter`
- **Description:** `This HTTP sampler has 'Retrieve All Embedded Resources' turned on with no URL filter. That means every image, CSS file, JavaScript file, and iframe source the response references gets fetched automatically — including resources on third-party CDNs, analytics domains, ad networks, and font providers. One main request can turn into fifty actual HTTP calls, and the extra calls pollute the metrics with latencies that have nothing to do with the system you're actually testing.`
- **Suggestion:** `Set the 'URLs must match' regex field on the sampler to a pattern that whitelists only your own domain — for example, 'https?://([^/]+\.)?example\.com/.*' if your app lives at example.com. JMeter will then skip any embedded URL that doesn't match. This keeps the test focused on your infrastructure, makes throughput calculations honest, and avoids accidentally load-testing your CDN provider or third-party tracking scripts.`
- **Known false positives:** None.

### THREAD_COUNT_EXCESSIVE

- **Category:** Scalability · **Severity:** WARN
- **Applies to:** `ThreadGroup`
- **Detects:** Single Thread Group configured with more than 1000 threads.
- **Detection logic:** Reads `ThreadGroup.num_threads` (default 0 when missing).
  Fires when the value is strictly `> 1000`.
- **Title:** `Thread Group has >1000 threads`
- **Description:** `This Thread Group is set to run {n} virtual users inside a single JVM. A single JMeter process can usually handle 500-1000 threads comfortably; past that, threads compete for CPU time and memory so heavily that they can't actually issue requests at the rate you configured. You end up measuring JMeter's own scheduling delays rather than the system under test, and the reported TPS plateaus well below what the target could actually handle.`
- **Suggestion:** `Split the load across multiple injectors. Two common approaches: run several Thread Groups of 500-1000 threads each on the same machine if CPU and memory allow (a common sizing heuristic), or distribute the test across multiple JMeter engines using distributed mode (one controller, several workers) or independent instances coordinated externally. As a rule of thumb, keep each injector's CPU below about 70% during the run — past that, JMeter tends to fall behind its own schedule.`
- **Known false positives:** Tests run on well-sized injectors tuned for high thread counts.

## Realism

### NO_THINK_TIMES

- **Category:** Realism · **Severity:** WARN
- **Applies to:** `ThreadGroup` (uses `ScanContext.hasDescendantOfType` — memoized per-node subtree query)
- **Detects:** Thread Group contains samplers but no Timer in its subtree.
- **Detection logic:** Calls `ctx.hasDescendantOfType(node, Sampler.class)` — exits
  without firing when false. Then calls `ctx.hasDescendantOfType(node, Timer.class)` —
  exits when true. Fires when the Thread Group has ≥ 1 Sampler descendant and
  zero Timer descendants.
- **Title:** `Thread Group has no think times`
- **Description:** `This Thread Group runs its requests one right after another with nothing slowing them down. Real users pause between actions — they read the page, scroll, type, decide — so a load test without pauses hits the server much faster and harder than production traffic ever would, and the response times and error rates you get back won't reflect what real users experience.`
- **Suggestion:** `Add a Timer element somewhere inside this Thread Group so JMeter pauses between requests. A Constant Timer gives every thread the same fixed delay (e.g., 2 seconds) — quick to set up. A Gaussian Random Timer varies the delay around a target average ('about 3 seconds, give or take one') — more realistic. Even a few seconds of pause per action usually makes the load shape look much closer to real traffic.`
- **Known false positives:** Stress tests intentionally eliminating think time.

### MISSING_RAMP_UP

- **Category:** Realism · **Severity:** INFO
- **Applies to:** `ThreadGroup`
- **Detects:** Thread Group with >10 threads and zero ramp-up.
- **Detection logic:** Reads `ThreadGroup.num_threads` and `ThreadGroup.ramp_time`
  (both default 0 when missing). Fires when `num_threads > 10` **AND**
  `ramp_time == 0` (i.e., `ramp_time > 0` suppresses the finding).
- **Title:** `Thread Group has no ramp-up`
- **Description:** `This Thread Group starts {n} virtual users all at exactly the same instant (ramp-up period is 0 seconds). That's a traffic spike no real system ever sees — connection pools fill in a single millisecond, caches haven't warmed up, the JIT compiler hasn't finished optimising hot paths. The first few seconds of results reflect a cold, overwhelmed system rather than steady-state behaviour, which skews every averaged metric for the rest of the run.`
- **Suggestion:** `Set the Ramp-Up Period on the Thread Group to a non-zero value so JMeter introduces the threads gradually. A good rule of thumb is one to ten seconds per 100 threads — for example, 30-60 seconds for a 1000-thread group. Even for smaller runs, a 30-second ramp-up is usually enough to let connection pools, caches, and JIT compilation reach steady state before you start averaging the measurements that matter.`
- **Known false positives:** Sustained load tests starting from a pre-warmed pool.

### MISSING_COOKIE_MANAGER

- **Category:** Realism · **Severity:** INFO · **Whole-tree**
- **Applies to:** `TestPlan` (whole-tree scan via `ScanContext.memoize`)
- **Detects:** Test plan has HTTP samplers but no HTTP Cookie Manager.
- **Detection logic:** Two memoized tree walks keyed `anyHttpSampler` and
  `anyCookieManager` — each iterates `allNodes(ctx.tree())` once across the scan.
  Exits without firing when no `HTTPSamplerBase` exists anywhere. Otherwise exits
  when any `CookieManager` exists anywhere. Fires when HTTP samplers exist and no
  Cookie Manager does.
- **Title:** `No HTTP Cookie Manager`
- **Description:** `The test plan makes HTTP requests but has no HTTP Cookie Manager anywhere in the tree. That means JMeter doesn't store cookies between requests — every sampler acts like a brand-new browser that's never been to the site before. If the application relies on session cookies for login, shopping carts, CSRF tokens, or sticky load-balancer routing, the test isn't actually exercising real user flows; it's exercising a series of unauthenticated first-visits.`
- **Suggestion:** `Add an HTTP Cookie Manager element to the test tree. Putting it directly under the Test Plan makes it apply to every Thread Group; putting it inside a specific Thread Group scopes it to that group only. The default settings (clear cookies each iteration = true, CookieManager.save.cookies = false) work for most cases — JMeter will accept, store, and replay cookies across a single thread's iterations, which is how a real browser behaves.`
- **Known false positives:** Stateless APIs that don't use cookies.

## Maintainability

### HARDCODED_HOST

- **Category:** Maintainability · **Severity:** WARN
- **Applies to:** `HTTPSamplerBase`, `ConfigTestElement` (HTTP Request Defaults)
- **Detects:** HTTP Sampler or HTTP Request Defaults with a literal hostname.
- **Detection logic:** Reads `HTTPSampler.domain`. Skips when blank or contains `${`.
  Then requires the value to match the hostname regex
  `^[a-zA-Z0-9][a-zA-Z0-9.\-]+(\.[a-zA-Z]{2,})?$`. The trailing
  `(\.[a-zA-Z]{2,})?` group is optional and the char class allows digits + dots,
  so bare IPv4 addresses (e.g. `192.168.1.1`) **do** match and fire. Values
  containing characters outside `[a-zA-Z0-9.\-]` are skipped — so IPv6 literals
  (colons), `host:port` strings, and URLs with `/` do not fire.
- **Title:** `Hard-coded hostname`
- **Description:** `The Server Name field is set to '{host}' — a literal hostname written directly into the test plan. That ties this test to one specific environment. Anyone who wants to run the same test against dev, staging, or a branch deployment has to hand-edit the .jmx, which either means maintaining multiple copies of the file (drift hazard) or remembering to change it back before committing (leakage hazard).`
- **Suggestion:** `Replace the hard-coded hostname with a variable reference like ${HOST}. Define the variable either in a User Defined Variables block at the top of the test plan (easy to change per run from the GUI), or via a JMeter property passed on the command line (jmeter -JHOST=staging.example.com ...) so the same .jmx works across every environment without modification. For a multi-environment team, command-line properties are usually cleanest — the .jmx stays identical and the environment is picked at launch time.`
- **Known false positives:** Deliberate single-target tests.

### DEFAULT_SAMPLER_NAME

- **Category:** Maintainability · **Severity:** INFO
- **Applies to:** `Sampler`
- **Detects:** Sampler keeps JMeter's default name (hard to read in reports).
- **Detection logic:** Fires when `node.getName()` (trimmed) equals one of the
  hard-coded defaults: `HTTP Request`, `Debug Sampler`, `JSR223 Sampler`,
  `JDBC Request`, `SOAP/XML-RPC Request`, `FTP Request`, `TCP Sampler`,
  `JMS Publisher`, `JMS Subscriber`, `Java Request`, `BeanShell Sampler`.
- **Title:** `Sampler uses default name`
- **Description:** `This sampler still carries JMeter's default name, '{name}'. In the results table, Aggregate Report, and every summary graph, that default name labels every measurement. When the test plan has several samplers all called 'HTTP Request' (or any other default), you can't tell which one is spiking, which one is slow, or which one is failing — the labels are indistinguishable.`
- **Suggestion:** `Rename the sampler to something that describes the business action it represents, ideally method plus endpoint or an operation name — for example, 'POST /checkout', 'GET product detail', or 'Login — fetch CSRF token'. The new name flows through automatically to every report and listener, so after the rename the metrics become immediately readable. A good test of a name: if a colleague sees it in a results table without context, can they tell what the request does?`
- **Known false positives:** Quick diagnostic samplers intentionally left at default.

### DISABLED_ELEMENT_IN_TREE

- **Category:** Maintainability · **Severity:** INFO
- **Applies to:** `TestElement` (registered against all; filtered by `isEnabled()` inside `check`)
- **Detects:** Disabled element left in the tree.
- **Detection logic:** Fires when `TestElement.isEnabled() == false`. Root node
  (where `node.getParent() == null`) is skipped so the Test Plan itself never fires.
- **Title:** `Disabled element in tree`
- **Description:** `The element '{name}' is disabled — it still exists in the test tree and gets saved into the .jmx file, but it doesn't execute during test runs. Over time, disabled elements pile up: an experiment someone tried once, a listener left over from debugging, a branch commented out 'temporarily' months ago. They confuse anyone reading the test plan later because it's hard to tell whether a disabled element is intentionally paused or forgotten junk.`
- **Suggestion:** `If the element is genuinely no longer needed, delete it — .jmx files are in version control, so if you ever want it back it's one git log away. If you're keeping it for a specific reason (a debug listener you re-enable when investigating something, an alternative flow that might come back), add a Comment on the element (right-click → Edit Comment) explaining why it's there and what it's for, so the next person understands at a glance.`
- **Known false positives:** Debug listeners intentionally parked for re-enabling.

### MISSING_TRANSACTION_CONTROLLER

- **Category:** Maintainability · **Severity:** INFO
- **Applies to:** `ThreadGroup`
- **Detects:** Thread Group has samplers not wrapped in Transaction Controllers.
- **Detection logic:** Iterates the Thread Group's **direct** children via
  `node.children()`. Fires when any direct child's test element is a `Sampler`,
  regardless of Transaction Controllers elsewhere in the subtree — the finding is
  about loose samplers, not about the absence of Transaction Controllers.
- **Title:** `Samplers outside Transaction Controllers`
- **Description:** `This Thread Group has {n} sampler(s) as direct children with no Transaction Controller grouping them. Each sampler shows up as its own row in aggregate reports, which means the per-business-action view of the test has to be reconstructed by hand. A realistic flow like 'checkout' might touch six samplers (load cart, validate promo, submit payment, confirm, etc.); without a Transaction Controller you get six separate rows instead of one 'Checkout' row with a clean end-to-end duration.`
- **Suggestion:** `Group related samplers under a Transaction Controller named for the business flow they represent — 'Checkout', 'User Login', 'Search Product'. In reports, the controller shows up as a single row with its total duration (time from the first sampler starting to the last one finishing), alongside the individual sampler rows. Turn on 'Generate Parent Sample' on the controller if you want only the grouped row in the summary; leave it off if you want both the grouped row and the individual ones.`
- **Known false positives:** Single-action flows that don't need grouping.

### CSV_ABSOLUTE_PATH

- **Category:** Maintainability · **Severity:** WARN
- **Applies to:** `CSVDataSet`
- **Detects:** CSV Data Set Config references an absolute file path.
- **Detection logic:** Reads the property key `filename` (unqualified, not
  `CSVDataSet.filename`). Skips when blank or contains `${`. Fires when the value
  starts with `/` OR when its second character is `:` (Windows drive letter).
- **Title:** `CSV Data Set uses absolute path`
- **Description:** `The CSV Data Set is configured to load data from '{path}' — an absolute file path pointing to a specific location on the machine that authored the test plan. Anyone else running the test (a teammate, a CI server, a different engineer) won't have the same directory structure, so the CSV load fails and the test either errors out immediately or silently reuses stale values, depending on how the rest of the test plan is configured.`
- **Suggestion:** `Change the filename to a path relative to the .jmx file — for example, if the CSV sits next to the test plan, just put 'data/users.csv'. JMeter resolves relative paths against the .jmx's directory, so the test becomes portable. If the CSV lives somewhere conventional but external, use a variable: set ${CSV_DIR} in a User Defined Variables block or a JMeter property, and reference it as ${CSV_DIR}/users.csv. Then each environment can point at its own data directory without editing the test plan.`
- **Known false positives:** None.

### JTL_EXCESSIVE_SAVE_FIELDS

- **Category:** Maintainability · **Severity:** WARN · **Whole-tree**
- **Applies to:** `TestPlan`
- **Detects:** Test Plan enables more than 20 jmeter.save.saveservice.* fields.
- **Detection logic:** Iterates `TestElement.propertyIterator()` on the Test Plan.
  Counts properties whose key starts with `jmeter.save.saveservice.` and whose
  `getStringValue()` parses (via `Boolean.parseBoolean`) as `true`. Threshold is
  `20` (`THRESHOLD` constant in the rule class); fires when the count is strictly `> 20`.
- **Title:** `Excessive JTL save fields enabled`
- **Description:** `This test plan has {n} jmeter.save.saveservice.* properties set to true, which tells JMeter to write that many columns into every row of the JTL results file. Every extra column adds I/O work during the test and disk space afterwards — on a long run with millions of samples, the difference between a minimal column set and everything enabled can be tens of gigabytes plus noticeably higher CPU overhead in the writer thread, which sometimes ends up slowing the test itself.`
- **Suggestion:** `Trim the save fields down to the ones you actually use for analysis. One practical minimal set is: timestamp, elapsed, label, responseCode, success, threadName — six columns that together cover throughput, error rate, per-sampler latency, and per-thread grouping. Turn the rest off by removing the corresponding jmeter.save.saveservice.* properties from the Test Plan (or setting them to false). Keep the richer set only for targeted diagnostic runs where you specifically need response times by sub-component, assertion results, or latency breakdowns.`
- **Known false positives:** Detailed diagnostic runs.

## Observability

### HTTP_SAMPLER_NO_ASSERTION

- **Category:** Observability · **Severity:** WARN
- **Applies to:** `HTTPSamplerBase`
- **Detects:** HTTP Sampler without a Response Assertion at element or parent scope.
- **Detection logic:** Inspects the direct children of the sampler via
  `node.children()` for any element that is an `Assertion`. If none found, walks
  up via `getParent()` and inspects the direct children of each ancestor up to the
  root. Fires only when **no** node in that element-and-ancestor chain has an
  `Assertion` as a direct child.
- **Title:** `HTTP Sampler has no Response Assertion`
- **Description:** `This HTTP sampler has no Response Assertion attached to it or inherited from any ancestor. JMeter's default definition of 'success' is just 'the connection completed and the HTTP status code was under 400' — so a 200 response containing an actual error page, an empty body, a captcha, or a maintenance message all count as passing samples. Error rate graphs stay green while the system under test is in fact completely broken.`
- **Suggestion:** `Add a Response Assertion as a child of the sampler, or on an ancestor (Thread Group, Transaction Controller) so it applies to multiple samplers at once. A minimal useful check asserts that the response code equals 200, or that the response text contains a string you expect on success ('Welcome', 'orderId', etc.). Even one such check makes the error rate trustworthy. For APIs, asserting on a JSON field via a JSON Assertion is usually stronger than a status-code-only check.`
- **Known false positives:** None.

### UNNAMED_TRANSACTION_CONTROLLER

- **Category:** Observability · **Severity:** INFO
- **Applies to:** `TransactionController`
- **Detects:** Transaction Controller left with its default name.
- **Detection logic:** Fires when `node.getName()` (trimmed) equals exactly
  `"Transaction Controller"`.
- **Title:** `Transaction Controller unnamed`
- **Description:** `This Transaction Controller is still named 'Transaction Controller' — the default. In aggregated results the controller appears as a row with exactly that label, and if the test has more than one such controller (which is common), every row reads 'Transaction Controller' with no way to tell them apart. The grouped metrics the controller is there to produce become unreadable.`
- **Suggestion:** `Rename the controller to describe the business flow it wraps: 'Checkout Flow', 'User Registration', 'Search And Filter'. The name shows up verbatim in every report, so pick something that reads naturally when a stakeholder skims the summary. If several controllers represent variants of the same flow (e.g., guest vs logged-in checkout), include the variant in the name so they sort together and stay distinguishable.`
- **Known false positives:** None.

### TRANSACTION_PARENT_SAMPLE

- **Category:** Observability · **Severity:** INFO
- **Applies to:** `TransactionController`
- **Detects:** Transaction Controller without 'Generate Parent Sample' enabled.
- **Detection logic:** Reads `TransactionController.parent` via `propBool`
  (missing property is treated as `false`). Fires when the resolved value is `false`.
- **Title:** `Transaction Controller not generating parent sample`
- **Description:** `This Transaction Controller has 'Generate Parent Sample' turned off. That means the controller's total duration (first sampler start to last sampler end) doesn't get its own row in reports — only the individual child samplers do. You can still see the pieces, but you can't easily answer 'how long did the full checkout take?' without manually summing child rows, and parallel samples don't add the way sequential ones do anyway.`
- **Suggestion:** `Open the Transaction Controller and check the 'Generate parent sample' box. After the change, the controller appears as a single aggregated row in summary reports, alongside the child sampler rows. If you want the total to include time spent in timers and pre-/post-processors as well, also enable 'Include duration of timer and pre-post processors in generated sample'. For most load tests, switching the parent sample on gives clean per-flow metrics without losing the detailed per-sampler view.`
- **Known false positives:** Plans that deliberately keep child samples separate.

### JSR223_NO_CACHE_KEY

- **Category:** Observability · **Severity:** WARN
- **Applies to:** `TestElement` (registered against all; filtered by class name inside `check`)
- **Detects:** JSR223 element without compilation cache key — recompiles each iteration.
- **Detection logic:** Reads the element's fully-qualified class name and exits
  without firing when it does not contain the literal substring `"JSR223"`
  (case-sensitive — JMeter's JSR223 classes use all-caps). Then reads `script`
  and `cacheKey` property keys. Exits when `script` is blank. Fires when `script`
  is non-blank AND `cacheKey` is blank.
- **Title:** `JSR223 script missing cache key`
- **Description:** `This JSR223 element has a script body but no Cache Key set. Every time the element fires (potentially thousands of times per second under load), Groovy compiles the script from scratch — a process that takes several milliseconds and allocates a lot of short-lived objects. Those milliseconds add up into real latency on top of the actual request, and the allocations pressure the garbage collector, which sometimes kicks in mid-test and creates artificial response-time spikes that look like the system under test misbehaving.`
- **Suggestion:** `Fill in the Cache Key field with any unique string — 'my_login_script_v1', 'auth_token_builder', anything consistent and distinctive. Groovy uses the key to remember its compiled version of the script, so after the first execution it reuses the cached compile instead of redoing the work. Don't copy-paste the same cache key across multiple elements (that makes the wrong script run); give every JSR223 element its own key, and change the key whenever you edit the script so the cache gets invalidated.`
- **Known false positives:** Trivial scripts where recompilation cost is negligible.
