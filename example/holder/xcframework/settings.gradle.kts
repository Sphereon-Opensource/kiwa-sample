/*
 * © 2026 Sphereon International B.V.
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

rootProject.name = "kiwa-sdk-xcframework"

pluginManagement {
    repositories {
        fun RepositoryHandler.privateKiwaRepo(urlStr: String) {
            maven {
                url = uri(urlStr)
                credentials {
                    username = providers.environmentVariable("KIWA_REPO_USER")
                        .orElse(providers.gradleProperty("kiwaRepoUser"))
                        .orElse(providers.environmentVariable("NEXUS_USERNAME"))
                        .orNull ?: throw GradleException("No KIWA_REPO_USER environment or kiwaRepoUser gradle property set to access the Kiwa repo")
                    password = providers.environmentVariable("KIWA_REPO_PASSWORD")
                        .orElse(providers.gradleProperty("kiwaRepoPassword"))
                        .orElse(providers.environmentVariable("NEXUS_PASSWORD"))
                        .orNull ?: throw GradleException("No KIWA_REPO_PASSWORD environment or kiwaRepoUser gradle property set to access the Kiwa repo")
                }
            }
        }
        // Do not remove the content part when maven local is at the top!
        // https://slack-chats.kotlinlang.org/t/27045384/hi-there-i-have-a-very-annoying-internal-compiler-error-here
        mavenLocal {
            content {
                includeGroupAndSubgroups("com.sphereon")
            }
        }
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
        privateKiwaRepo("https://nexus.sphereon.com/repository/kiwa-snapshots")
        privateKiwaRepo("https://nexus.sphereon.com/repository/kiwa-releases")

        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("sphereonplug") {
            from("com.sphereon.gradle:gradle-plugin-bom:0.5.0@toml")
        }
        create("sphereonlib") {
            from("com.sphereon.gradle:library-bom:0.5.0@toml")
        }
        create("libs") {
            from(files("../../../gradle/libs.versions.toml"))
        }
    }
    repositories {
        fun RepositoryHandler.privateKiwaRepo(urlStr: String) {
            maven {
                url = uri(urlStr)
                credentials {
                    username = providers.environmentVariable("KIWA_REPO_USER")
                        .orElse(providers.gradleProperty("kiwaRepoUser"))
                        .orElse(providers.environmentVariable("NEXUS_USERNAME"))
                        .orNull ?: throw GradleException("No KIWA_REPO_USER environment or kiwaRepoUser gradle property set to access the Kiwa repo")
                    password = providers.environmentVariable("KIWA_REPO_PASSWORD")
                        .orElse(providers.gradleProperty("kiwaRepoPassword"))
                        .orElse(providers.environmentVariable("NEXUS_PASSWORD"))
                        .orNull ?: throw GradleException("No KIWA_REPO_PASSWORD environment or kiwaRepoUser gradle property set to access the Kiwa repo")
                }
            }
        }
        // Do not remove the content part when maven local is at the top!
        // https://slack-chats.kotlinlang.org/t/27045384/hi-there-i-have-a-very-annoying-internal-compiler-error-here
        mavenLocal {
            content {
                includeGroupAndSubgroups("com.sphereon")
            }
        }
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
        privateKiwaRepo("https://nexus.sphereon.com/repository/kiwa-snapshots")
        privateKiwaRepo("https://nexus.sphereon.com/repository/kiwa-releases")
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
