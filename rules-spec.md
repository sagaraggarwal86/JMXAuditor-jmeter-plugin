# JAuditor Rule Specification

All 25 rules in PRD §7 order. Each entry documents detection logic, finding strings, and known false positives.

## EXTRACTOR_NO_DEFAULT

- Category: Correctness · Default severity: ERROR
- Detects: Regex / JSON / Boundary Extractor with empty or missing default value.
- Detection logic: For `RegexExtractor`, `JSONPostProcessor`, `BoundaryExtractor`, inspect the default-value property (
  `RegexExtractor.default` / `.default_empty_value`, `JSONPostProcessor.defaultValues`,
  `BoundaryExtractor.default_empty_value`). Fires when both are blank.
- Title: "Extractor missing default value"
- Description: "Extractor has no default value set. Extraction failures will silently leave the variable unset."
- Suggestion: "Set a sentinel default (e.g., NOT_FOUND) to surface extraction failures in downstream assertions."
- Known false positives: None.

## THREAD_GROUP_ZERO_DURATION

- Category: Correctness · Default severity: ERROR
- Detects: Thread Group with scheduler enabled but duration = 0 or blank.
- Detection logic: When `ThreadGroup.scheduler` is true, check `ThreadGroup.duration`. Fires if blank or `"0"`.
- Title: "Thread Group scheduler enabled with zero duration"
- Description: "Scheduler is enabled but duration is blank or zero. Test will stop immediately."
- Suggestion: "Set a positive duration, or disable the scheduler."
- Known false positives: Test plans that explicitly set duration via a property override at runtime.

## ASSERTION_SCOPE_MISMATCH

- Category: Correctness · Default severity: WARN
- Detects: Response Assertion with main-sample scope on an HTTP sampler that generates sub-samples (embedded resources).
- Detection logic: Check `Assertion.scope` on `ResponseAssertion`; if main/blank and parent `HTTPSamplerBase` has
  `image_parser = true`, fire.
- Title: "Assertion scope may miss sub-samples"
- Description: "Response Assertion uses default/main-sample scope on a sampler that generates sub-samples (embedded
  resources)."
- Suggestion: "Set assertion scope to 'Main sample and sub-samples' if sub-sample validation is required."
- Known false positives: Intentional — user only wants to assert the main HTML response.

## EXTRACTOR_NO_REFERENCE_NAME

- Category: Correctness · Default severity: ERROR
- Detects: Extractor element with empty Reference Name field.
- Detection logic: Inspect `refname` property on RegexExtractor / BoundaryExtractor and `referenceNames` on
  JSONPostProcessor.
- Title: "Extractor missing reference name"
- Description: "Extractor has no reference name. Extracted value will not be accessible as a variable."
- Suggestion: "Set a reference name matching the intended JMeter variable."
- Known false positives: None.

## PLAINTEXT_PASSWORD_IN_BODY

- Category: Security · Default severity: ERROR
- Detects: HTTP sampler body argument with a credential-ish name (`password|passwd|pwd|secret|token|apikey`) containing
  a literal (non-`${}`) value.
- Title: "Plaintext credential in request body"
- Suggestion: "Replace the literal value with a ${variable} sourced from a CSV or User Defined Variables — never commit
  credentials to .jmx."
- Known false positives: Test fixtures deliberately using throwaway credentials.

## PLAINTEXT_TOKEN_IN_HEADER

- Category: Security · Default severity: WARN
- Detects: Header Manager `Authorization` header containing a literal bearer token (not `${…}`).
- Suggestion: "Move the token to a ${variable} populated from environment or CSV."
- Known false positives: Public demo APIs with documented test tokens.

## CREDENTIALS_IN_UDV

- Category: Security · Default severity: WARN
- Detects: UDV / Arguments element with name matching `(password|secret|token|apikey|api_key)` holding a literal value.
- Suggestion: "Load the value from environment, CSV, or a vault at runtime — do not store secrets in .jmx."
- Known false positives: Variables used for test-only data.

## GUI_LISTENER_IN_LOAD_PATH

- Category: Scalability · Default severity: ERROR
- Detects: Enabled `ResultCollector` with a GUI-heavy `gui_class` (View Results Tree, View Results in Table, Graph
  Results, Summary Report, etc.).
- Suggestion: "Disable in GUI-mode runs or replace with Simple Data Writer + offline analysis."
- Known false positives: Short smoke tests where heap headroom is not a concern.

## BEANSHELL_USAGE

- Category: Scalability · Default severity: WARN
- Detects: Any element whose class name contains `beanshell`/`BeanShell`.
- Suggestion: "Replace with a JSR223 element using the Groovy language engine."
- Known false positives: None.

## SAVE_RESPONSE_DATA_ENABLED

- Category: Scalability · Default severity: WARN
- Detects: HTTP sampler with `save_response_as_md5` or equivalent save-response property enabled.
- Suggestion: "Disable response saving on the load path; enable only for diagnostic runs."
- Known false positives: Tests that intentionally capture bodies for validation.

