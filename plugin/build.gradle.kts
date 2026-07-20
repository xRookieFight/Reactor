plugins {
    id("java")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(project(":api"))

    compileOnly(libs.powernukkitx)
    compileOnly(libs.bundles.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.shadowJar {
    archiveBaseName = "Reactor"
    archiveClassifier = ""
    archiveVersion = project.version.toString()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
