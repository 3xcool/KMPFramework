# Tekmoon KMP Framework

Kotlin Multiplatform framework targeting Android, iOS, and Desktop (JVM).
The idea is to create useful resources that will be used in other apps, serving as libraries.

## Architecture

- **Kompass**: Custom Redux-pattern navigation library (`framework/kompass/`)
- **Design System**: Token-based theming with `DsTheme`, semantic colors, and components (`framework/core/designsystem/`)
- **Convention Plugins**: Gradle build-logic in `build-logic/convention/` — use `cmp.feature` for features, `cmp.library` for libraries
- **Core Modules**: data (Ktor HTTP), domain, presentation (CoreBottomNavBar), logger (KMPFramework `com.tekmoon.logger` package, exposed as `ShowMeLoggerK`)

## Key Conventions

- Destinations use `enum class : Destination` with stable IDs: `"kompass/<feature>/$name"`
- Navigation args are `@Serializable` data classes encoded via `Json.encodeToString`
- Use `newScope()` for screens with multiple instances, `defaultScope()` for singletons
- Use `rememberScoped<T>()` instead of raw ViewModel for navigation-scoped lifecycle
- UI uses design system tokens (`DsTheme.colors`, `DsTheme.spacing`, etc.) — prefer `DsButton`, `DsText` over Material directly
- Feature modules use the `convention.cmp.feature` plugin which auto-includes all core dependencies

## Skills

See `.claude/skills/` for detailed guidance:

- `tekmoon-kompass-navigation` — Navigation patterns
- `tekmoon-design-system` — Theme and components
- `tekmoon-feature-scaffold` — Creating new feature modules + NavigationGraph wiring
- `tekmoon-feature-mvi` — Per-screen MVI (Domain/ViewModel/Screen) on `CommonViewModel`
- `tekmoon-project-structure` — Module layout and build system
- `caffeinate` — Prevent macOS sleep during long builds
- `7rule` — Baseline working discipline
- `git-commit` — Atomic Conventional Commits
- `git-pr` — Push a feature branch and open a PR for review (supersedes the old "never push" rule; protected branches stay off-limits)

Sub-agents in `.claude/agents/`: code-explorer, feature-scaffolder, build-verifier, reviewer, detekt-fixer, pr-manager, context-guardian.

## Git workflow

- After any file creation or edit, run `git add` on the touched files (or `git add -A` from the project root) so the working tree stays staged.
- Commits are allowed when I ask for them (e.g. "commit this") or under the Autonomous Agent Protocol below. Use Conventional Commit messages (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- Pushing `feature/*` branches is allowed via the `git-pr` skill (`git push -u origin feature/<name>`).
- Merging into `develop` is allowed — open a PR targeting `develop` and merge it (Git Flow), or merge the feature branch into `develop` directly. You may push to `develop`.
- **`main` is protected and MINE.** NEVER push to `main`, merge into `main`, or approve/merge a PR targeting `main`. The `develop → main` promotion is my exclusive decision.
- If `git add` reports an error (e.g. the file is gitignored), just continue; do not stop to report it.

## Autonomous Agent Protocol

You may be asked to work autonomously, with little or no supervision. Follow this protocol strictly so I can review your work safely and keep a clean Git Flow history.

### Branching model (Git Flow)

This repo uses Git Flow with three kinds of branches:

- `main` — production. NEVER work here directly. NEVER commit here.
- `develop` — integration branch. NEVER commit here directly.
- `feature/*` — where all actual work happens.

Feature branches ALWAYS start from `develop`, never from `main`.

### Starting a task — create a feature branch

Before writing any code, create a feature branch from an up-to-date `develop`:

1. Make sure `develop` is current: `git fetch && git switch develop && git pull`
2. Create and switch to a new feature branch. Use a short, descriptive, kebab-case name based on the task:
   `git switch -c feature/<task-name>`
3. Do ALL of your work on this branch inside the main checkout (`~/Desktop/Andre/Apps/KMP/KMPFramework`).
4. State clearly which branch you created.

### Working — commit discipline

- Restate the task in one or two sentences and list expected files before starting. If ambiguous, STOP and ask.
- Work in small, logical steps.
- After each logical unit, `git add` the changed files and make a LOCAL commit with a Conventional Commit message (`feat:`, `fix:`, `refactor:`, `test:`, etc.).
- Keep commits small and atomic — one concern each — so I can review and revert individual pieces.

### Hard limits (never do these)

- Do real work on `feature/*` branches; push them via the `git-pr` skill.
- Merging `feature/*` into `develop` is allowed (PR to `develop` then merge, or a direct Git Flow merge). You may push to `develop`.
- **NEVER push to, commit to, or merge into `main`.** Never approve/merge a PR targeting `main`. The `develop → main` promotion is MINE alone.
- NEVER force-push, hard-reset, or run `git clean`.
- NEVER delete files in bulk or run destructive shell commands.
- NEVER touch signing material (`*.jks`, `*.keystore`, `keystore.properties`) or any `.env` file.
- NEVER change `build-logic`, version numbers, or publishing config unless the task explicitly says so.

### Verify before committing

- Before each commit, make sure the relevant module still compiles (e.g. `./gradlew :composeApp:compileKotlinJvm`).
- If the task touches game-engine logic, run the relevant tests.
- If something fails and you cannot fix it confidently, STOP, make a WIP commit describing the problem, and leave a note for me instead of forcing a workaround.

### Finishing

When done (or stuck), summarize:

- which feature branch you used,
- what you did and which commits you made (with their messages),
- compile/test status,
- what is left and every assumption you made.

**Before merge** — I will review the feature branch and decide whether it merges into `develop`.

**After merge into `develop`** — run branch hygiene (see below).

### Branch hygiene — pruning merged feature branches

Merged `feature/*` branches accumulate locally. Whenever you finish a merge-into-`develop`, ALSO run a quick prune of older merged branches before reporting the wrap-up.

**Retention rule** — a merged branch is SPARED if **either** of these is true:
- it was merged within the **last 7 days**, OR
- it is among the **last 10 merged feature branches** (counted by merge-commit date on develop).

A branch is deleted only when BOTH conditions fail (older than 7 days AND outside the last-10 window).

**Recipe**:

1. List all locally-merged feature branches, sorted by merge-commit date on develop (most recent first):
   ```sh
   git for-each-ref --format='%(refname:short) %(committerdate:iso8601)' refs/heads/feature \
     | while read branch _date; do
         if git merge-base --is-ancestor "$branch" develop; then
           merge_commit=$(git log --merges --first-parent develop --format='%H %ci' \
             | grep -F "$(git rev-parse $branch)" | head -1)
           echo "$merge_commit $branch"
         fi
       done | sort -r
   ```
2. Apply the retention rule. The candidates to delete are those past **both** windows.
3. Show me the list of candidates and the kept ones BEFORE deleting. I confirm, then:
   ```sh
   git branch -d <name>   # safe-delete; never use -D
   ```
4. If `-d` refuses (would need `-D`), STOP and flag the branch — that means git still sees unmerged work on it.

Skip the prune entirely if listing the candidates would take more than a few seconds (e.g. on a fresh clone with no merged feature branches). The just-merged branch counts toward the "last 10" — it never gets pruned by the same wrap-up that merged it.
