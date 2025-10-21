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
    alias(sphereonplug.plugins.com.android.library)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose)
    alias(sphereonplug.plugins.io.kotest.multiplatform.io.kotest.multiplatform.gradle.plugin)
    alias(sphereonplug.plugins.sphereon.gradle.plugin.project.publication)
    id("maven-publish")
//    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

//    jvm("desktop")

    sourceSets {
//        val desktopMain by getting

        androidMain.dependencies {
            implementation(sphereonlib.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtimeSaveable)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
//            implementation(libs.bundles.kotlin.inject)
            implementation(libs.amz.app.platform.presenter.molecule.public)
            implementation(libs.amz.app.platform.presenter.molecule.impl)
            implementation(libs.amz.app.platform.renderer.compose.public)
            implementation(libs.amz.kotlin.inject.contribute.public)
            implementation(libs.sphereon.core.api.public)
            implementation(libs.sphereon.core.logger.mobile)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.core)
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.cbor)
            implementation(sphereonlib.dev.whyoleg.cryptography.core)
            implementation(libs.amz.app.platform.presenter.molecule.public)
            implementation(libs.collection)
        }
        commonTest.dependencies {
            implementation(sphereonlib.org.jetbrains.kotlin.test)
        }
//        desktopMain.dependencies {
//            implementation(compose.desktop.currentOs)
//        }
    }
    /* ksp {
         // We are using the Amazon App Platform binding processor instead!
         arg("software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor", "disabled")
     }

     dependencies {
         addProvider("ksp", libs.kotlin.inject.compiler.ksp)
         add("ksp", libs.amz.kotlin.inject.contribute.public)
         add("ksp", libs.amz.kotlin.inject.contribute.code.generators)
         add("ksp", libs.anvil.compiler.ksp)

         addProvider("kspDesktop", libs.kotlin.inject.compiler.ksp)
         add("kspDesktop", libs.amz.kotlin.inject.contribute.public)
         add("kspDesktop", libs.amz.kotlin.inject.contribute.code.generators)
         add("kspDesktop", libs.anvil.compiler.ksp)

         addProvider("kspAndroid", libs.kotlin.inject.compiler.ksp)
         add("kspAndroid", libs.amz.kotlin.inject.contribute.public)
         add("kspAndroid", libs.amz.kotlin.inject.contribute.code.generators)
         add("kspAndroid", libs.anvil.compiler.ksp)

         addProvider("kspAndroidDebug", libs.kotlin.inject.compiler.ksp)
         add("kspAndroidDebug", libs.amz.kotlin.inject.contribute.public)
         add("kspAndroidDebug", libs.amz.kotlin.inject.contribute.code.generators)
         add("kspAndroidDebug", libs.anvil.compiler.ksp)

         addProvider("kspAndroidTest", libs.kotlin.inject.compiler.ksp)
         add("kspAndroidTest", libs.amz.kotlin.inject.contribute.public)
         add("kspAndroidTest", libs.amz.kotlin.inject.contribute.code.generators)
         add("kspAndroidTest", libs.anvil.compiler.ksp)



     }*/
}

android {
    namespace = "com.sphereon.kiwa.sample.ui.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
    }

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
