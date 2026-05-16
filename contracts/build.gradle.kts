import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vanniktech.publish)
}

kotlin {
    jvm()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            // kotlinx-serialization is `api` because Route is required to be @Serializable;
            // consumers will compose serializers against types declared here.
            api(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

mavenPublishing {
    // The module folder is named "contracts"; override to keep the io.navix:navix-* naming
    // convention consistent. Group and version come from gradle.properties (GROUP / VERSION_NAME).
    coordinates(artifactId = "navix-contracts")

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
        )
    )

    pom {
        name.set("Navix Contracts")
        description.set(
            "Shared data types and stable interfaces for the Navix navigation platform " +
                "(Route, RouteEntry, BackstackSnapshot, NavEvent)."
        )
    }
}
