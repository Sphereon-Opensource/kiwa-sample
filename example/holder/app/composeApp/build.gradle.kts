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

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(sphereonplug.plugins.org.jetbrains.kotlin.multiplatform)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(sphereonplug.plugins.com.android.application)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose.hot.reload)
    alias(sphereonplug.plugins.io.kotest.multiplatform.io.kotest.multiplatform.gradle.plugin)
    alias(sphereonplug.plugins.sphereon.gradle.plugin.project.publication)
    id("maven-publish")
    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin)
}

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
            implementation(compose.preview)
            implementation(sphereonlib.androidx.activity.compose)
            implementation(libs.kiwa.holder.sdk.impl)
            implementation(libs.sphereon.core.api.default)
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
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.json)
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.cbor)
            implementation(sphereonlib.org.jetbrains.androidx.lifecycle.viewmodel)
            implementation(sphereonlib.org.jetbrains.androidx.lifecycle.runtime.compose)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.core)
            implementation(libs.sphereon.core.api.public)
            implementation(libs.amz.kotlin.inject.impl)
            implementation(libs.amz.kotlin.inject.contribute.public)
            implementation(libs.sphereon.data.link.ble.public)
            implementation(libs.sphereon.data.link.nfc.public)
            implementation(libs.sphereon.data.link.nfc.impl)
            implementation(libs.sphereon.mdoc.core)
            implementation(libs.sphereon.mdoc.datatransfer)
            implementation(libs.sphereon.crypto)
            implementation(libs.sphereon.crypto.kms)
            implementation(libs.sphereon.crypto.kms.software)
            implementation(libs.sphereon.core.logger.mobile)
            implementation(libs.kiwa.holder.sdk.impl)
            implementation(libs.amz.app.platform.presenter.molecule.public)
            implementation(libs.amz.app.platform.presenter.molecule.impl)
            implementation(libs.amz.app.platform.renderer.compose.public)
            implementation(projects.example.holder.ui.core.kiwaExampleHolderUiCorePublic)
            implementation(projects.example.holder.ui.core.kiwaExampleHolderUiCoreImpl)
            implementation(projects.example.holder.ui.elicense.kiwaExampleHolderUiElicensePublic)
            implementation(projects.example.holder.ui.elicense.kiwaExampleHolderUiElicenseImpl)
            implementation(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthPublic)
            implementation(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthImpl)
            implementation(projects.example.holder.ui.card.kiwaExampleHolderUiCardPublic)
            implementation(projects.example.holder.ui.card.kiwaExampleHolderUiCardImpl)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.collection)
            implementation(libs.qrcode.kotlin)
//            implementation(libs.kiwa.holder.sdk.public)
//            implementation(libs.kiwa.holder.sdk.impl)
        }
        commonTest.dependencies {
            implementation(sphereonlib.org.jetbrains.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.swing)
        }
    }

}

android {
    namespace = "com.sphereon.mdoc.testapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sphereon.mdoc.testapp"
        minSdk = 30
//        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"
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

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.sphereon.mdoc.testapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.sphereon.mdoc.testapp"
            packageVersion = "1.0.0"
        }
    }
}

ksp {
    // We are using the Amazon App Platform binding processor instead!
    arg("software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor", "disabled")
}

dependencies {
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
}
