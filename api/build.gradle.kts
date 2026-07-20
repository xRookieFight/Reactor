plugins {
    id("java-library")
}

dependencies {
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.powernukkitx)
    compileOnly(libs.bundles.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
