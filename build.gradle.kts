import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

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
    // Declared here (apply false) so Spotless classes are on the buildscript
    // classpath for the subprojects{} configuration block below.
    alias(libs.plugins.spotless) apply false
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
            "navix-lint"
        )
    )
}

// ---------------------------------------------------------------------------
// Subproject configuration — Spotless formatting + Maven Central publishing
// Spotless:  ./gradlew spotlessApply   auto-format | spotlessCheck   CI gate
// Publishing: applied automatically to every subproject with vanniktech plugin.
// Module-specific fields (name, description) are set in each module's build.gradle.kts.
// ---------------------------------------------------------------------------
subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            ktlint("1.5.0")
                .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
            target("src/**/*.kt")
        }
        kotlinGradle {
            ktlint("1.5.0")
                .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
            target("*.gradle.kts")
        }
    }

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
            signAllPublications()

            pom {
                url.set("https://github.com/vaxters/navix")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("vaxters")
                        name.set("Ilya Amialiuk")
                        email.set("vaxters@gmail.com")
                    }
                }

                scm {
                    url.set("https://github.com/vaxters/navix")
                    connection.set("scm:git:git://github.com/vaxters/navix.git")
                    developerConnection.set("scm:git:ssh://git@github.com/vaxters/navix.git")
                }
            }
        }
    }
}
