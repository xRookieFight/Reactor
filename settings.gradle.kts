rootProject.name = "Reactor"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.powernukkitx.org/releases")
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://repo.opencollab.dev/maven-snapshots")
        maven("https://repo.viaversion.com")
        maven("https://maven.lenni0451.net/everything")
        maven("https://libraries.minecraft.net")
        maven("https://jitpack.io")
        maven("https://storage.okaeri.eu/repository/maven-public/")
    }
}

include("api")
include("plugin")
