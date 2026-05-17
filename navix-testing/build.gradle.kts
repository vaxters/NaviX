import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":navix-runtime"))
            api(project(":contracts"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // navix-compose provides NavixHost and NavGraphBuilder used in NavixTestRule.
            api(project(":navix-compose"))
            implementation(project.dependencies.platform(libs.compose.bom))
            implementation(libs.compose.ui.test.junit4)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
    sourceSets.commonMain.dependencies {
        implementation(kotlin("test"))
    }
}

android {
    namespace = "io.navix.testing"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        abortOnError = false
        htmlReport = true
        xmlReport = true
        warningsAsErrors = false
    }
}

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
            androidVariantsToPublish = listOf("release"),
        ),
    )

    pom {
        name.set("Navix Testing")
        description.set(
            "Test utilities for the Navix navigation platform: FakeNavigator with assertion " +
                "helpers, and Compose test rule integration for navigation-driven UI tests.",
        )
    }
}
