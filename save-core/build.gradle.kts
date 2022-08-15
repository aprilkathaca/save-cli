
import com.saveourtool.save.generation.argumentsConfigFilePath
import com.saveourtool.save.generation.generateConfigOptions
import com.saveourtool.save.generation.optionsConfigFilePath

import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("com.saveourtool.save.buildutils.kotlin-library")
    id("de.undercouch.download")
}

kotlin {
    sourceSets {
        val commonNonJsMain by getting {
            dependencies {
                implementation(projects.saveCommon)
                implementation(projects.saveReporters)
                api(libs.okio)
                implementation(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.properties)
                implementation(libs.kotlinx.cli)
                implementation(libs.ktoml.core)
                implementation(libs.ktoml.file)
                implementation(projects.savePlugins.fixPlugin)
                implementation(projects.savePlugins.fixAndWarnPlugin)
                implementation(projects.savePlugins.warnPlugin)
            }
        }
        val commonNonJsTest by getting {
            dependencies {
                implementation(projects.saveCommonTest)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

val generateConfigOptionsTaskProvider = tasks.register("generateConfigOptions") {
    inputs.file(optionsConfigFilePath())
    inputs.file(argumentsConfigFilePath())
    val generatedFile = File("$buildDir/generated/src/com/saveourtool/save/core/config/SaveProperties.kt")
    outputs.file(generatedFile)

    doFirst {
        generateConfigOptions(generatedFile)
    }
}
val generateVersionFileTaskProvider = tasks.register("generateVersionsFile") {
    inputs.property("project version", version.toString())
    val versionsFile = File("$buildDir/generated/src/com/saveourtool/save/core/config/Versions.kt")
    outputs.file(versionsFile)

    doFirst {
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package com.saveourtool.save.core.config

            internal const val SAVE_VERSION = "$version"

            """.trimIndent()
        )
    }
}
kotlin.sourceSets.getByName("commonNonJsMain") {
    kotlin.srcDir("$buildDir/generated/src")
}
tasks.withType<KotlinCompile<*>>().forEach {
    it.dependsOn(generateConfigOptionsTaskProvider)
    it.dependsOn(generateVersionFileTaskProvider)
}

tasks.register<Download>("downloadTestResources") {
    src(Versions.IntegrationTest.ktlintLink)
    overwrite(false)
    onlyIfModified(true)
    dest("../examples/kotlin-diktat")
    dependsOn("downloadTestResources2")
}

tasks.register<Download>("downloadTestResources2") {
    src(Versions.IntegrationTest.diktatLink)
    overwrite(false)
    onlyIfModified(true)
    dest("../examples/kotlin-diktat/diktat.jar")
}

tasks.withType<Test>().configureEach {
    dependsOn("downloadTestResources")
}
