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


    sourceSets {

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
            implementation(libs.kiwa.holder.sdk.public)
            implementation(libs.amz.app.platform.presenter.molecule.public)
            implementation(libs.amz.app.platform.renderer.compose.public)
            implementation(libs.amz.kotlin.inject.contribute.public)
            implementation(libs.kottage)
            implementation(libs.qrcode.kotlin)
            implementation(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthPublic)
            implementation(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthImpl)
            implementation(projects.example.holder.ui.core.kiwaExampleHolderUiCorePublic)
            implementation(projects.example.holder.ui.elicense.kiwaExampleHolderUiElicensePublic)
            implementation(projects.example.holder.ui.card.kiwaExampleHolderUiCardPublic)
            implementation(projects.example.holder.ui.card.kiwaExampleHolderUiCardImpl)
            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(sphereonlib.org.jetbrains.kotlin.test)
        }
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

ksp {
    // We are using the Amazon App Platform binding processor instead!
    arg("software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor", "disabled")
}

dependencies {
    val kspAndroidConfig = "kspAndroid"
    val kspAndroidDebugConfig = "kspAndroidDebug"
    val kspAndroidTestConfig = "kspAndroidTest"


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
