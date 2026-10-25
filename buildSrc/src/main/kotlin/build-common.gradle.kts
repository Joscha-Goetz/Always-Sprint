import java.net.HttpURLConnection
import java.net.URI

// Shared configuration for all loaders.
// Applied via plugins { id("build-common") } in build.<loader>.gradle.kts.
// Expects Stonecutter node names in the form "<minecraft version>-<loader>".
// Provides "requiredJava" in extra.

plugins {
    java
}

val loader = project.name.substringAfterLast('-')
val minecraftVersion = project.name.substringBeforeLast('-')

// Compares plain release versions like "1.20.5" or "26.1" segment by segment
fun minecraftAtLeast(target: String): Boolean {
    val current = minecraftVersion.split('.').map { it.toInt() }
    val other = target.split('.').map { it.toInt() }
    for (i in 0 until maxOf(current.size, other.size)) {
        val a = current.getOrElse(i) { 0 }
        val b = other.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return true
}

val requiredJava: JavaVersion = when {
    minecraftAtLeast("26.1") -> JavaVersion.VERSION_25
    minecraftAtLeast("1.20.5") -> JavaVersion.VERSION_21
    minecraftAtLeast("1.18") -> JavaVersion.VERSION_17
    minecraftAtLeast("1.17") -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
extra["requiredJava"] = requiredJava

// Version and jar base name
version = "${property("mod.version")}-$minecraftVersion-$loader"
base.archivesName = property("mod.id").toString()

// Java compatibility and toolchain
java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

// Skip publishing if this exact version was already uploaded
val publishVersion = version.toString()
val jarFileName = "${property("mod.id")}-$publishVersion.jar"
val modrinthId = property("publish.modrinth_id").toString()
val curseforgeId = property("publish.curseforge_id").toString()

tasks.matching { it.name == "publishModrinth" }.configureEach {
    val checkVersion = publishVersion
    val projectId = modrinthId
    val token = providers.gradleProperty("modrinthToken")
    onlyIf("Version not yet on Modrinth") {
        val connection = URI("https://api.modrinth.com/v2/project/$projectId/version/$checkVersion")
            .toURL().openConnection() as HttpURLConnection
        token.orNull?.let { connection.setRequestProperty("Authorization", it) }
        connection.setRequestProperty("Cache-Control", "no-cache")
        val code = connection.responseCode
        connection.disconnect()
        code != 200
    }
}

tasks.matching { it.name == "publishCurseforge" }.configureEach {
    val fileName = jarFileName
    val projectId = curseforgeId
    onlyIf("Version not yet on CurseForge") {
        val alreadyUploaded = runCatching {
            URI("https://api.cfwidget.com/$projectId").toURL().readText().contains(fileName)
        }.getOrDefault(false)
        !alreadyUploaded
    }
}