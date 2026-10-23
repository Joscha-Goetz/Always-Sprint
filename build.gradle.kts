plugins {
    id("dev.kikugie.loom-back-compat")
    id("com.modrinth.minotaur")
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

modrinth {
    token = providers.gradleProperty("modrinthToken")
    projectId = "always-sprint"
    versionNumber = project.version.toString()
    versionType = "release"
    uploadFile = loomx.modJar.get()
    gameVersions.addAll(compatibleVersions)
    loaders.add("fabric")
    changelog = "Finally a modern up-to-date Version"
    debugMode = providers.gradleProperty("modrinthDebug").isPresent
}

tasks.named("modrinth") { dependsOn(loomx.modJar) }

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val fullVersion = project.version.toString()
        inputs.property("fullVersion", fullVersion)

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("minecraft", "mod.mc_compat")
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
