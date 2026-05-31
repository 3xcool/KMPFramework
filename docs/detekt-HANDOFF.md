# Detekt — handoff for local verification

Branch: `feature/detekt-quality` (off `develop`). Work item: roadmap → Phase 1.5 → Lint.

The detekt setup is **authored but NOT compile-verified** — Gradle could not run in
the Cowork sandbox (no network to download the distribution). Run the steps below
in local Claude Code (or your terminal); the `detekt-fixer` and `build-verifier`
agents are made for this.

## What was added
- `gradle/libs.versions.toml` — `detekt = "1.23.8"`, `detekt-gradlePlugin`,
  `detekt-formatting`, plugin aliases `detekt` + `convention-detekt`.
- `build-logic/convention/build.gradle.kts` — `compileOnly(libs.detekt.gradlePlugin)`
  + `register("detekt")` → `DetektConventionPlugin`.
- `build-logic/.../DetektConventionPlugin.kt` — applies detekt, shared config,
  per-module `detekt-baseline.xml`, `detekt-formatting`, KMP source dirs, HTML/SARIF reports.
- `config/detekt/detekt.yml` — shared rules (buildUponDefaultConfig = true).
- Applied in `KmpLibraryConventionPlugin` + `CmpLibraryConventionPlugin` (covers all modules).
- `.github/workflows/pr_quality.yml` — detekt step before compile.

## Run / verify (in order)
```sh
# 1. Does build-logic compile with the new plugin on the classpath?
./gradlew :build-logic:convention:compileKotlin

# 2. Does the detekt task resolve across modules?
./gradlew detekt --dry-run

# 3. First real run (will report existing violations)
./gradlew detekt

# 4. Grandfather existing violations into per-module baselines
./gradlew detektBaseline

# 5. Re-run — should now be GREEN (only new issues fail)
./gradlew detekt
```

## Known risks to check
1. **Kotlin 2.2 compatibility.** detekt 1.23.8 embeds an older Kotlin compiler. If
   `detekt` errors on Kotlin 2.2 syntax, bump to the newest detekt
   (check the latest release) and re-run. This is the single most likely failure.
2. **`detektBaseline` task name** is provided per-module by the detekt plugin; if
   the aggregated `detektBaseline` doesn't exist, run per module
   (e.g. `:framework:core:presentation:detektBaseline`).
3. **KMP source sets.** The convention plugin points detekt at `src/`. If common/
   platform sources are missed, adjust `setSource(...)` in `DetektConventionPlugin`.
4. **`maxIssues: 0`** with baselines means new violations fail the build — intended.

## After it's green
- Commit the generated `detekt-baseline.xml` files (one per module).
- Have `detekt-fixer` clean up anything trivial you'd rather fix than baseline.
- Open the PR to `develop` via the `git-pr` skill; confirm the PR Quality check passes.
- Delete this handoff file in the same PR.
