plugins {
    id("build-common")
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
}

val requiredJava = extra["requiredJava"] as JavaVersion

val neoModId = (property("mod.id") as String).replace('-', '_')

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

val mcRange: String =
    if (compatibleVersions.size <= 1) "[${sc.current.version}]"
    else "[${compatibleVersions.first()},${compatibleVersions.last()}]"

val neoforgeVersion: String = sc.properties["deps.neoforge"]

neoForge {
    version = neoforgeVersion
    sc.properties.rawOrNull("deps", "parchment")?.toString()?.split(":")?.let { (parchmentMc, parchmentDate) ->
        parchment {
            minecraftVersion = parchmentMc
            mappingsVersion = parchmentDate
        }
    }
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
        projectId = property("publish.curseforge_id").toString()
        minecraftVersions.addAll(compatibleVersions)
        client = true
        server = false
    }

    modrinth {
        accessToken = providers.gradleProperty("modrinthToken")
        projectId = property("publish.modrinth_id").toString()
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
