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

    implementation(libs.bundles.viastack)
    implementation(libs.netminecraft) {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation(libs.raknet) {
        exclude(group = "io.netty")
    }

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
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("plugin.yml") {
        expand("version" to pluginVersion)
    }
}
