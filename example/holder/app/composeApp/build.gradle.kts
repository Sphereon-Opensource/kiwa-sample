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
    alias(sphereonplug.plugins.com.android.application)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose.hot.reload)
    alias(sphereonplug.plugins.io.kotest.io.kotest.gradle.plugin)
    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

   /* listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KiwaSampleApp"
            isStatic = true
        }
    }*/


    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(sphereonlib.androidx.activity.compose)
            implementation(libs.kiwa.holder.sdk.impl)
            implementation(libs.sphereon.core.logger.mobile)
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
            implementation(libs.amz.kotlin.inject.impl)
            implementation(libs.amz.kotlin.inject.impl)
            implementation(libs.amz.kotlin.inject.contribute.public)
            implementation(libs.kiwa.holder.sdk.public)
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
        }
    }
}
val applicationId = "com.sphereon.kiwa.sample.app"

android {
    namespace = applicationId
    compileSdk = 36

    defaultConfig {
        applicationId = applicationId
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

fun DependencyHandlerScope.addKspDependencies(configName: String) {
    addProvider(configName, libs.kotlin.inject.compiler.ksp)
    add(configName, libs.amz.kotlin.inject.contribute.public)
    add(configName, libs.amz.kotlin.inject.contribute.code.generators)
    add(configName, libs.anvil.compiler.ksp)
}

dependencies {
    val kspConfigurations = listOf("Android", "AndroidDebug"/*, "IosArm64", "IosX64", "IosSimulatorArm64"*/)
    kspConfigurations.forEach { configName ->
        addKspDependencies("ksp$configName")
    }
}
