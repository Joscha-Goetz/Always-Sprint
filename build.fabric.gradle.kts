import java.net.HttpURLConnection
import java.net.URI

plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

val loader = "fabric"
version = "${property("mod.version")}-${sc.current.version}-$loader"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val ctNamespace = if (sc.current.parsed >= "26.1") "official" else "named"
val ctHeader = Regex("""^(classTweaker|accessWidener) v(\d) \w+""")

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

val prereleaseMap: Map<String, List<String>> = rootProject.layout.projectDirectory.file("mc-prereleases.json").let { file ->
    if (!file.asFile.isFile) emptyMap()
    else {
        @Suppress("UNCHECKED_CAST")
        groovy.json.JsonSlurper().parseText(providers.fileContents(file).asText.get()) as Map<String, List<String>>
    }
}

val modrinthVersions: List<String> =
    (compatibleVersions + compatibleVersions.flatMap { prereleaseMap[it].orEmpty() }).distinct()

fun withPrereleases(range: String): String {
    val parts = range.trim().split(Regex("\\s+"))
    return if (parts.size == 1 && parts[0].first().isDigit()) ">=${parts[0]}- <=${parts[0]}"
    else parts.joinToString(" ") { if (it.startsWith(">=") && !it.endsWith("-")) "$it-" else it }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
        accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/always-sprint.ct"),
        "build/processed.ct"
        ).also { file ->
            file.writeText(file.readText().replaceFirst(ctHeader, "$1 v$2 $ctNamespace"))
        }

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {

    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
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
    file = loomx.modJar.flatMap { it.archiveFile }
    version = project.version.toString()
    displayName = project.version.toString()
    changelog = "Now also works on snapshots, pre-releases and release candidates of each supported Minecraft version"
    type = STABLE
    modLoaders.add("fabric")
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
        minecraftVersions.addAll(modrinthVersions)
    }
}


tasks {
    processResources {
        exclude("META-INF/neoforge.mods.toml")
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val fullVersion = project.version.toString()
        inputs.property("fullVersion", fullVersion)

        val rawCompat: String = sc.properties["mod.mc_compat"]
        val mcCompat = withPrereleases(rawCompat)
        inputs.property("minecraft", mcCompat)

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            put("minecraft", mcCompat)
            put("version", fullVersion)
        }

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        val ns = ctNamespace
        val headerPattern = ctHeader.pattern
        inputs.property("ctNamespace", ns)
        filesMatching("*.ct") {
            filter { line -> line.replaceFirst(Regex(headerPattern), "$1 v$2 $ns") }
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}

val publishVersion = project.version.toString()

tasks.named("publishModrinth") {
    val version = publishVersion
    val token = providers.gradleProperty("modrinthToken")
    onlyIf("Version noch nicht auf Modrinth") {
        val conn = URI("https://api.modrinth.com/v2/project/always-sprint/version/$version")
            .toURL().openConnection() as HttpURLConnection
        token.orNull?.let { conn.setRequestProperty("Authorization", it) }
        conn.setRequestProperty("Cache-Control", "no-cache")
        val code = conn.responseCode
        conn.disconnect()
        code != 200
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
tasks.register("printModrinthVersions") {
    group = "publishing"
    val versions = modrinthVersions
    doLast { println(versions.joinToString(", ")) }
}
