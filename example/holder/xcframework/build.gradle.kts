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

@file:OptIn(KspExperimental::class, ExperimentalKotlinGradlePluginApi::class)

import co.touchlab.skie.configuration.SealedInterop
import com.google.devtools.ksp.KspExperimental
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(sphereonplug.plugins.org.jetbrains.kotlin.multiplatform)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose)
    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin)
    id("co.touchlab.skie") version "0.10.8"
    `maven-publish`
}

skie {
    features {
        // Disable enum/sealed class wrapping for Indispensable due to Asn1Element/Asn1Sequence type hierarchy issues
        group("at.asitplus.signum.indispensable") {
            coroutinesInterop.set(false)
            SealedInterop.Enabled(false)
        }
        group("at.asitplus.signum") {
            coroutinesInterop.set(false)
            SealedInterop.Enabled(false)
        }

        group("Indispensable") {
            coroutinesInterop.set(false)
            SealedInterop.Enabled(false)
        }
    }
}

val xcFramework = XCFramework("KiwaSdk")

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KiwaSdk"
            isStatic = false
            freeCompilerArgs += listOf("-Xbinary=bundleId=com.sphereon.kiwa.sdk")
            xcFramework.add(this)
            export(libs.kiwa.holder.sdk.impl)
            export(libs.kiwa.holder.sdk.public)
            export(libs.sphereon.core.api.public)
            export(libs.sphereon.core.api.default)
            export(libs.sphereon.data.link.http.client.public)
            export(libs.sphereon.data.link.http.client.impl)
            export(libs.sphereon.core.logger.mobile)
            export(libs.sphereon.mdoc.core.public)
            export(libs.sphereon.mdoc.core.impl)
            export(libs.sphereon.mdoc.datatransfer.public)
            export(libs.sphereon.mdoc.datatransfer.impl)
            transitiveExport = false
        }
    }

    sourceSets {
        iosMain.dependencies {
            // Kiwa SDK Implementation
            api(libs.kiwa.holder.sdk.impl)
            api(libs.kiwa.holder.sdk.public)
            // Logger directly from the Sphereon Identity Development Kit as it is not exposed via the Kiwa SDK
            api(libs.sphereon.core.logger.mobile)
//            api(libs.sphereon.mdoc.transport.nfc)
            api(libs.sphereon.mdoc.transport.ble)
            api(libs.sphereon.mdoc.transport.restapi)
            api(libs.sphereon.mdoc.transport.oid4vp)
            api(libs.sphereon.mdoc.datatransfer.impl)
            api(libs.sphereon.core.api.public)
            api(libs.sphereon.core.api.default)
            api(libs.sphereon.data.link.http.client.impl)
            api(libs.sphereon.mdoc.core.impl)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.json)
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.cbor)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.core)
        }
    }
}

// Publishing configuration
group = "com.sphereon.kiwa"

// Create XCFramework zip for distribution
val xcframeworkZip by tasks.registering(Zip::class) {
    group = "publishing"
    description = "Creates a zip archive of the XCFramework for CocoaPods distribution"
    dependsOn("assembleKiwaSdkReleaseXCFramework")

    archiveFileName.set("kiwa-sdk-ios-${version}.zip")
    destinationDirectory.set(layout.buildDirectory.dir("cocoapods"))
    from(layout.buildDirectory.dir("XCFrameworks/release"))
    include("KiwaSdk.xcframework/**")
}

