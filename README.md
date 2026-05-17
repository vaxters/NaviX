# Navix

**A production-grade, Compose-first navigation platform for Android and Kotlin Multiplatform.**

Navix is not a wrapper around `NavController`. It is a standalone navigation runtime built around
a deterministic state machine, with zero reflection, first-class telemetry, built-in devtools,
and a KMP-portable core.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.vaxters/navix-runtime?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.vaxters/navix-runtime)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.20-blueviolet.svg)](https://kotlinlang.org)
[![KMP](https://img.shields.io/badge/KMP-Android%20%7C%20JVM%20%7C%20iOS-orange.svg)]()

---

## Quick Start

### 1. Add dependencies

```kotlin
// build.gradle.kts

// Required when using navix-compiler — sets the generated registry class name.
// Must be unique per module in multi-module projects (e.g., "checkout", "profile").
ksp {
    arg("navix.moduleName", "app")
}

dependencies {
    // Required — backstack engine, NavixHost, Navigator, and all core APIs.
    implementation("io.github.vaxters:navix-runtime:$navixVersion")

    // Required — KSP processor. Generates NavixRouteRegistry and DeepLinkHandler
    // implementations from your @RouteDestination-annotated routes at build time.
    ksp("io.github.vaxters:navix-compiler:$navixVersion")

    // Optional — navigation event pipeline with pluggable exporters (Logcat, Firebase, …).
    // Omit if you don't need analytics or in-app event history.
    implementation("io.github.vaxters:navix-telemetry:$navixVersion")

    // Optional — in-app debug overlay: live backstack inspector + event timeline.
    // Safe to ship; the overlay is a no-op when enabled = false (default).
    // debugImplementation keeps it out of release APKs entirely.
    debugImplementation("io.github.vaxters:navix-devtools:$navixVersion")

    // Optional — FakeNavigator and Compose test helpers for unit and UI tests.
    testImplementation("io.github.vaxters:navix-testing:$navixVersion")
}
```

### 2. Define routes

```kotlin
@Serializable
@RouteDestination(deepLinks = ["myapp://product/{productId}"])
data class ProductDetail(val productId: String) : Route

@Serializable
data object Home : Route
```

### 3. Set up navigation

```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator(root = Home)

    NavixHost(navigator = navigator) {
        screen<Home> { _, _ ->
            HomeScreen(onProductClick = { id ->
                navigator.push(ProductDetail(id), NavTransitionKey.SlideLeft)
            })
        }
        screen<ProductDetail> { _, route ->
            ProductDetailScreen(productId = route.productId)
        }
    }
}
```

### 4. Navigate

```kotlin
// Push a new screen
navigator.push(ProductDetail("123"))

// Push with a specific transition
navigator.push(ProductDetail("123"), NavTransitionKey.SlideLeft)

// Pop
navigator.pop()

// Replace current screen
navigator.replace(Settings)

// Clear stack and start over
navigator.reset(Login)

// Pop back to a specific route
navigator.popTo<Home>()

// Handle a deep link URI
navigator.handleDeepLink("myapp://product/123")
```

---

## Module Overview

| Module              | Description                                              |    KMP     |
|---------------------|----------------------------------------------------------|:----------:|
| `contracts`         | Shared data types (`Route`, `RouteEntry`, `NavEvent`, …) |     ✅      |
| `navix-annotations` | `@RouteDestination` (source retention)                   |     ✅      |
| `navix-runtime`     | Backstack engine + Compose `NavixHost`                   |  Core: ✅   |
| `navix-compiler`    | KSP processor — route discovery, deep link generation    | Build-time |
| `navix-telemetry`   | Event pipeline with pluggable exporters                  |     ✅      |
| `navix-devtools`    | Live backstack inspector + event timeline overlay        |  Android   |
| `navix-testing`     | `FakeNavigator` + Compose test helpers                   |  Core: ✅   |

---

## Telemetry

`NavixTelemetryPipeline` fans out every `NavEvent` to all registered exporters asynchronously.
Events are buffered — navigation is never blocked waiting for exporter I/O.

```kotlin
val telemetry = NavixTelemetryPipeline(
    exporters = listOf(
        LogcatExporter(),           // built-in
        MyFirebaseExporter(),       // your implementation
        InMemoryEventExporter(),    // retain history for in-app inspection
    )
)

val navigator = rememberNavigator(root = Home, telemetry = telemetry)
```

Implement `NavEventExporter` to route events to any backend:

```kotlin
class MyFirebaseExporter : NavEventExporter {
    override fun export(event: NavEvent) {
        Firebase.analytics.logEvent(event.type.name) {
            param("from", event.from?.route?.let { it::class.simpleName } ?: "")
            param("to", event.to?.route?.let { it::class.simpleName } ?: "")
        }
    }
}
```

An in-memory exporter is useful for surfacing event history inside the app itself — it holds
a `StateFlow<List<NavEvent>>` that a screen can observe directly, unlike the hot `SharedFlow`
on `Navigator.events` which loses events emitted before a subscriber attaches:

```kotlin
class InMemoryEventExporter(private val maxEvents: Int = 100) : NavEventExporter {
    private val _events = MutableStateFlow<List<NavEvent>>(emptyList())
    val events: StateFlow<List<NavEvent>> = _events.asStateFlow()

    override fun export(event: NavEvent) {
        _events.update { (listOf(event) + it).take(maxEvents) }
    }
}
```

---

## DevTools

Add the overlay above your `NavixHost` content. It auto-disables in release builds.

```kotlin
Box(Modifier.fillMaxSize()) {
    NavixHost(navigator = navigator) { /* screens */ }
    NavixDevToolsOverlay(navigator = navigator)  // debug only
}
```

The overlay shows:

- Live backstack with lifecycle states
- Navigation event timeline
- Route timing and transition keys

---

## Testing

`FakeNavigator` is a drop-in `Navigator` that records all calls and provides assertion helpers:

```kotlin
val nav = FakeNavigator(root = Home)

nav.push(ProductDetail("42"))
nav.assertCurrentRoute(ProductDetail("42"))
nav.assertBackstackSize(2)
nav.assertLastPushed(ProductDetail("42"))
nav.assertCanPop(true)
nav.assertPushCount(1)

nav.reset(Home)
nav.assertBackstackSize(1)
nav.assertCanPop(false)
```

Use it in ViewModel tests with Turbine to verify nav effects:

```kotlin
@Test
fun `onProductClicked emits correct nav effect`() = runTest {
    val vm = HomeViewModel(GetProductsUseCase(FakeProductRepository()))

    vm.navEffect.test {
        vm.onProductClicked("42")
        val effect = awaitItem()
        assertEquals("42", (effect as HomeNavEffect.OpenProductDetail).productId)
    }
}
```

Use it in Compose UI tests to assert navigation outcomes without a real back stack:

```kotlin
class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `clicking product navigates to detail`() {
        val navigator = FakeNavigator(root = Home)
        composeRule.setContent {
            NavixHost(navigator = navigator) {
                screen<Home> { _, _ -> HomeScreen(navigator) }
            }
        }

        composeRule.onNodeWithText("Kotlin Multiplatform Guide").performClick()

        navigator.assertLastPushed(ProductDetail("p-001"))
        navigator.assertBackstackSize(2)
    }
}
```

---

## Deep Links

Annotate your route with deep link URI templates:

```kotlin
@Serializable
@RouteDestination(deepLinks = ["myapp://product/{productId}"])
data class ProductDetail(val productId: String) : Route
```

The KSP compiler generates a `DeepLinkHandler` automatically. Register it:

```kotlin
val navigator = rememberNavigator(
    root = Home,
    deepLinkHandlers = listOf(ProductDetailDeepLinkHandler()), // KSP-generated
)
```

Handle the incoming intent in your Activity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
        val navigator = rememberNavigator(root = Home, deepLinkHandlers = handlers)
        LaunchedEffect(intent) {
            intent?.data?.toString()?.let { uri -> navigator.handleDeepLink(uri) }
        }
        NavixHost(navigator) { /* screens */ }
    }
}
```

---

## Multi-Module Projects

Multi-module graph composition requires no annotations. Set the `navix.moduleName` KSP argument
in each subproject's `build.gradle.kts` — the compiler uses it to generate a uniquely-named
`NavixRouteRegistry` per module, preventing collisions:

```kotlin
// checkout/build.gradle.kts
ksp {
    arg("navix.moduleName", "Checkout")
}
```

This generates `CheckoutNavixRouteRegistry`. Compose all modules' registries at the `NavixHost`
call site. No per-file annotation ceremony is needed.

---

## State Restoration

Use `rememberSaveableNavigator` instead of `rememberNavigator` and the entire navigation
state survives **configuration changes and process death** — automatically:

```kotlin
val navigator = rememberSaveableNavigator(
    root = Home,
    saver = JsonNavigatorSaver(AppNavixSerializersModule), // KSP-generated module
)
NavixHost(navigator = navigator) { /* screen<…> { } */ }
```

What survives a process-death restore:

| State                                              | Mechanism                                                                                  |
|----------------------------------------------------|--------------------------------------------------------------------------------------------|
| Backstack (every route + its arguments)            | serialised by the `NavigatorSaver`                                                         |
| `ViewModel` `SavedStateHandle`                     | per-entry `SavedStateRegistry`, restored into a recreated entry                            |
| Screen `rememberSaveable { }`                      | per-entry, entry-id-keyed saved-state holder                                               |
| Entry-scoped `ViewModel` instances (config change) | host-Activity-scoped store, retained across recreation — no serialization cost on rotation |

Routes must be `@Serializable` so the snapshot can be persisted; the
`${Module}NavixSerializersModule` generated by KSP wires the polymorphic serializers.
Popped entries are evicted from the saved blob automatically, so its size tracks the live
backstack depth (not total navigations). If the saved blob can't be read (e.g. a schema
change across an app update), restore falls back to a fresh navigator at `root` — never a
crash.

### Multi-stack (bottom navigation)

`rememberSaveableNavixMultiStack` persists the **active tab index and every tab's
backstack** in addition to per-entry state:

```kotlin
val multiStack = rememberSaveableNavixMultiStack(
    specs = listOf(
        NavStackSpec(HomeRoot, key = "home"),
        NavStackSpec(SearchRoot, key = "search"),
        NavStackSpec(ProfileRoot, key = "profile"),
    ),
    saver = JsonNavigatorSaver(AppNavixSerializersModule),
)
NavixMultiStackHost(multiStack) { /* screen<…> { } shared across tabs */ }
```

Give each `NavStackSpec` a stable, unique `key` — per-tab restore is keyed by it, not by
list order.

---

## Custom Transitions

Pass a `NavTransitionSpec` to `NavixHost` to override enter/exit animations per
`NavTransitionKey`. Use this to remap what the `Default` key means globally, or to handle
custom keys your app defines:

```kotlin
val mySpec = object : NavTransitionSpec {
    override fun enterTransition(from: RouteEntry?, to: RouteEntry, key: NavTransitionKey) =
        when (key) {
            NavTransitionKey.Default -> slideInHorizontally { it }
            NavTransitionKey.Scale -> scaleIn(initialScale = 0.9f) + fadeIn()
            else -> fadeIn()
        }

    override fun exitTransition(from: RouteEntry, to: RouteEntry?, key: NavTransitionKey) =
        when (key) {
            NavTransitionKey.Default -> slideOutHorizontally { -it }
            NavTransitionKey.Scale -> scaleOut(targetScale = 1.1f) + fadeOut()
            else -> fadeOut()
        }
}

NavixHost(navigator = navigator, transitionSpec = mySpec) { /* screens */ }
```

Built-in keys: `Default`, `None`, `Fade`, `SlideLeft`, `SlideRight`, `Scale`.
Custom keys: `NavTransitionKey("my_key")`.

---

## Custom Backstack Reducer

The `BackstackReducer` type alias (`(BackstackSnapshot, BackstackAction) -> BackstackSnapshot`)
is an escape hatch for advanced stack behavior. Pass it to `rememberNavigator`:

```kotlin
// Single-top: navigating to a route type already on the stack pops to it instead.
val SingleTopReducer: BackstackReducer = { snapshot, action ->
    if (action is BackstackAction.Push) {
        val existingIndex = snapshot.entries.indexOfLast { it.route::class == action.route::class }
        if (existingIndex >= 0) {
            DefaultBackstackReducer(snapshot, BackstackAction.PopTo(action.route::class, inclusive = false))
        } else {
            DefaultBackstackReducer(snapshot, action)
        }
    } else {
        DefaultBackstackReducer(snapshot, action)
    }
}

val navigator = rememberNavigator(root = Home, reducer = SingleTopReducer)
```

---

## Disabling Telemetry

Pass `NavixTelemetry.NoOp` to produce zero overhead when telemetry is off:

```kotlin
val navigator = rememberNavigator(
    root = Home,
    telemetry = if (analyticsEnabled) myPipeline else NavixTelemetry.NoOp,
)
```

For a stable reference that can switch at runtime without recreating the navigator, use a
delegating wrapper:

```kotlin
val telemetry = object : NavixTelemetry {
    override fun onEvent(event: NavEvent) {
        if (analyticsEnabled.value) pipeline.onEvent(event)
    }
}
```

---

## Multiple Deep Link Handlers

Register handlers in priority order — the first match wins:

```kotlin
val navigator = rememberNavigator(
    root = Home,
    deepLinkHandlers = listOf(
        ProductDeepLinkHandler(),   // navix://product/{id}
        ProfileDeepLinkHandler(),   // navix://profile
    ),
)
```

Trigger deep links programmatically from inside the app:

```kotlin
navigator.handleDeepLink("navix://product/42")
```

---

## KMP Support

The navigation state machine (`BackstackStore`, `NavigatorImpl`, `BackstackReducer`) is in
`commonMain` and compiles without the Android SDK. The Compose layer (`NavixHost`,
`rememberNavigator`) lives in `androidMain`.

Future non-Android KMP targets (Desktop, iOS via Compose Multiplatform) are supported by
providing a platform-specific `NavixHost` equivalent.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for module responsibilities, test requirements,
and architecture invariants that every PR must respect.

---

## License

```
Copyright 2026 Navix Contributors

Licensed under the Apache License, Version 2.0
```

See [LICENSE](LICENSE) for the full license text.