## RETRIEVE_EMBEDDED_RESOURCES

- Category: Scalability · Default severity: WARN
- Detects: HTTP sampler with `image_parser = true` and no `embedded_url_re` filter.
- Suggestion: "Set an 'URLs must match' regex to scope embedded requests to your application domain."
- Known false positives: None.

## THREAD_COUNT_EXCESSIVE

- Category: Scalability · Default severity: WARN
- Detects: Single Thread Group with `num_threads > 1000`.
- Suggestion: "Split across multiple Thread Groups or inject from multiple engines."
- Known false positives: Tests run on well-sized injectors tuned for high thread counts.

## NO_THINK_TIMES

- Category: Realism · Default severity: WARN
- Detects: Thread Group has ≥1 Sampler in its subtree but no Timer anywhere under it.
- Suggestion: "Add a Constant Timer or Gaussian Random Timer to simulate realistic pacing."
- Known false positives: Stress tests intentionally eliminating think time.

## MISSING_RAMP_UP

- Category: Realism · Default severity: INFO
- Detects: Thread Group with `num_threads > 10` and `ramp_time = 0`.
- Suggestion: "Set ramp-up to 1-10 seconds per 100 threads for a realistic warm-up."
- Known false positives: Sustained load tests that start from a pre-warmed pool.

## MISSING_COOKIE_MANAGER

- Category: Realism · Default severity: INFO · Whole-tree
- Detects: Any HTTP sampler exists in the tree but no Cookie Manager exists anywhere.
- Suggestion: "Add an HTTP Cookie Manager at the Test Plan or Thread Group level."
- Known false positives: Stateless APIs that don't use cookies.

## HARDCODED_HOST

- Category: Maintainability · Default severity: WARN
- Detects: HTTP Sampler or Defaults with literal `HTTPSampler.domain` (no `${…}`).
- Suggestion: "Replace with ${HOST} backed by a User Defined Variable or property."
- Known false positives: Deliberate single-target tests.

## DEFAULT_SAMPLER_NAME

- Category: Maintainability · Default severity: INFO
- Detects: Sampler named with JMeter defaults ("HTTP Request", "Debug Sampler", "JSR223 Sampler", etc.).
- Suggestion: "Rename it to describe the business action (e.g., 'POST /checkout')."
- Known false positives: Quick diagnostic samplers left at default.

## DISABLED_ELEMENT_IN_TREE

- Category: Maintainability · Default severity: INFO
- Detects: `TestElement.isEnabled() == false` on any non-root element.
- Suggestion: "Remove dead branches, or add a comment explaining why it's retained."
- Known false positives: Reserved debug listeners intentionally parked.

## MISSING_TRANSACTION_CONTROLLER

- Category: Maintainability · Default severity: INFO
- Detects: Thread Group has samplers as direct children (not wrapped in a Transaction Controller).
- Suggestion: "Wrap related samplers in a Transaction Controller with a business-meaningful name."
- Known false positives: Single-action flows that don't need grouping.

## CSV_ABSOLUTE_PATH

- Category: Maintainability · Default severity: WARN
- Detects: `CSVDataSet.filename` is an absolute path (`/` or `X:`) and contains no `${…}`.
- Suggestion: "Use a path relative to the .jmx location, or parameterize via ${CSV_DIR}."
- Known false positives: None.

## JTL_EXCESSIVE_SAVE_FIELDS

- Category: Maintainability · Default severity: WARN · Whole-tree
- Detects: Test Plan has > 20 properties matching `jmeter.save.saveservice.*` set to `true`.
- Suggestion: "Disable fields not needed for analysis — keep timestamp, elapsed, label, responseCode, success,
  threadName."
- Known false positives: Detailed diagnostic runs.

## HTTP_SAMPLER_NO_ASSERTION

- Category: Observability · Default severity: WARN
- Detects: HTTP Sampler without any Assertion as a direct child or in an ancestor's children.
- Suggestion: "Add a Response Assertion on status code or response content — or rely on a parent-scope assertion."
- Known false positives: None.

## UNNAMED_TRANSACTION_CONTROLLER

- Category: Observability · Default severity: INFO
- Detects: `TransactionController` named exactly "Transaction Controller" (default).
- Suggestion: "Rename to a business-meaningful action (e.g., 'Checkout Flow')."
- Known false positives: None.

## TRANSACTION_PARENT_SAMPLE

- Category: Observability · Default severity: INFO
- Detects: `TransactionController.parent = false`.
- Suggestion: "Enable 'Generate Parent Sample' for cleaner summary metrics."
- Known false positives: Plans that deliberately keep child samples separate.

## JSR223_NO_CACHE_KEY

- Category: Observability · Default severity: WARN
- Detects: JSR223 element (class name contains "JSR223") with non-blank `script` but blank `cacheKey`.
- Suggestion: "Set a unique cacheKey (e.g., 'my_script_v1') on each JSR223 element."
- Known false positives: Trivial scripts where recompilation cost is negligible.
