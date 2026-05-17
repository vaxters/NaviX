import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "io.navix.compose"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        // Emit native JVM default methods for Kotlin interface default bodies (e.g.
        // NavTransitionSpec.predictiveExit). Safe for minSdk 24+ which supports
        // interface default methods at the Android runtime level.
        freeCompilerArgs += "-Xjvm-default=all"
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests {
            // Robolectric + Compose UI test (StateRestorationTester) need real Android
            // resources and non-default stubs for the local JVM test task.
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    lint {
        abortOnError = false
        htmlReport = true
        xmlReport = true
        warningsAsErrors = false
    }
}

// Mark contracts types as stable so the Compose compiler skips unnecessary recompositions
// when BackstackSnapshot, RouteEntry, NavEvent, or NavTransitionKey are unchanged.
composeCompiler {
    stabilityConfigurationFiles.add(
        rootProject.layout.projectDirectory.file("navix-runtime/compose_stability.conf"),
    )
}

dependencies {
    // navix-runtime is api so consumers of navix-compose automatically get Navigator,
    // Reducer, EntryFactory, DeepLinkHandler, and all contracts types on their classpath.
    api(project(":navix-runtime"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.runtime)
    // Per-entry lifecycle/ViewModel/SavedState owners (Phase 4)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    // Per-entry SavedStateHandle: enableSavedStateHandles(), SavedStateViewModelFactory,
    // SAVED_STATE_REGISTRY_OWNER_KEY. Made explicit (not relied on transitively).
    implementation(libs.lifecycle.viewmodel.savedstate)
    // Dialog and ModalBottomSheet overlay destinations (Phase 8)
    implementation(libs.compose.material3)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    // State-restoration tests run on the JVM via Robolectric (SavedStateRegistry, Bundle)
    // and Compose's StateRestorationTester (process-death emulation).
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.lifecycle.viewmodel.savedstate)
}

mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        ),
    )

    pom {
        name.set("Navix Compose")
        description.set(
            "Jetpack Compose host, DSL, and transitions for the Navix navigation platform. " +
                "Provides NavixHost, rememberNavigator, NavTransitionSpec, and multi-stack support.",
        )
    }
}
