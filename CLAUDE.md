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
- `tekmoon-feature-scaffold` — Creating new features
- `tekmoon-project-structure` — Module layout and build system

## Git workflow

- After any file creation or edit, run `git add` on the touched files (or `git add -A` from the project root) so the working tree stays staged.
- Commits are allowed when I ask for them (e.g. "commit this") or under the Autonomous Agent Protocol below. Use Conventional Commit messages (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
- **NEVER run `git push`.** All commits stay local for my review.
- If `git add` reports an error (e.g. the file is gitignored), just continue; do not stop to report it.

## Autonomous Agent Protocol

You may be asked to work autonomously, with little or no supervision. Follow this protocol strictly so I can review your work safely and keep a clean Git Flow history.

### Branching model (Git Flow)

This repo uses Git Flow with three kinds of branches:

- `main` — production. NEVER work here directly. NEVER commit here.
- `develop` — integration branch. NEVER commit here directly.
- `feature/*` — where all actual work happens.

Feature branches ALWAYS start from `develop`, never from `main`.

### Starting a task — create an isolated worktree

Before writing any code, isolate your work in its own git worktree on a new feature branch, created from an up-to-date `develop`:

1. Make sure `develop` is current: `git fetch && git switch develop && git pull`
2. Create the worktree + feature branch from develop. Use a short, descriptive, kebab-case name based on the task:
   `git worktree add ../soccos-<task-name> -b feature/<task-name> develop`
3. Do ALL of your work inside that worktree directory. Do not edit files in the main checkout.
4. State clearly which worktree path and which branch you created.

If you are already running inside an isolated worktree (started with `--worktree`), still make sure your branch is `feature/<task-name>` and was based on `develop`; rename/rebase if needed, and tell me what you did.

The repo-root `.worktreeinclude` file lists local-only / gitignored files (e.g. `local.properties`, `keystore.properties`, `.claude/settings.local.json`) that must be copied into each new worktree so Gradle and tooling work out of the box. Worktree-creation scripts read it; do not edit it as part of a feature unless the task asks.

### Working — commit discipline

- Restate the task in one or two sentences and list expected files before starting. If ambiguous, STOP and ask.
- Work in small, logical steps.
- After each logical unit, `git add` the changed files and make a LOCAL commit with a Conventional Commit message (`feat:`, `fix:`, `refactor:`, `test:`, etc.).
- Keep commits small and atomic — one concern each — so I can review and revert individual pieces.

### Hard limits (never do these)

- NEVER run `git push`. All commits stay local for my review.
- NEVER commit to `main` or `develop` directly.
- NEVER merge into `main`. You MAY merge `develop` into your feature branch to stay current, but the feature → develop → main flow is MY decision, done by me.
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

- which worktree path and feature branch you used,
- what you did and which commits you made (with their messages),
- compile/test status,
- what is left and every assumption you made.

**Before merge** — I will review the feature branch and decide whether it merges into `develop`. Do NOT clean up the worktree yourself yet — leave it intact so I can review it.

**After merge into `develop`** (whether you ran the merge or I did) — clean up the worktree as part of the wrap-up, then run branch hygiene (see below):

```sh
git worktree remove ../<worktree-name>
```

The feature branch ref stays after the worktree is removed; branch hygiene below decides when it gets deleted.

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
