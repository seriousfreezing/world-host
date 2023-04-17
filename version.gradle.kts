import xyz.deftu.gradle.GameInfo.fetchMcpMappings
import xyz.deftu.gradle.GameInfo.fetchYarnMappings

plugins {
    java
    id("xyz.deftu.gradle.multiversion")
    id("xyz.deftu.gradle.tools")
    id("xyz.deftu.gradle.tools.blossom")
    id("xyz.deftu.gradle.tools.minecraft.loom")
    id("xyz.deftu.gradle.tools.minecraft.releases")
}

version = "${modData.version}+${mcData.versionStr}-${mcData.loader.name}"

repositories {
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
}

dependencies {
    if (mcData.version > 1_15_02) {
        @Suppress("UnstableApiUsage")
        mappings(loom.layered {
            officialMojangMappings()
            when {
                mcData.version >= 1_19_03 -> "1.19.3:2023.03.12"
                mcData.version >= 1_19_02 -> "1.19.2:2022.11.27"
                mcData.version >= 1_18_02 -> "1.18.2:2022.11.06"
                mcData.version >= 1_17_01 -> "1.17.1:2021.12.12"
                mcData.version >= 1_16_05 -> "1.16.5:2022.03.06"
                else -> null
            }?.let {
                parchment("org.parchmentmc.data:parchment-$it@zip")
            }
        })
    } else if (mcData.isForge) {
        mappings("de.oceanlabs.mcp:mcp_${fetchMcpMappings(mcData.version)}")
    } else if (mcData.isFabric) {
        mappings("net.fabricmc:yarn:${fetchYarnMappings(mcData.version)}")
    } else {
        mappings(loom.officialMojangMappings())
    }
}

val generatedResources = "$buildDir/generated-resources/main"
val langRel = "assets/world-host/lang"

sourceSets {
    main {
        output.dir(generatedResources, "builtBy" to "generateLangFiles")
    }
}

java {
    withSourcesJar()
}

tasks.register("generateLangFiles") {
    doLast {
        if (mcData.version >= 1_13_00) return@doLast
        val srcDir = File("$buildDir/resources/main/$langRel")
        if (!srcDir.exists()) return@doLast
        val destDir = File(generatedResources, langRel)
        destDir.mkdirs()
        for (file in srcDir.listFiles()!!) {
            @Suppress("UNCHECKED_CAST") val json = groovy.json.JsonSlurper().parse(file) as Map<String, String>
            destDir.resolve(file.name.substringBeforeLast('.') + ".lang").writer().use {
                json.forEach { (key, value) ->
                    it.write("$key=${value.replace("\n", "\\n")}\n")
                }
            }
        }
    }
}

tasks.jar {
    archiveBaseName.set(rootProject.name)
}

tasks.remapJar {
    archiveBaseName.set(rootProject.name)
}
