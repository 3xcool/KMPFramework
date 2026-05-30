# Analytics

How the Tekmoon KMP framework records product analytics, how it stays compliant with **LGPD** (Lei nº 13.709 / 2018) by default, and how to wire a real backend (Firebase / Mixpanel / Amplitude) when you're ready.

---

## TL;DR

- The framework owns a vendor-neutral `AnalyticsClient` interface in `:framework:feature:analytics`. Adapters are separate, swappable modules.
- Every event passes through a `PiiPolicy` decorator installed at startup. The **default policy drops every `Pii`-tagged value** — the framework is LGPD-safe out of the box and stays that way until you consciously opt into a more permissive policy with lawful basis.
- Inject `AnalyticsClient` via constructors. Composables read `LocalAnalytics`; interactive `Ds*` primitives auto-emit a `track(...)` event when you set `analyticsId = ...`.

---

## Architecture

```
   call site (ViewModel / repo / composable)
                    │
                    │  inject / LocalAnalytics
                    ▼
            AnalyticsClient (interface)
                    │
                    │  installed by Framework.start
                    ▼
        PolicyAnalyticsClient (decorator) ──── applies PiiPolicy
                    │
                    ▼
         underlying client (NoOp | Multi | Firebase | Mixpanel | …)
```

The `PolicyAnalyticsClient` is always present — it's set up by `Framework.start` based on `FrameworkInit.analyticsClient` + `FrameworkInit.piiPolicy`. Adapters never see raw `Pii` values; they receive either the policy-transformed plain value or nothing at all.

---

## The PII model

Three pieces, all in `com.tekmoon.analytics`.

### `Pii(value, classification)`

Wraps a single property value to mark it as personally identifiable. Plain values in the params map are treated as anonymous and pass through.

```kotlin
analytics.track("checkout_completed", mapOf(
    "amount"  to 99.50,                                    // anonymous, plain
    "cart_id" to cart.id,                                  // anonymous, plain
    "email"   to Pii(user.email, PiiClass.Personal),       // tagged Personal
    "ip"      to Pii(deviceIp, PiiClass.PseudoAnonymous),  // tagged pseudo
))
```

### `PiiClass`

Tiering matches LGPD:

| Tier | LGPD reference | Examples |
|---|---|---|
| `PseudoAnonymous` | Art. 5, II — _dado pessoal_ | device ID, hashed user ID, session token |
| `Personal` | Art. 5, II — _dado pessoal_ | email, full name, phone, address |
| `Sensitive` | Art. 11 — _dado pessoal sensível_ | race, religion, health, biometrics, union membership |

`Anonymous` is intentionally **not** in the enum — non-PII values don't need a tag.

### `PiiPolicy`

`fun interface PiiPolicy { fun transform(key: String, pii: Pii): Any? }` — returns the value to forward, or `null` to drop the property.

Built-ins:

| Policy | Behavior | When to use |
|---|---|---|
| `PiiPolicy.DropAll` | Strips every `Pii` value regardless of tier. **Default.** | Before you have lawful basis for processing personal data. |
| `PiiPolicy.KeepPseudonymized` | Keeps `PseudoAnonymous`; drops `Personal` and `Sensitive`. | You have legitimate-interest basis for pseudonymized device/user IDs but not directly identifying data. |
| `PiiPolicy.PassThrough` | Forwards every tier as-is. | Only after collecting explicit, informed consent. Useful in dev / staging or in a crash-reporting adapter with a separate opt-in. |

A custom policy is trivial:

```kotlin
val hashing = PiiPolicy { key, pii ->
    when (pii.classification) {
        PiiClass.Sensitive -> null                          // never forward
        PiiClass.Personal -> sha256(pii.value.toString())   // hash and forward
        PiiClass.PseudoAnonymous -> pii.value               // forward as-is
    }
}
```

---

## Wiring it up

### At startup

```kotlin
Framework.start(
    FrameworkInit(
        apiBaseUrl = "https://api.example.com",
        analyticsClient = MultiAnalyticsClient(listOf(
            FirebaseAnalyticsClient(context),    // your adapter
            MixpanelAnalyticsClient(token),       // your adapter
        )),
        piiPolicy = PiiPolicy.KeepPseudonymized,  // chosen after legal review
    )
)
```

`Framework.start` wraps the client with `PolicyAnalyticsClient(analyticsClient, piiPolicy)` and exposes it via `Framework.analytics`.

### At call sites — constructor injection (preferred)

```kotlin
class CheckoutViewModel(
    private val analytics: AnalyticsClient,
) : CommonViewModel<…>() {
    fun onSubmit() {
        analytics.track("checkout_started", mapOf("cart_size" to state.items.size))
        // …
    }
}
```

Inject `Framework.analytics` at the construction site (typically your DI module). Tests pass `RecordingAnalyticsClient()` instead.

### At call sites — composables