// Publishing configuration for Nexus repository
publishing {
    publications {
        create<MavenPublication>("xcframework") {
            groupId = "com.sphereon.kiwa"
            artifactId = "kiwa-sdk-ios"
            version = project.version.toString()

            artifact(xcframeworkZip) {
                extension = "zip"
            }

            pom {
                name.set("Kiwa SDK XCFramework")
                description.set("Kiwa SDK for digital credential management - iOS XCFramework")
                url.set(providers.gradleProperty("pod.homepage").getOrElse("https://sphereon.com"))
                licenses {
                    license {
                        name.set(providers.gradleProperty("pod.license").getOrElse("Proprietary"))
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "nexus"
            val isSnapshot = version.toString().endsWith("-SNAPSHOT")
            url = uri(
                if (isSnapshot) {
                    "https://nexus.sphereon.com/repository/kiwa-snapshots"
                } else {
                    "https://nexus.sphereon.com/repository/kiwa-releases"
                }
            )
            credentials {
                username = providers.environmentVariable("KIWA_REPO_USER")
                    .orElse(providers.gradleProperty("kiwaRepoUser"))
                    .orElse(providers.environmentVariable("NEXUS_USERNAME"))
                    .orNull
                password = providers.environmentVariable("KIWA_REPO_PASSWORD")
                    .orElse(providers.gradleProperty("kiwaRepoPassword"))
                    .orElse(providers.environmentVariable("NEXUS_PASSWORD"))
                    .orNull
            }
        }
    }
}

// Generate podspec file for CocoaPods distribution
val generatePodspec by tasks.registering {
    group = "publishing"
    description = "Generates a podspec file for CocoaPods distribution"

    val podspecFile = layout.buildDirectory.file("cocoapods/KiwaSdk.podspec")
    outputs.file(podspecFile)

    doLast {
        val isSnapshot = version.toString().endsWith("-SNAPSHOT")
        val nexusRepo = if (isSnapshot) {
            "https://nexus.sphereon.com/repository/kiwa-snapshots"
        } else {
            "https://nexus.sphereon.com/repository/kiwa-releases"
        }
        val xcframeworkUrl = "$nexusRepo/com/sphereon/kiwa/kiwa-sdk-ios/$version/kiwa-sdk-ios-$version.zip"

        podspecFile.get().asFile.parentFile.mkdirs()
        // Read configurable values from gradle.properties
        val podHomepage = providers.gradleProperty("pod.homepage").getOrElse("https://sphereon.com")
        val podLicense = providers.gradleProperty("pod.license").getOrElse("Proprietary")
        val podAuthor = providers.gradleProperty("pod.author").getOrElse("Sphereon")
        val podAuthorEmail = providers.gradleProperty("pod.authorEmail").getOrElse("dev@sphereon.com")

        podspecFile.get().asFile.writeText(
            """
            |Pod::Spec.new do |spec|
            |  spec.name         = 'KiwaSdk'
            |  spec.version      = '${version.toString().replace("-SNAPSHOT", ".snapshot")}'
            |  spec.summary      = 'Kiwa elicense SDK'
            |  spec.description  = <<-DESC
            |    Kotlin Multiplatform SDK for digital credential management,
            |    integrating with Sphereon IDK (Identity Development Kit) for
            |    ISO 18013-5, 18013-7 mDoc, OID4VP.
            |  DESC
            |  spec.homepage     = '$podHomepage'
            |  spec.license      = { :type => '$podLicense' }
            |  spec.author       = { '$podAuthor' => '$podAuthorEmail' }
            |  spec.source       = {
            |    :http => '$xcframeworkUrl',
            |    :type => 'zip',
            |    :headers => ['Authorization: Basic #{Base64.strict_encode64(ENV["KIWA_REPO_USER"] + ":" + ENV["KIWA_REPO_PASSWORD"])}']
            |  }
            |  spec.ios.deployment_target = '15.0'
            |  spec.vendored_frameworks = 'KiwaSdk.xcframework'
            |  spec.static_framework = false
            |  spec.requires_arc = true
            |  spec.swift_versions = ['5.9', '5.10', '6.0', '6.1', '6.2']
            |end
            """.trimMargin()
        )
        println("Generated podspec at: ${podspecFile.get().asFile.absolutePath}")
    }
}

// Task to prepare for CocoaPods publishing
val prepareCocoaPodsPublish by tasks.registering {
    group = "publishing"
    description = "Prepares the XCFramework and podspec for CocoaPods publishing"
    dependsOn("assembleKiwaSdkReleaseXCFramework", generatePodspec)
}

ksp {
    useKsp2.set(false)
    arg("software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor", "disabled")
}

fun DependencyHandlerScope.addKspDependencies(configName: String) {
    addProvider(configName, libs.kotlin.inject.compiler.ksp)
    add(configName, libs.amz.kotlin.inject.contribute.public)
    add(configName, libs.amz.kotlin.inject.contribute.code.generators)
    add(configName, libs.anvil.compiler.ksp)
}

dependencies {
    // KSP for generating DI components
    addKspDependencies("ksp")
    addKspDependencies("kspIosX64")
    addKspDependencies("kspIosArm64")
    addKspDependencies("kspIosSimulatorArm64")
}
