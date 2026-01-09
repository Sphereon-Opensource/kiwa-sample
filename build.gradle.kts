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

@file:Suppress("UnstableApiUsage")


allprojects {
    group = "com.sphereon.kiwa.sample"
    version = "0.13.1-SNAPSHOT"

    plugins.withType<MavenPublishPlugin> {
        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "sphereon"
                    val snapshotsUrl = "https://nexus.sphereon.com/repository/sphereon-opensource-snapshots/"
                    val releasesUrl = "https://nexus.sphereon.com/repository/sphereon-opensource-releases/"
                    url = uri(if (version.toString().contains("SNAPSHOT")) snapshotsUrl else releasesUrl)
                    credentials {
                        username = System.getenv("NEXUS_USERNAME")
                        password = System.getenv("NEXUS_PASSWORD")
                    }
                }
            }
        }
    }

}

plugins {
    alias(sphereonplug.plugins.com.android.library) apply false
    alias(sphereonplug.plugins.com.android.application) apply false
    alias(sphereonplug.plugins.org.jetbrains.kotlin.multiplatform) apply false
    alias(sphereonplug.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(sphereonplug.plugins.com.vanniktech.maven.publish) apply false
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.serialization) apply false
    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin) apply false
    alias(sphereonplug.plugins.org.jetbrains.kotlin.android) apply false
    alias(sphereonplug.plugins.dev.petuska.npm.publish.dev.petuska.npm.publish.gradle.plugin) apply false
    alias(sphereonplug.plugins.software.amazon.app.platform) apply false
    alias(sphereonplug.plugins.org.jetbrains.kotlinx.atomicfu) apply false
    alias(sphereonplug.plugins.com.sphereon.gradle.plugin.conventions) apply false
    alias(sphereonplug.plugins.com.sphereon.gradle.plugin.integration.tests) apply false
    alias(sphereonplug.plugins.com.sphereon.gradle.plugin.project.publication) apply false
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.compose) apply false
    alias(sphereonplug.plugins.org.jetbrains.compose) apply false
    alias(sphereonplug.plugins.org.jetbrains.compose.hot.reload) apply false
    alias(libs.plugins.dokka) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}


val detektVersion = libs.versions.detekt.get()
val detektIncluded = setOf(""
  /*  ":example:holder:app:kiwa-example-holder-app-composeApp",
    ":example:holder:ui:auth:kiwa-example-holder-ui-auth-public"    ,
    ":example:holder:ui:auth:kiwa-example-holder-ui-auth-impl",
    ":example:holder:ui:core:kiwa-example-holder-ui-core-public",
    ":example:holder:ui:core:kiwa-example-holder-ui-core-impl",
    ":example:holder:ui:card:kiwa-example-holder-ui-card-public",
    ":example:holder:ui:card:kiwa-example-holder-ui-card-impl",
    ":example:holder:ui:elicense:kiwa-example-holder-ui-elicense-public",
    ":example:holder:ui:elicense:kiwa-example-holder-ui-elicense-impl",*/
)


subprojects {
    apply(plugin = "com.sphereon.gradle.plugin.conventions")
    apply(plugin = "io.gitlab.arturbosch.detekt")



    val isIncluded = (path in detektIncluded) /*|| !skipByProperty*/

    if (isIncluded) {
        // Apply Detekt only to non-excluded modules
        apply(plugin = "io.gitlab.arturbosch.detekt")

        dependencies {
            // Optional: ktlint rules via detekt-formatting
            "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
        }

        // Detekt extension configuration
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            // Point to a shared config file in the repo:
            // e.g. root/config/detekt/detekt.yml
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))

            // Per-module baseline (create with :module:detektBaseline if you want)
            baseline = file("$projectDir/detekt-baseline.xml")

            // Keep false in CI. You can override locally with -PdetektAutoCorrect=true
            val autoCorrectProp = (findProperty("detektAutoCorrect") as? String)?.toBoolean() ?: false
            autoCorrect = autoCorrectProp
        }

        // Configure all Detekt tasks in this module
        tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            // Match your toolchain/targets
            jvmTarget = "17"

            // KMP modules often place generated sources under various dirs—exclude them
            setSource(files(projectDir))
            include("**/*.kt", "**/*.kts")
            exclude(
                "**/build/**",
                "**/generated/**",
                "**/build/generated/**",
                // Common OpenAPI generator outputs (tweak to your repo)
                "**/build/openapi*/**",
                "**/src/**/kotlin-gen/**",
                "**/src/**/java-gen/**"
            )

            reports {
                // Enable what you need for CI/code scanning
                xml.required.set(true)
                html.required.set(true)
                sarif.required.set(false)
                txt.required.set(false)
                md.required.set(false)
            }
        }

        // Make `./gradlew check` run Detekt too for these modules
        tasks.matching { it.name == "check" }.configureEach {
            dependsOn(tasks.named("detekt"))
        }
    }
}


// Aggregate task at the root to lint everything that actually has Detekt enabled
tasks.register("detektAll") {
    group = "verification"
    description = "Run Detekt on all non-excluded subprojects"
    dependsOn(
        subprojects.flatMap { sub ->
            sub.tasks.matching { it.name == "detekt" }.toList()
        }
    )
}

tasks.register("detektFixAll") {
    group = "verification"
    description = "Run Detekt with autoCorrect enabled to fix issues in all non-excluded subprojects"

    // Configure autoCorrect at configuration time instead of execution time
    subprojects.forEach { sub ->
        sub.tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            // Only set autoCorrect when this specific task is being executed
            val isDetektFixAllTask = gradle.startParameter.taskNames.any {
                it.contains("detektFixAll") || it.endsWith(":detektFixAll")
            }
            if (isDetektFixAllTask) {
                autoCorrect = true
            }
        }
    }

    dependsOn(
        subprojects.flatMap { sub ->
            sub.tasks.matching { it.name == "detekt" }.toList()
        }
    )
}
