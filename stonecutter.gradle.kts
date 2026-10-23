plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
    id("de.joscha-goetz.publish-ordered") version "1.1.0"
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "neoforge")
}
