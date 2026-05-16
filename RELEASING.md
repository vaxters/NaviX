# Releasing Navix to Maven Central

This document covers everything you need to do a Navix release end-to-end — from first-time
setup through tagging a version and verifying the artifacts on Maven Central.

---

## First-time setup

### 1. Register the `io.navix` namespace on Sonatype Central Portal

1. Create an account at <https://central.sonatype.com>.
2. Go to **Namespaces → Add namespace** and enter `io.navix`.
3. Sonatype will give you a DNS TXT record to add to your `navix.io` DNS. Add it and click
   **Verify namespace**.
4. Once verified, the namespace is permanently registered to your account.

### 2. Generate a User Token

User Tokens are the credentials used by the publish workflow. They are **not** your Sonatype
account password.

1. Log in at <https://central.sonatype.com>.
2. Go to **Account → User Token → Generate User Token**.
3. Copy the **username** and **password** shown — they are only displayed once.
4. In your GitHub repository go to **Settings → Secrets and variables → Actions** and add:
   - `MAVEN_CENTRAL_USERNAME` — the token username
   - `MAVEN_CENTRAL_PASSWORD` — the token password

### 3. Generate a GPG signing key

Maven Central requires every artifact to be GPG-signed.

```bash
# Generate a key (RSA 4096 recommended)
gpg --full-generate-key

# List keys to find the key ID (last 8 hex characters of the fingerprint)
gpg --list-secret-keys --keyid-format SHORT

# Upload your public key to the Ubuntu key server (required by Maven Central)
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>
```

### 4. Export the private key for CI

The CI workflow uses an **in-memory** key (no key ring file on disk).

```bash
# Export the ASCII-armored private key and base64-encode it for safe storage
gpg --export-secret-keys --armor <YOUR_KEY_ID> | base64 | tr -d '\n'
```

Copy the output. In GitHub repository go to **Settings → Secrets and variables → Actions**
and add:

| Secret name             | Value                                                    |
|-------------------------|----------------------------------------------------------|
| `SIGNING_KEY_ID`        | Last 8 characters of your GPG key fingerprint            |
| `SIGNING_KEY`           | Base64-encoded ASCII-armored private key (from above)    |
| `SIGNING_KEY_PASSWORD`  | Passphrase of the GPG key                               |

### 5. Fill in the TODO placeholders in build files

Before the first release, replace every `TODO` marker in `build.gradle.kts`:

```kotlin
// In the root build.gradle.kts subprojects block:
url.set("https://github.com/YOUR_ORG/navix")          // project homepage
connection.set("scm:git:git://github.com/YOUR_ORG/navix.git")
developerConnection.set("scm:git:ssh://git@github.com/YOUR_ORG/navix.git")

developers {
    developer {
        id.set("your-github-username")
        name.set("Your Real Name")
        email.set("you@example.com")
    }
}
```

---

## Changelog generation

