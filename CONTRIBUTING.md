# Contributing to Navix

Thank you for your interest in contributing to Navix! This guide explains how to get started,
what the codebase expectations are, and how to submit quality contributions.

---

## Development Setup

```bash
git clone https://github.com/navix/navix.git
cd navix
./gradlew assemble
```

Minimum requirements:
- JDK 17
- Android Studio Ladybug or newer
- Gradle 8.10+

---

## Module Guide

| Module | Responsibility | Who can modify |
|---|---|---|
| `contracts` | Shared data types — zero business logic | Architecture team + PR approval |
| `navix-annotations` | Source-retention annotations | Compiler team |
| `navix-runtime` | Backstack state machine + Compose layer | Runtime team |
| `navix-compiler` | KSP processor | Compiler team |
| `navix-telemetry` | Event pipeline + exporters | Telemetry team |
| `navix-devtools` | Debug overlay | DevTools team |
| `navix-testing` | Testing DSL + fakes | Testing team |
| `navix-demo-app` | Showcase application | Anyone |

**Adding a field to `contracts`** requires a PR description explaining the ABI impact and
migration path for existing consumers.

---

## Test Requirements

Every PR must include tests. Test coverage expectations by change type:

| Change type | Required tests |
|---|---|
| New `BackstackAction` variant | `BackstackReducerTest` cases for all edge states |
| New `Navigator` method | `NavigatorImplTest` covering success + failure paths |
| New `NavEventExporter` | `NavixTelemetryPipelineTest` fan-out verification |
| KSP compiler change | `DeepLinkTemplateParserTest` + compiler success/error tests |
| New Compose composable | Compose `ComposeTestRule` UI test |

Run all tests locally before submitting:
```bash
./gradlew check
```

---

## Code Style

- Kotlin standard style (ktlint enforced in CI)
- No comments unless the WHY is non-obvious
- No `TODO`/`FIXME` without an associated GitHub issue link
- No reflection — zero `Class.forName`, no `kotlin.reflect` in `commonMain`
- No Android SDK APIs in `commonMain` source sets

---

## Architecture Invariants

Before submitting, verify:
1. `contracts` has no business logic (no function bodies beyond computed properties)
2. `navix-runtime` `commonMain` compiles without the Android SDK on the classpath
3. `navix-devtools` is not added to any `implementation` dependency — only `debugImplementation`
4. `BackstackReducer` implementations have no side effects

---

## Submitting a Pull Request

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Write your change + tests
4. Run `./gradlew check` and ensure it passes
5. Open a PR against `main` with the provided template filled out

PRs that fail CI, lack tests, or modify `contracts` without an ABI analysis will not be merged.
