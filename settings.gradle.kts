rootProject.name = "save"
include("save-common")
include("save-core")
include("save-cli")
include("save-plugins:fix-and-warn-plugin")
include("save-plugins:fix-plugin")
include("save-plugins:warn-plugin")
include("save-reporters")
include("save-common-test")

plugins {
    id("com.gradle.enterprise") version "3.10"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.6.5"
}
gradleEnterprise {
    server = "http://ec2-18-207-131-223.compute-1.amazonaws.com"
    allowUntrustedServer = true // unless a trusted certificate is already configured in GE buildScan {
    buildScan {
        publishAlways()
        capture {
            isTaskInputFiles = true
        }
        isUploadInBackground = System.getenv("CI") == null // adjust to your CI provider }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/saveourtool/sarif4k")
            val gprUser: String? by settings
            val gprKey: String? by settings
            credentials {
                username = gprUser
                password = gprKey
            }
            content {
                includeGroup("io.github.detekt.sarif4k")
            }
        }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")