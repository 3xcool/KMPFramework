# Kompass vs Navigation 3

A side-by-side comparison of the in-house **Kompass** navigation library
(`framework/kompass` in this repo) and Google's **Navigation 3** (alpha) library
for Jetpack Compose / Compose Multiplatform.

> **Knowledge horizon.** This document was written in May 2026 with Navigation 3
> details accurate as of mid-2025 alpha releases; specifics (package names,
> class names, alpha cadence) may have evolved. Treat the Nav3 sections as a
> snapshot, not a moving target. Validate with the latest AndroidX docs before
> betting on specific API shapes.

---

## TL;DR

| Aspect                     | Kompass                                  | Navigation 3                            |
| -------------------------- | ---------------------------------------- | --------------------------------------- |
| **Mental model**           | Redux reducer over an immutable state    | Direct mutation of an observable stack  |
| **Multiplatform fit**      | Pure CMP from day one                    | Compose-first, increasingly CMP-friendly|
| **Type-safety of args**    | `TypedDestination<T>` with KSerializer   | Data-class destination keys             |
| **Multi-pane / list-detail** | Built-in (`SceneLayoutListDetail`)     | Built-in ("Scenes")                     |
| **Maturity**               | Stable (you own it), small surface       | Alpha — API may shift                   |
| **Ecosystem**              | None — internal                          | Google-backed, growing fast             |
| **Testability of routing** | High (pure reducer)                      | Medium (mutate + observe pattern)       |
| **Best for**               | Teams that want full control, CMP-first  | Greenfield Compose apps OK with alpha   |

**Ratings: Kompass 8.5 / 10 · Navigation 3 8 / 10.** Both are good. The
"right" choice depends more on your team's context than on a feature-feature
comparison; see *When to choose which*.

---

## What each library is

### Kompass (this repo)

A homegrown Compose Multiplatform navigation library following a Redux pattern.
Core surface:

```
NavigationState  ──+──> NavigationHandler.reduce(state, cmd) ──> new NavigationState
                   │
NavigationCommand──┘     (pure, deterministic reducer)
```

- `NavigationState` — immutable, contains a `backStack: ImmutableList<BackStackEntry>`
- `NavigationCommand` — sealed type: `Navigate`, `Pop`, `ReplaceRoot`
- `NavigationHandler` — the reducer; pure function, no side effects
- `NavController` — Compose-aware façade; dispatches commands and triggers
  scope cleanup
- `Destination` — interface with a stable `id: String`
- `BackStackEntry` — destinationId + opaque encoded args + scopeId + pending
  result key + results map
- `NavigationGraph` — composes destinations to renderers; multiple graphs can
  coexist
- `SceneLayout` — pluggable strategy for rendering the active back stack.
  Three implementations ship out of the box: `SceneLayoutSinglePane` (no
  animation), `SceneLayoutDefaultAnimatedSinglePane` (animated), and
  `SceneLayoutListDetail` (adaptive 35/65 master-detail above a configurable
  width threshold, single-pane below). Custom implementations are trivial.
- `TypedDestination<T>` — type-safe variant of `Destination`. Carries a
  `KSerializer<T>`; the controller's `Json` instance handles encode/decode
  transparently via `navigateTo` / `requireArgs` extensions. Plain
  `Destination` remains for parameterless screens.
- `DeepLinkHandler` — explicit deep-link → command list resolver
- `NavigationScopes` — manages per-scope lifecycle (ViewModel-style scopes)
- Saveable via `rememberSaveable` with a custom JSON-based saver

### Navigation 3 (`androidx.navigation3`, alpha 2025)

Google's next-generation Compose-first navigation library. Replaces the older
`androidx.navigation` for new projects (the old library remains supported).
Key concepts:

- `NavKey` — your destination type, typically a `@Serializable data class`
- Backstack is `mutableStateListOf<NavKey>()` you own and mutate directly
- `NavDisplay` — renders the backstack with transitions
- `entryProvider { entry<HomeKey> { ... } }` — DSL mapping keys to content
- `NavEntryDecorator` — built-in lifecycle, ViewModel, and saved-state scoping
- "Scenes" support side-by-side or layered destinations (e.g., list-detail)
- First-class predictive-back support
- Compose-Multiplatform-friendly (`navigation3-runtime` artifact targets KMP)

