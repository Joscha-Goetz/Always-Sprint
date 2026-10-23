import java.net.HttpURLConnection
import java.net.URI

plugins {
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

val loader = "neoforge"
val neoModId = (property("mod.id") as String).replace('-', '_')
version = "${property("mod.version")}-${sc.current.version}-$loader"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion =
    if (sc.current.parsed >= "26.1") JavaVersion.VERSION_25 else JavaVersion.VERSION_21

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

val mcRange: String =
    if (compatibleVersions.size <= 1) "[${sc.current.version}]"
    else "[${compatibleVersions.first()},${compatibleVersions.last()}]"

val neoforgeVersion: String = sc.properties["deps.neoforge"]

neoForge {
    version = neoforgeVersion
    mods {
        register(neoModId) {
            sourceSet(sourceSets.main.get())
        }
    }
    runs {
        register("client") {
            client()
            gameDirectory.set(rootProject.file("run"))
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

publishMods {
    file = tasks.jar.flatMap { it.archiveFile }
    version = project.version.toString()
    displayName = project.version.toString()
    changelog = "Now also available for NeoForge"
    type = STABLE
    modLoaders.add("neoforge")
    dryRun = providers.gradleProperty("publishDryRun").isPresent

    curseforge {
        accessToken = providers.gradleProperty("curseforgeToken")
        projectId = "1761202"
        minecraftVersions.addAll(compatibleVersions)
        client = true
        server = false
    }

    modrinth {
        accessToken = providers.gradleProperty("modrinthToken")
        projectId = "p6841kf7"
        minecraftVersions.addAll(compatibleVersions)
    }
}

tasks.processResources {
    exclude("fabric.mod.json", "*.ct")

    val props = mapOf(
        "id" to neoModId,
        "name" to (project.property("mod.name") as String),
        "version" to project.version.toString(),
        "minecraft" to mcRange,
        "iconKey" to (if (sc.current.parsed >= "26.2") "iconFile" else "logoFile"),
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

    val mixinJava = "JAVA_${requiredJava.majorVersion}"
    filesMatching("*.mixins.json") { expand("java" to mixinJava) }
}

val publishVersion = project.version.toString()

tasks.named("publishModrinth") {
    val version = publishVersion
    onlyIf("Version noch nicht auf Modrinth") {
        val json = URI("https://api.modrinth.com/v2/project/always-sprint/version").toURL().readText()
        !json.contains("\"version_number\":\"$version\"")
    }
}

tasks.named("publishCurseforge") {
    val fileName = "always-sprint-$publishVersion.jar"
    onlyIf("Version noch nicht auf CurseForge") {
        val onCurseforge = runCatching {
            URI("https://api.cfwidget.com/1761202").toURL().readText().contains(fileName)
        }.getOrDefault(false)
        !onCurseforge
    }
}
