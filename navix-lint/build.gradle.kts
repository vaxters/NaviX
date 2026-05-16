plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.android.lint.api)
    compileOnly(libs.android.lint.checks)

    testImplementation(libs.android.lint.api)
    testImplementation(libs.android.lint.tests)
    testImplementation(kotlin("test"))
}

// Required by the Lint infrastructure to locate the IssueRegistry.
tasks.jar {
    manifest {
        attributes["Lint-Registry-v2"] = "io.navix.lint.NavixIssueRegistry"
    }
}

// Expose a single-JAR configuration so that `lintPublish(project(":navix-lint"))` in
// consuming modules resolves to exactly one file. AGP's prepareLintJarForPublish task
// fails if the default configuration contains more than one artifact (e.g. sources jar).
configurations {
    create("lintJar") {
        isCanBeConsumed = true
        isCanBeResolved = false
    }
}

artifacts {
    add("lintJar", tasks.jar)
}
