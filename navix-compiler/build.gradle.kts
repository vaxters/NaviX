import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.publish)
}

dependencies {
    implementation(project(":navix-annotations"))
    implementation(libs.ksp.api)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.kotlin.compile.testing.ksp)
}

// Register as a KSP symbol processor provider
tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Navix Compiler"
    }
}

mavenPublishing {
    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
        ),
    )

    pom {
        name.set("Navix Compiler")
        description.set(
            "KSP symbol processor for Navix: generates route registries and deep link " +
                "handler implementations from @RouteDestination annotations at build time.",
        )
    }
}
