plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
}

stonecutter active "26.3"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
}

fun mcVersionKey(v: String) = v.split('.').map { it.toIntOrNull() ?: 0 }

val mcVersionOrder = Comparator<String> { a, b ->
    val x = mcVersionKey(a)
    val y = mcVersionKey(b)
    (0 until maxOf(x.size, y.size))
        .map { x.getOrElse(it) { 0 }.compareTo(y.getOrElse(it) { 0 }) }
        .firstOrNull { it != 0 } ?: 0
}

gradle.projectsEvaluated {
    val ordered = rootProject.subprojects
        .filter { it.tasks.findByName("publishModrinth") != null }
        .sortedWith(compareBy(mcVersionOrder) { it.name })

    for (taskName in listOf("publishModrinth", "publishCurseforge")) {
        ordered.zipWithNext { older, newer ->
            newer.tasks.named(taskName) { mustRunAfter(older.tasks.named(taskName)) }
        }
    }
}