pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases/") { name = "KikuGie Releases" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val fabricOnly = listOf(
    "1.14.4", "1.15", "1.15.1", "1.15.2",
    "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5",
    "1.17", "1.17.1", "1.18", "1.18.1", "1.18.2",
    "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
    "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5",
)

val fabricAndNeoforge = listOf(
    "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5",
    "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
    "26.1", "26.1.1", "26.1.2", "26.2", "26.3",
)

val ideSync = System.getProperty("idea.sync.active") == "true"
val activeProject = Regex("""stonecutter active "([^"]+)"""")
    .find(file("stonecutter.gradle.kts").readText())?.groupValues?.get(1)

stonecutter {
    create(rootProject) {
        fun mc(version: String, vararg loaders: String) = loaders.forEach {
            val name = "$version-$it"
            if (!ideSync || activeProject == null || name == activeProject) {
                version(name, version).buildscript = "build.$it.gradle.kts"
            }
        }
        fabricOnly.forEach { mc(it, "fabric") }
        fabricAndNeoforge.forEach { mc(it, "fabric", "neoforge") }
        vcsVersion = if (ideSync && activeProject != null) activeProject else "26.3-fabric"
    }
}

rootProject.name = "Always Sprint"