---

## Mental model

### Kompass: pure reducer

Every navigation change is described as a `NavigationCommand`. Commands are
data; they don't *do* anything. The `NavigationHandler.reduce(state, cmd)`
function turns a command into a new state. The `NavController` then atomically
swaps the state and runs scope-cleanup side effects.

**Implication:** `NavigationHandler` is a pure function. You can unit-test
every routing rule (pop, popUpTo, popUntil, replaceRoot, reuseIfExists,
clearBackStack) without spinning up Compose or any UI.

### Navigation 3: observable mutation

You expose a `mutableStateListOf<NavKey>()` from your ViewModel (or a
remembered state holder). To navigate, you `add()` or `removeLast()` on the
list. `NavDisplay` observes the list and animates accordingly.

**Implication:** Routing logic lives wherever you mutate the list. Test by
exercising the same mutator code; less ceremony, but no built-in audit trail
of "what command was issued".

Both are valid; Kompass leans toward predictability and audit-ability,
Navigation 3 leans toward simplicity and direct manipulation.

---

## Feature comparison

| Feature                                | Kompass                                            | Navigation 3                                          |
| -------------------------------------- | -------------------------------------------------- | ----------------------------------------------------- |
| Pure Kotlin / no Android deps          | Yes (commonMain only)                              | Yes (`navigation3-runtime` is KMP-friendly)           |
| Type-safe destinations                 | `Destination` (parameterless) or `TypedDestination<T>` (typed args) | `NavKey` data classes; args are class fields          |
| Type-safe args                         | Yes — `TypedDestination<T>` + `@Serializable` data class            | Yes — class properties                                |
| Argument restoration                   | Built-in JSON encode/decode via controller's `Json` instance        | Built-in via `@Serializable` keys                     |
| Result-passing                         | First-class (`pendingResultKey` + `results` map)   | Manual (shared state or custom protocol)              |
| Deep links                             | First-class (`DeepLinkHandler` → command list)     | Manual (parse URL, mutate the stack)                  |
| Multi-graph composition                | First-class (`NavigationGraph` list)               | Via composed `entryProvider` blocks                   |
| Multi-pane / list-detail               | First-class — `SceneLayoutListDetail` ships built in (adaptive)     | First-class "Scenes" API                              |
| Predictive back (Android)              | DIY via `PlatformBackHandler`                      | First-class                                           |
| Per-entry ViewModel scoping            | Custom `NavigationScopeId` + `rememberScoped<T>()` | Built-in via `NavEntryDecorator`                      |
| State restoration across recompose     | `rememberSaveable` + custom Saver                  | Built-in                                              |
| State restoration across process death | Yes via `rememberSaveable` JSON                    | Yes via `@Serializable`                               |
| Animation customization                | `NavigationTransition` + `SceneLayout`             | `NavDisplay.transitionSpec` + per-key transitions     |
| Reducer testability                    | Excellent — pure function                          | Good — exercise mutator code                          |
| Multiple back stacks                   | Possible via scope ids and graphs                  | Possible via multiple `NavDisplay` instances          |
| iOS / Desktop support                  | Native — same code path as Android                 | Yes via KMP artifact, but Android-tested-most         |
| Documentation                          | KDoc in source, this repo's notes                  | developer.android.com (extensive but evolving)        |
| Community / Stack Overflow             | None (internal)                                    | Growing                                               |
| Risk of breaking changes               | You control it                                     | Alpha — yes, expect movement                          |

---

## Kompass — pros and cons

### Pros

- **Pure reducer architecture.** `NavigationHandler.reduce(state, cmd)` is a
  pure function. Every routing rule is unit-testable in isolation. This is the
  single biggest engineering virtue here.
- **You own the code.** No external dependency to track, no alpha-to-beta API
  churn, no "Google deprecated this" surprises. Bugs are yours to fix on your
  schedule.
- **Pure CMP from day one.** No Android assumptions baked in. Same code paths
  on Android, iOS, Desktop, JVM.
- **First-class result-passing.** `pendingResultKey` + `results` is a modeled
  pattern, not something each consumer reinvents.
