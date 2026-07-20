rootProject.name = "Reactor"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.powernukkitx.org/releases")
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://repo.opencollab.dev/maven-snapshots")
    }
}

include("api")
include("plugin")
