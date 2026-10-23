plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
    id("com.modrinth.minotaur") version "2.10.0" apply false
}

stonecutter active "26.3"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
}
