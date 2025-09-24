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
//    alias(sphereonplug.plugins.org.jetbrains.compose.hot.reload)
    alias(sphereonplug.plugins.io.kotest.multiplatform.io.kotest.multiplatform.gradle.plugin)
    alias(sphereonplug.plugins.sphereon.gradle.plugin.project.publication)
    id("maven-publish")
//    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin)
}

// val osName = System.getProperty("os.name")
// val targetOs = when {
//    osName == "Mac OS X" -> "macos"
//    osName.startsWith("Win") -> "windows"
//    osName.startsWith("Linux") -> "linux"
//    else -> error("Unsupported OS: $osName")
// }
//
// val osArch = System.getProperty("os.arch")
// val targetArch = when (osArch) {
//    "x86_64", "amd64" -> "x64"
//    "aarch64" -> "arm64"
//    else -> error("Unsupported arch: $osArch")
// }
//
// val skikoVersion = "0.9.21" // or any more recent version
// val target = "${targetOs}-${targetArch}"

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    /*    listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }*/

    jvm("desktop")

    /*  @OptIn(ExperimentalWasmDsl::class)
      wasmJs {
          outputModuleName.set("composeApp")
          browser {
              val rootDirPath = project.rootDir.path
              val projectDirPath = project.projectDir.path
              commonWebpackConfig {
                  outputFileName = "composeApp.js"
                  devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                      static = (static ?: mutableListOf()).apply {
                          // Serve sources to debug inside browser
                          add(rootDirPath)
                          add(projectDirPath)
                      }
                  }
              }
          }
          binaries.executable()
      }*/

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(sphereonlib.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            api(libs.sphereon.core.api.public)
            // disabled: implementation(libs.sphereon.di.scope.libDiScopePublic)
            // disabled: implementation(libs.amz.kotlin.inject.contribute.public)
            api(libs.sphereon.cbor)
            api(libs.sphereon.crypto)
            api(libs.sphereon.crypto.kms)
            api(libs.sphereon.data.link.ble.public)
            api(libs.sphereon.data.link.nfc.public)
            api(libs.sphereon.mdoc.core)
            api(libs.sphereon.mdoc.datatransfer)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.core)
            // default deps are already injected by conventions plugin!
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.cbor)
//            implementation(sphereonlib.dev.whyoleg.cryptography.core)
            implementation(libs.amz.app.platform.presenter.molecule.public)
            api(libs.kiwa.holder.sdk.public)
            api(projects.example.holder.ui.card.kiwaExampleHolderUiCardPublic)
            api(projects.example.holder.ui.core.kiwaExampleHolderUiCorePublic)
        }
        commonTest.dependencies {
            implementation(sphereonlib.org.jetbrains.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
    /*ksp {
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
    namespace = "com.sphereon.ui.elicense.engagament"
    compileSdk = 36

    /*defaultConfig {
        applicationId = "com.sphereon.mdoc.testapp"
        minSdk = 30
//        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
    }*/
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
