/*
 * © 2025 Sphereon International B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

rootProject.name = "kiwa-examples"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://aws.oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://nexus.sphereon.com/repository/sphereon-opensource-snapshots")
        }
        maven {
            url = uri("https://nexus.sphereon.com/repository/sphereon-opensource-releases")
        }
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenLocal()
    }
}



dependencyResolutionManagement {
    versionCatalogs {
        create("sphereonplug") {
            from("com.sphereon.gradle:gradle-plugin-bom:0.0.7-SNAPSHOT@toml")
        }
        create("sphereonlib") {
            from("com.sphereon.gradle:library-bom:0.0.7-SNAPSHOT@toml")
        }

    }
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://aws.oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            url = uri("https://nexus.sphereon.com/repository/sphereon-opensource-snapshots")
        }
        maven {
            url = uri("https://nexus.sphereon.com/repository/sphereon-opensource-releases")
        }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

}



plugins {
    id("com.gradle.develocity") version ("4.0.2")
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")

}


develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
        termsOfUseAgree = "yes"
    }
}

// We import the gradle projects below libraries automatically

println("Kiwa Examples:")
val librariesRoot = file("example")
librariesRoot
    .walkTopDown()
    .maxDepth(6)
    .filter { it.isDirectory }
    .filter { it.name != "generated" && it.name != "build" }
    .filter {
        File(it, "build.gradle.kts").exists()
    }
    .forEach { moduleDir ->
        // Derive a project path like ":libraries:core:api" from libraries/core/api
        val projectPath = moduleDir
            .relativeTo(rootDir)
            .invariantSeparatorsPath
            .split("/")
            .joinToString(":", prefix = ":")
        val name = projectPath.replace(":example", "kiwa-example").replace(":", "-")
        include(projectPath)
        project(projectPath).projectDir = moduleDir
        project(projectPath).name = name
        println("  $projectPath - $name")
    }