- **First-class deep linking.** `DeepLinkHandler` resolves a URI to a list of
  commands, which then run through the reducer like any other navigation —
  the same auditability as in-app navigation.
- **Pluggable rendering via `SceneLayout`.** Three strategies ship out of the
  box (`SceneLayoutSinglePane`, `SceneLayoutDefaultAnimatedSinglePane`, and the
  adaptive `SceneLayoutListDetail`). Routing logic is unaffected by the
  rendering strategy you pick — swap them per graph.
- **Type-safe arguments.** `TypedDestination<T>` lets you declare
  `@Serializable` argument data classes and use `navController.navigateTo(dest, args)`
  / `navController.requireArgs(dest)` without manual JSON encode/decode at
  call sites.
- **Lightweight.** No reflection, no annotation processing, no codegen. Plain
  Kotlin + Compose runtime.
- **Explicit scope lifecycle.** `NavigationScopeId` makes it obvious which
  entries share a ViewModel scope; `rememberScoped<T>()` reads naturally.

### Cons

- **No ecosystem.** No third-party libraries that integrate with it. No
  Stack Overflow answers. Future hires need to learn it from scratch.
- **No predictive back integration out of the box.** Predictive back on
  Android requires wiring through your own `PlatformBackHandler`. Doable but
  not free.
- **Maintenance burden falls on the team.** Every quirk, every edge case,
  every Compose runtime interaction change is yours to absorb. With one
  primary maintainer, this is a real risk.
- **Documentation is what's in the source.** KDoc is good, but there's no
  curated guide, no "common patterns" doc, no migration playbook beyond the
  samples in the repo.
- **Saveable JSON serialization can drift.** If the shape of `BackStackEntry`
  args changes between releases, restored state from older app versions can
  fail to decode. Need a versioning strategy (a `_version` field on each
  `@Serializable` args class is the simplest).

---

## Navigation 3 — pros and cons

### Pros

- **Type-safe destinations.** Your `NavKey` is a regular data class with
  typed fields. The compiler enforces argument shapes; no runtime decode
  errors from typo'd keys.
- **Backed by Google.** Curated docs, ongoing development, Stack Overflow
  presence will be there. Long-term Android support is essentially
  guaranteed.
- **Predictive back is first-class.** Animations integrate automatically.
- **Multi-pane / Scenes built in.** List-detail and side-by-side layouts
  ship out of the box; you don't write them.
- **Per-entry lifecycle / ViewModel scoping built in.** No custom
  `NavigationScopes` machinery to maintain.
- **Direct backstack ownership.** `mutableStateListOf<NavKey>()` is a
  primitive every Compose dev already understands. Onboarding cost is low.
- **CMP support is improving.** AndroidX Compose now ships through Compose
  Multiplatform, so KMP/iOS/Desktop targets work via the `navigation3-runtime`
  artifact (verify per release).

### Cons

- **It's alpha.** Names, package layout, behavior are all liable to change.
  If you ship to production, you're committing to track API churn.
- **Less prescribed for cross-cutting concerns.** Deep links, complex result
  flows, multiple back stacks — the docs nudge you toward "do it yourself by
  mutating state". Fine, but each team rebuilds the same primitives.
- **Routing logic isn't a pure function.** Tests look like "set up a list,
  call my mutator, assert the list state". Doable, but not as crisp as
  reducing a command over an immutable state.
- **Saveable arguments depend on `kotlinx.serialization`.** Mostly fine, but
  any non-serializable type (Compose state holders, lambdas, etc.) needs
  explicit handling.
- **Less control over rendering.** `NavDisplay` is opinionated. Customizing
  animations is supported but you're working within its model, not your own
  `SceneLayout`-style abstraction.
- **CMP coverage is uneven.** Android paths are best-tested. iOS/Desktop
  parity exists but lags Android in samples and bug fixes.

---

## Ratings

> Ratings are subjective and contextual. They reflect "how well does this
> library serve a senior mobile team building a CMP product right now."

### Kompass — **8.5 / 10**

