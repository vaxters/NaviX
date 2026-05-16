import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    // Declared here (apply false) so the plugin classes are on the buildscript
    // classpath and the subprojects{} block below can configure the extension
    // without each subproject having to repeat shared POM metadata.
    alias(libs.plugins.vanniktech.publish) apply false
    // Applied to the root project; it scans every subproject's public API and
    // generates / validates api/<module>.api baselines.
    alias(libs.plugins.binary.compatibility.validator)
    // Static analysis — applied false here; wired into every subproject below.
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// ---------------------------------------------------------------------------
// Binary compatibility validator
// Tracks the published API surface of every Navix library module. Build-time
// modules (compiler, lint), the demo application, and source-retention
// annotation modules are excluded.
// ---------------------------------------------------------------------------
apiValidation {
    ignoredProjects.addAll(
        listOf(
            "navix-demo-app",
            "navix-compiler",
            "navix-lint",
        )
    )
}

// ---------------------------------------------------------------------------
// Shared Maven Central publishing configuration
// Applied automatically to every subproject that applies the vanniktech plugin.
// Module-specific fields (name, description, coordinates override) are set
// inside each module's own build.gradle.kts.
// ---------------------------------------------------------------------------
subprojects {
    // ── ktlint ───────────────────────────────────────────────────────────────
    // Applied unconditionally — every subproject is a Kotlin project. The shared
    // root baseline at config/ktlint/baseline.xml covers existing style violations
    // so the build stays green; only newly touched/added code must comply fully.
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<KtlintExtension> {
        // Per-project baseline: avoids parallel write races that would occur if all
        // modules shared a single root-level file. Each module's violations are
        // baselined independently; only newly touched code in this module must comply.
        baseline.set(file("${projectDir}/ktlint-baseline.xml"))
    }

    // ── detekt ───────────────────────────────────────────────────────────────
    // Static analysis with the same baseline strategy: existing violations are
    // baselined; new code introduced by this hardening pass (and going forward)
    // must not introduce new issues.
    apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<DetektExtension> {
        // Shared rule config; per-project baseline avoids parallel write races.
        config.setFrom(file("${rootProject.projectDir}/config/detekt/detekt.yml"))
        baseline = file("${projectDir}/detekt-baseline.xml")
        buildUponDefaultConfig = true
        parallel = true
    }

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

            // signAllPublications() is a no-op when signing properties are absent,
            // so local builds without GPG keys still work fine.
            signAllPublications()

            pom {
                // TODO: replace with actual GitHub URL once the repo is created
                url.set("https://github.com/TODO/navix")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        // TODO: fill in your real details before publishing
                        id.set("TODO")
                        name.set("TODO Your Name")
                        email.set("TODO your@email.com")
                    }
                }

                scm {
                    // TODO: replace with actual URLs once the repo is created
                    url.set("https://github.com/TODO/navix")
                    connection.set("scm:git:git://github.com/TODO/navix.git")
                    developerConnection.set("scm:git:ssh://git@github.com/TODO/navix.git")
                }
            }
        }
    }
}