The design system provides a `LocalAnalytics` CompositionLocal. Set it once at the root of your composition, then `Ds*` primitives forward events automatically when you provide an `analyticsId`:

```kotlin
@Composable
fun App() {
    CompositionLocalProvider(LocalAnalytics provides Framework.analytics) {
        DsTheme {
            HomeScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    DsButton(
        text = "Continue",
        onClick = { /* … */ },
        analyticsId = "home.continue",                          // emits "ds_button_clicked"
        analyticsParams = mapOf("step" to currentStep),         // merged into event
    )
}
```

Events emitted by the design system today:

| Component | Event | Default params (besides your `analyticsParams`) |
|---|---|---|
| `DsButton` | `ds_button_clicked` | `id`, `text` |
| `DsIconButton` | `ds_icon_button_clicked` | `id` |
| `DsClickableText` | `ds_clickable_text_clicked` | `id` |
| `DsLinkText` | `ds_link_clicked` | `id`, `url` |

`DsLinkText` does its own tracking and explicitly passes `analyticsId = null` to the wrapped `DsClickableText` so you never get a duplicate event.

Multi-action components (`DsAlert`, `DsBanner`, `DsDialog`, `DsSnackbar`, clickable `DsCard`) will get the same treatment in a follow-up.

---

## Writing an adapter

An adapter is just an `AnalyticsClient` impl that forwards to a vendor SDK. Pattern:

```kotlin
class FirebaseAnalyticsClient(
    private val firebase: FirebaseAnalytics,  // the vendor type
) : AnalyticsClient {

    override fun track(event: String, params: Map<String, Any?>) {
        firebase.logEvent(event, params.toBundle())
    }

    override fun screen(name: String, params: Map<String, Any?>) {
        firebase.logEvent("screen_view", (params + ("screen_name" to name)).toBundle())
    }

    override fun identify(userId: String?, traits: Map<String, Any?>) {
        firebase.setUserId(userId)
        traits.forEach { (k, v) -> firebase.setUserProperty(k, v?.toString()) }
    }

    override fun reset() = firebase.resetAnalyticsData()
    override suspend fun flush() { /* SDK-specific or no-op */ }
}
```

You **don't** need to handle `Pii` — the `PolicyAnalyticsClient` decorator has already stripped or transformed every tagged value before your adapter is called.

Adapters live in their own sub-modules (`framework/feature/analytics-firebase`, etc.) so the consuming app only pulls in the SDKs it actually uses. None ship yet — adding them is mechanical now that the interface is locked.

---

## Testing

Use `RecordingAnalyticsClient` from commonMain (not commonTest — downstream consumers can use it too):

```kotlin
@Test
fun submit_emits_checkout_started_event() {
    val analytics = RecordingAnalyticsClient()
    val vm = CheckoutViewModel(analytics)

    vm.onSubmit()

    val event = analytics.tracks.single()
    assertEquals("checkout_started", event.event)
    assertEquals(3, event.params["cart_size"])
}
```

For policy-related assertions, wrap with `PolicyAnalyticsClient(recorder, somePolicy)` and assert against the recorder.

---

## What `Framework.analytics` returns

- If `FrameworkInit.analyticsClient` is `null`: a `PolicyAnalyticsClient(NoOpAnalyticsClient, piiPolicy)` — every call is silent. Call sites never need null checks.
- If a client was provided: `PolicyAnalyticsClient(yourClient, yourPolicy)`. The decorator is always present so the PII guarantee holds regardless of which adapter you wired.

`Framework.analytics` is read-only after `Framework.start` — no mid-session mutation, no race-prone global writes. Constructor injection on top of a stable, set-once value.

---

## Roadmap (deferred from this PR)

- **Adapters** — Firebase Analytics, Mixpanel, Amplitude as separate sub-modules.
- **Multi-action DS components** — `DsAlert` / `DsBanner` / `DsDialog` / `DsSnackbar` / clickable `DsCard` get `analyticsId` per action.
- **Kompass screen tracking** — auto-emit `analytics.screen(...)` when the `NavController` destination changes.
- **Consent UI** — an opt-in dialog module (LGPD / GDPR) that toggles `PiiPolicy` at runtime once user consent is granted. Likely Phase 3.

---

## Reference

- API: `com.tekmoon.analytics.{AnalyticsClient, NoOpAnalyticsClient, MultiAnalyticsClient, RecordingAnalyticsClient, Pii, PiiClass, PiiPolicy, PolicyAnalyticsClient}` in `:framework:feature:analytics`.
- Composable bridge: `com.tekmoon.designsystem.analytics.LocalAnalytics` in `:framework:core:designsystem`.
- Wiring: `Framework.start(FrameworkInit(analyticsClient = …, piiPolicy = …))` in `:framework:sdk`.
- Tests: `RecordingAnalyticsClientTest`, `MultiAnalyticsClientTest`, `PolicyAnalyticsClientTest`, `NoOpAnalyticsClientTest`.
