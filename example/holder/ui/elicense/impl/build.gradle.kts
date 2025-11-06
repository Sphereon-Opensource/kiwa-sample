/* Â© 2025 Sphereon International B.V.
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
//    alias(sphereonplug.plugins.io.kotest.multiplatform.io.kotest.multiplatform.gradle.plugin)
    alias(sphereonplug.plugins.sphereon.gradle.plugin.project.publication)
    id("maven-publish")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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

//    jvm("desktop")

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
//        val desktopMain by getting

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
            /*implementation(libs.sphereon.core.api.public)
            implementation(libs.sphereon.core.api.default)
            implementation(libs.sphereon.crypto.kms)
            implementation(libs.sphereon.compat)*/
            implementation(libs.bundles.kotlin.inject)
//            api(libs.kiwa.holder.sdk.impl)
            implementation(libs.kiwa.holder.sdk.public)
            /*  implementation(libs.kiwa.holder.sdk.public)
              implementation(libs.sphereon.cbor)
              implementation(libs.sphereon.crypto)
              implementation(libs.sphereon.crypto.kms)

              implementation(libs.sphereon.data.link.ble.public)
              implementation(libs.sphereon.data.link.nfc.public)
              implementation(libs.sphereon.mdoc.core)
              implementation(libs.sphereon.mdoc.datatransfer)*/
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.core)
            // default deps are already injected by conventions plugin!
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.cbor)
            implementation(sphereonlib.dev.whyoleg.cryptography.core)
            implementation(libs.amz.app.platform.presenter.molecule.public)
            implementation(libs.amz.app.platform.renderer.compose.public)
            implementation(libs.amz.kotlin.inject.contribute.public)
            implementation(libs.kottage)
            implementation(libs.qrcode.kotlin)
            implementation(libs.sphereon.mdoc.datatransfer)
            implementation(libs.sphereon.crypto.kms.software)
            implementation(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthPublic)
            implementation(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthImpl)
            implementation(projects.example.holder.ui.core.kiwaExampleHolderUiCorePublic)
            implementation(projects.example.holder.ui.elicense.kiwaExampleHolderUiElicensePublic)
            implementation(projects.example.holder.ui.card.kiwaExampleHolderUiCardPublic)
            implementation(projects.example.holder.ui.card.kiwaExampleHolderUiCardImpl)
            /*implementation(projects.example.holder.ui.core.kiwaExampleHolderUiCorePublic)*/
            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(sphereonlib.org.jetbrains.kotlin.test)
            implementation(libs.amz.kotlin.inject.impl)
        }
//        desktopMain.dependencies {
//            implementation(compose.desktop.currentOs)
//            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.swing)
//        }
    }
}

android {
    namespace = "com.sphereon.kiwa.sample.ui.elicense.engagement.impl"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
/*

compose.desktop {
    application {
        mainClass = "com.sphereon.kiwa.sample.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.sphereon.kiwa.sample.app"
            packageVersion = "1.0.0"
        }
    }
}
*/

ksp {
    // We are using the Amazon App Platform binding processor instead!
    arg("software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor", "disabled")
}

dependencies {
//    val kspDesktopConfig = "kspDesktop"
    val kspAndroidConfig = "kspAndroid"
    val kspAndroidDebugConfig = "kspAndroidDebug"
    val kspAndroidTestConfig = "kspAndroidTest"

//    addProvider(kspDesktopConfig, libs.kotlin.inject.compiler.ksp)
//    add(kspDesktopConfig, libs.amz.kotlin.inject.contribute.public)
//    add(kspDesktopConfig, libs.amz.kotlin.inject.contribute.code.generators)
//    add(kspDesktopConfig, libs.anvil.compiler.ksp)

    addProvider(kspAndroidConfig, libs.kotlin.inject.compiler.ksp)
    add(kspAndroidConfig, libs.amz.kotlin.inject.contribute.public)
    add(kspAndroidConfig, libs.amz.kotlin.inject.contribute.code.generators)
    add(kspAndroidConfig, libs.anvil.compiler.ksp)

    addProvider(kspAndroidDebugConfig, libs.kotlin.inject.compiler.ksp)
    add(kspAndroidDebugConfig, libs.amz.kotlin.inject.contribute.public)
    add(kspAndroidDebugConfig, libs.amz.kotlin.inject.contribute.code.generators)
    add(kspAndroidDebugConfig, libs.anvil.compiler.ksp)

    addProvider(kspAndroidTestConfig, libs.kotlin.inject.compiler.ksp)
    add(kspAndroidTestConfig, libs.amz.kotlin.inject.contribute.public)
    add(kspAndroidTestConfig, libs.amz.kotlin.inject.contribute.code.generators)
    add(kspAndroidTestConfig, libs.anvil.compiler.ksp)
}