What it gets right is genuinely excellent: the reducer pattern makes routing
the most testable part of your codebase, and pure-CMP support without Android
assumptions is rare. With `TypedDestination<T>` + `navigateTo` /
`requireArgs`, type-safe arguments are now first-class — no more opaque JSON
strings at call sites. With the three pre-built `SceneLayout` strategies
(including the adaptive `SceneLayoutListDetail`), multi-pane support is also
out of the box. The cost is what you'd expect from any in-house abstraction —
you're the ecosystem.

**Why not higher:** predictive back on Android still needs explicit wiring,
the documentation outside this repo is non-existent, and there's a real
single-maintainer risk if the team grows or rotates.

**Why not lower:** the architecture is genuinely good. The samples
(`NavSample1ReturningResult` … `NavSample8ExpenseTracker`) demonstrate
nontrivial real-world flows. The recent additions (typed destinations, the
list-detail scene) close the two most common gaps consumers hit when moving
from Nav3 / older `androidx.navigation`.

### Navigation 3 — **8 / 10**

The right long-term bet for a Compose-first team that's OK with alpha churn.
Type-safety, scoping, predictive back, and Google-backed docs cover the
breadth that an in-house lib can't realistically match. The discount is the
alpha label; if you ship something today and Google reshuffles the API in
beta, that's your problem to absorb.

**Why not higher:** alpha means production teams have to track API moves.
Result-passing and deep-linking are less prescribed than Kompass's
opinionated takes — your team will end up reinventing some patterns.

**Why not lower:** the core design is a clear improvement over the older
`androidx.navigation`, the type-safety is genuine, and the multi-pane support
out of the box is a meaningful productivity win.

---

## When to choose which

**Stay on Kompass if:**
- The team is small, the maintainer is engaged, and the CMP-first story
  matters (you target iOS and Desktop seriously, not just Android).
- Your existing code is already on Kompass and the patterns work for you.
  Migration cost is real; don't pay it without a concrete reason.
- You value the audit-ability of "every navigation change is a typed
  command" — e.g., you log them, replay them in tests, or reason about
  them in design docs.
- You don't need predictive back on Android *yet*, or you're OK wiring it
  through `PlatformBackHandler` once.

**Move to (or start on) Navigation 3 if:**
- You're starting fresh and want type-safety from day one.
- The team will grow and onboarding cost matters more than control.
- You need multi-pane / list-detail layouts and don't want to write
  `SceneLayout` strategies yourself.
- You're OK riding the alpha → beta → stable wave (it's mostly settled now,
  but expect occasional movement).
- You don't have an existing custom navigation lib to maintain.

**Hybrid (worth considering):**
- Use Kompass today as the primary nav system, but add a thin `Destination`
  adapter so you could *swap to Nav3 later* without rewriting screen code.
  Mostly that means: never let screens import `NavController` directly,
  always go through a small façade. Cheap insurance.
- Treat Nav3 as the destination — once it's stable for ~6 months and your
  team's CMP needs are well-served — and migrate then.

---

## A pragmatic note for this repo

Kompass currently lives in `framework/kompass` and is consumed by Soccos via
the published `com.tekmoon:kompass` artifact. It works. The samples
(`samples/expenseTracker`, etc.) demonstrate real-world flows.

**Closed gaps (already in the codebase):**

- ✅ **Type-safe arguments** — `TypedDestination<T>` + `NavController.navigateTo` /
  `requireArgs` extensions. See `TypedDestination.kt`.
- ✅ **Multi-pane / adaptive layouts** — `SceneLayoutListDetail` ships with a
  configurable compact-width threshold and a 35/65 master-detail split above it.

**Open gaps (worth tackling next):**

1. **Predictive back integration.** Wire `PlatformBackHandler` to Android's
   `OnBackPressedDispatcher` predictive-back callbacks. ~20 lines per
   platform; biggest UX gap vs Nav3 today.
2. **Args-class versioning convention.** Add a `_version` field guideline in
   the docs and a small migration helper. Today, an old saved state with a
   newer args class that has new required fields will fail to decode.
3. **Curated docs site.** A short MkDocs/Dokka page covering common patterns
   (typed destinations, result-passing, scoped ViewModels, deep links). The
   in-source KDoc is excellent but not discoverable.

Closing those three would push Kompass into ~9 territory and keep you on a
navigation library you fully control.

---

*Document maintained alongside `framework/kompass`. Update when either library
materially changes.*
