import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultOperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    application
    id("com.saveourtool.save.buildutils.kotlin-library")
}

kotlin {
    val os = getCurrentOperatingSystem()

    jvm()

    registerNativeBinaries(os, this)

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.okio)
            }
        }

        val commonNonJsMain by getting {
            dependencies {
                implementation(projects.saveCore)
                implementation(projects.saveCommon)
                implementation(libs.kotlinx.serialization.properties)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(projects.saveCommon)
                implementation(projects.saveReporters)
                implementation(projects.savePlugins.fixPlugin)
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter.engine)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }

    linkProperExecutable(os)

    tasks.withType<Test>().configureEach {
        dependsOn(":save-core:downloadTestResources")
    }
}

application {
    mainClass.set("com.saveourtool.save.cli.SaveCliRunnerKt")
}

/**
 * @param os
 * @param kotlin
 * @throws GradleException
 */
fun registerNativeBinaries(os: DefaultOperatingSystem, kotlin: KotlinMultiplatformExtension) {
    val saveTarget = when {
        os.isWindows -> kotlin.mingwX64()
        os.isLinux -> kotlin.linuxX64()
        os.isMacOsX -> kotlin.macosX64()
        else -> throw GradleException("Unknown operating system $os")
    }

    configure(listOf(saveTarget)) {
        binaries {
            val name = "save-${project.version}-${this@configure.name}"
            executable {
                this.baseName = name
                entryPoint = "com.saveourtool.save.cli.main"
            }
        }
    }
}

/**
 * @param os
 * @throws GradleException
 */
fun linkProperExecutable(os: DefaultOperatingSystem) {
    val linkReleaseExecutableTaskProvider = when {
        os.isLinux -> tasks.getByName("linkReleaseExecutableLinuxX64")
        os.isWindows -> tasks.getByName("linkReleaseExecutableMingwX64")
        os.isMacOsX -> tasks.getByName("linkReleaseExecutableMacosX64")
        else -> throw GradleException("Unknown operating system $os")
    }
    project.tasks.register("linkReleaseExecutableMultiplatform") {
        dependsOn(linkReleaseExecutableTaskProvider)
    }

    // disable building of some binaries to speed up build
    // possible values: `all` - build all binaries, `debug` - build only debug binaries
    val enabledExecutables = if (hasProperty("enabledExecutables")) property("enabledExecutables") as String else null
    if (enabledExecutables != null && enabledExecutables != "all") {
        linkReleaseExecutableTaskProvider.enabled = false
    }

    // Integration test should be able to have access to binary during the execution. Also we use here the debug version,
    // in aim to have ability to run it in CI, which operates only with debug versions
    tasks.getByName("jvmTest").dependsOn(tasks.getByName(
        when {
            os.isLinux -> "linkDebugExecutableLinuxX64"
            os.isWindows -> "linkDebugExecutableMingwX64"
            os.isMacOsX -> "linkDebugExecutableMacosX64"
            else -> throw GradleException("Unknown operating system $os")
        }
    ))
}

application {
    mainClass.set("com.saveourtool.save.cli.SaveCliRunnerKt")
}