Navix uses [git-cliff](https://git-cliff.org/) to generate changelog entries from
[Conventional Commits](https://www.conventionalcommits.org/). The configuration lives in
`cliff.toml` at the repo root.

### Commit message format

Write commit messages in the conventional format so git-cliff can categorise them:

```
<type>[optional scope]: <description>

feat: add predictive-back gesture support
fix: prevent double-pop on rapid back press
perf(backstack): reduce allocations in reducer hot path
docs: update NavixHost API reference
refactor(runtime): extract BackstackReducer into its own file
test: add FakeNavigator assertion for empty backstack
chore(release): prepare v0.2.0           ← skipped by git-cliff
```

Supported types and the CHANGELOG section they map to:

| Type       | Section       |
|------------|---------------|
| `feat`     | Added         |
| `fix`      | Fixed         |
| `perf`     | Performance   |
| `refactor` | Changed       |
| `docs`     | Documentation |
| `test`     | Testing       |
| `ci`       | CI            |
| `chore`    | Chore         |

### Install git-cliff

```bash
# macOS / Linux (Homebrew)
brew install git-cliff

# Cargo (any platform)
cargo install git-cliff
```

### Generate a changelog entry before tagging

```bash
# Preview the unreleased changes without writing to disk
git cliff --unreleased --tag v0.2.0

# Prepend the new section to CHANGELOG.md (review before committing)
git cliff --unreleased --tag v0.2.0 --prepend CHANGELOG.md
```

Commit the updated `CHANGELOG.md` as part of the release commit:

```bash
git add gradle.properties CHANGELOG.md README.md
git commit -m "chore(release): prepare v0.2.0"
```

The `publish.yml` workflow automatically extracts this section from `CHANGELOG.md` and
attaches it as the body of the GitHub Release created after a successful Maven Central publish.

---

## Releasing a new version

### Checklist before tagging

- [ ] All tests pass locally: `./gradlew check`
- [ ] `CHANGELOG.md` updated with the new version section
- [ ] `README.md` dependency snippets updated to the new version
- [ ] `VERSION_NAME` in `gradle.properties` set to the release version (no `-SNAPSHOT` suffix)
- [ ] No uncommitted changes: `git status`

### Bump the version

Edit `gradle.properties`:

```properties
VERSION_NAME=0.2.0   # ← change this
```

Commit:

```bash
git add gradle.properties CHANGELOG.md README.md
git commit -m "Release v0.2.0"
```

### Tag and push

```bash
git tag v0.2.0
git push origin main --tags
```

Pushing the tag triggers the `publish.yml` workflow automatically.

### Monitor the publish job

1. Go to **Actions → Publish to Maven Central** in your GitHub repository.
2. Wait for the job to complete (typically 10–20 minutes for all 7 modules).
3. On success, the job summary lists all published coordinates.

### Verify on Maven Central

The artifacts are usually searchable within 10–30 minutes:
<https://central.sonatype.com/search?q=io.navix>

You can also verify individual artifacts:
<https://repo1.maven.org/maven2/io/navix/>

### After the release

Bump the version to the next development snapshot in `gradle.properties`:

```properties
VERSION_NAME=0.3.0-SNAPSHOT
```

Commit and push:

```bash
git add gradle.properties
git commit -m "Prepare next development iteration (0.3.0-SNAPSHOT)"
git push origin main
```

---

## Publishing a dry run locally

To inspect artifacts without touching Maven Central:

```bash
./gradlew publishToMavenLocal
```

Artifacts are published to `~/.m2/repository/io/navix/`. Inspect the POMs, JARs, and
signatures before committing to a real release.

You can also trigger a dry run via the GitHub Actions UI:
**Actions → Publish to Maven Central → Run workflow → check "dry run"**.

---

## Modules published

| Artifact ID           | Description                                        |
|-----------------------|----------------------------------------------------|
| `navix-contracts`     | Shared data types (Route, RouteEntry, NavEvent, …) |
| `navix-annotations`   | @RouteDestination source annotation                |
| `navix-runtime`       | Backstack engine + Compose NavixHost               |
| `navix-compiler`      | KSP processor for route discovery / deep links     |
| `navix-telemetry`     | Pluggable navigation event pipeline                |
| `navix-devtools`      | Debug overlay (Android-only)                       |
| `navix-testing`       | FakeNavigator + Compose test helpers               |

The `navix-demo-app` module is an application and is **never published**.

---

## Troubleshooting

### `Could not find io.navix:navix-contracts` after publishing

Inter-module dependencies use `project(":contracts")` at build time but resolve to
`io.navix:navix-contracts` when consumed from Maven. This means all 7 artifacts must be
present on Maven Central before a consumer build can resolve them. Always publish all modules
together (the workflow does this).

### `401 Unauthorized` during publish

Your User Token may have expired or been revoked. Generate a new one at
<https://central.sonatype.com> and update the GitHub Secrets.

### Artifact stuck in "Pending" on Central Portal

Click **Publish** manually on the deployment at <https://central.sonatype.com/publishing/deployments>.
The Vanniktech plugin with `publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)` triggers
automatic release, but if it times out you can complete it in the UI.

### GPG signature verification failure

Make sure your public key is on the Ubuntu keyserver:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>
```
