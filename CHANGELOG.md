# Changelog

All notable changes to Navix are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Navix follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- **Per-entry state restoration.** `rememberSaveableNavigator` now persists each entry's
  `ViewModel` `SavedStateHandle` and screen `rememberSaveable` state across process death
  (previously only the backstack / route arguments survived). Entry-scoped `ViewModel`
  instances now also survive configuration changes (host-Activity-scoped store) instead of
  being recreated on every rotation.
- `rememberSaveableNavixMultiStack` — saveable multi-stack that persists the active tab
  index, every tab's backstack, and per-entry state. Additive; existing
  `rememberNavixMultiStack` is unchanged.
- Initial architecture and module scaffold
- `contracts` module: `Route`, `RouteEntry`, `BackstackSnapshot`, `NavEvent`, `NavLifecycleState`, `NavixTelemetry`
- `navix-annotations`: `@RouteDestination`
- `navix-runtime`: `BackstackReducer` (pure reducer), `BackstackStore`, `NavigatorImpl`, `NavixHost` (Compose), `NavTransitionSpec`, `NavTransitions`, `rememberNavigator`
- `navix-telemetry`: `NavixTelemetryPipeline`, `NavEventExporter`, `LogcatExporter`, `NoOpExporter`
- `navix-compiler`: KSP processor for route discovery, deep link template parsing, `RouteRegistryGenerator`, `DeepLinkHandlerGenerator`
- `navix-testing`: `FakeNavigator` with assertion helpers, `NavixTestRule` for Compose tests
- `navix-devtools`: `NavixDevToolsOverlay`, `BackstackInspectorPanel`, `EventTimelinePanel`
- `navix-demo-app`: Auth, Home, Product detail, Profile, Settings, Telemetry viewer screens
- 10 Architecture Decision Records in `docs/adr/`
- Apache 2.0 license

### Changed
- The `rememberSaveableNavigator` saved-state blob format changed from a raw `ByteArray`
  to a combined `Bundle` envelope (backstack bytes + per-entry registry bundles in one
  atomically-saved blob). **No source changes required for callers** — the public
  signature is unchanged. A blob written by an older Navix version is not readable by the
  new restore path and degrades gracefully to a fresh navigator at `root` (same fallback
  as a serializer schema mismatch); this only affects a process-death restore that spans
  an in-place app update mid-session.

---
