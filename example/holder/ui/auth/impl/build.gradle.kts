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

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(sphereonplug.plugins.org.jetbrains.kotlin.multiplatform)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin)
    alias(sphereonplug.plugins.com.android.library)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose.hot.reload)
    alias(sphereonplug.plugins.io.kotest.multiplatform.io.kotest.multiplatform.gradle.plugin)
    alias(sphereonplug.plugins.sphereon.gradle.plugin.project.publication)
    id("maven-publish")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(sphereonlib.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(sphereonlib.org.jetbrains.androidx.lifecycle.viewmodel)
            implementation(sphereonlib.org.jetbrains.androidx.lifecycle.runtime.compose)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.core)
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.cbor)
            implementation(sphereonlib.dev.whyoleg.cryptography.core)
            implementation(libs.amz.app.platform.presenter.molecule.public)
            implementation(libs.amz.app.platform.renderer.compose.public)
            implementation(libs.amz.kotlin.inject.contribute.public)
//            implementation(libs.sphereon.core.api.default)
            api(libs.bundles.kotlin.inject)
            api(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthPublic)
            api(projects.example.holder.ui.core.kiwaExampleHolderUiCorePublic)
            api(projects.example.holder.ui.card.kiwaExampleHolderUiCardPublic)
            implementation("com.russhwolf:multiplatform-settings:1.3.0")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation("com.russhwolf:multiplatform-settings-coroutines:1.3.0")
            implementation("com.russhwolf:multiplatform-settings-datastore:1.3.0")
        }
        commonTest.dependencies {
            implementation(sphereonlib.org.jetbrains.kotlin.test)
            implementation(libs.amz.kotlin.inject.impl)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.swing)
        }
    }
}

android {
    namespace = "com.sphereon.kiwa.sample.ui.auth.impl"
    compileSdk = 36

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
//    implementation(project(":sdks:holder:example:ui:core:kiwa-holder-example-ui-core-public"))
    debugImplementation(compose.uiTooling)
}

ksp {
    // We are using the Amazon App Platform binding processor instead!
    arg("software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor", "disabled")
}

fun DependencyHandlerScope.addKspDependencies(configName: String) {
    val kspConfig = "ksp$configName"
    addProvider(kspConfig, libs.kotlin.inject.compiler.ksp)
    add(kspConfig, libs.amz.kotlin.inject.contribute.public)
    add(kspConfig, libs.amz.kotlin.inject.contribute.code.generators)
    add(kspConfig, libs.anvil.compiler.ksp)
}

dependencies {
    val kspConfigurations = listOf("Desktop", "Android", "AndroidDebug", "AndroidTest")
    kspConfigurations.forEach { configName ->
        addKspDependencies(configName)
    }
}
