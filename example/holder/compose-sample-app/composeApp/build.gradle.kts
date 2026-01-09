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

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import co.touchlab.skie.configuration.SealedInterop
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(sphereonplug.plugins.org.jetbrains.kotlin.multiplatform)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(sphereonplug.plugins.com.android.application)
    alias(sphereonplug.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose)
    alias(sphereonplug.plugins.org.jetbrains.compose.hot.reload)
    alias(sphereonplug.plugins.io.kotest.io.kotest.gradle.plugin)
    alias(sphereonplug.plugins.com.google.devtools.ksp.com.google.devtools.ksp.gradle.plugin)
    id("co.touchlab.skie") version "0.10.8"
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



val xcFramework = XCFramework("KiwaSampleApp")

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        // Only build debug framework to avoid slow release linking
        iosTarget.binaries.framework(listOf(NativeBuildType.DEBUG)) {
            baseName = "KiwaSampleApp"
            isStatic = true
            xcFramework.add(this)
            export(libs.kiwa.holder.sdk.impl)
            export(libs.kiwa.holder.sdk.public)
            export(libs.sphereon.core.api.public)
            export(libs.sphereon.core.api.default)
            export(libs.sphereon.data.link.http.client.public)
            export(libs.sphereon.data.link.http.client.impl)
            export(libs.sphereon.mdoc.core.public)
            export(libs.sphereon.mdoc.core.impl)
            export(libs.sphereon.mdoc.datatransfer.public)
            export(libs.sphereon.mdoc.datatransfer.impl)
            transitiveExport = false
        }
    }


    sourceSets {

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(sphereonlib.androidx.activity.compose)
            api(libs.amz.app.platform.presenter.molecule.impl)
            // Kiwa SDK Implementation in final Android project only
            api(libs.kiwa.holder.sdk.impl)
            // Logger directly from the Sphereon Identity Development Kit as it is not exposed via the Kiwa SDK
            api(libs.sphereon.core.logger.mobile)
            api(libs.sphereon.mdoc.datatransfer.impl)
            api(libs.sphereon.mdoc.transport.nfc)
            api(libs.sphereon.mdoc.transport.ble)
            api(libs.sphereon.mdoc.transport.restapi)
            api(libs.sphereon.mdoc.transport.oid4vp)
        }

        iosMain.dependencies {
            // Kiwa SDK Implementation in final Android project only
            api(libs.kiwa.holder.sdk.impl)
            // Logger directly from the Sphereon Identity Development Kit as it is not exposed via the Kiwa SDK
            api(libs.sphereon.core.logger.mobile)
            api(libs.sphereon.mdoc.datatransfer.impl)
            api(libs.sphereon.mdoc.transport.nfc)
            api(libs.sphereon.mdoc.transport.ble)
            api(libs.sphereon.mdoc.transport.restapi)
            api(libs.sphereon.mdoc.transport.oid4vp)
            api(libs.amz.app.platform.presenter.molecule.impl)
        }

        iosX64Main.dependencies {
            api(libs.amz.app.platform.presenter.molecule.impl)
        }

        iosArm64Main.dependencies {
            api(libs.amz.app.platform.presenter.molecule.impl)
        }

        iosSimulatorArm64Main.dependencies {
            api(libs.amz.app.platform.presenter.molecule.impl)
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
            // Kiwa SDK interfaces and common code
            api(libs.kiwa.holder.sdk.public)
            api(libs.sphereon.mdoc.core.public)
            api(libs.sphereon.mdoc.core.impl)
            api(libs.sphereon.mdoc.datatransfer.public)


            api(libs.amz.app.platform.presenter.molecule.public)
            api(libs.amz.app.platform.renderer.compose.public)
            api(libs.amz.app.platform.scope.public)
            api(libs.amz.app.platform.renderer.public)
            api(libs.amz.app.platform.presenter.public)
            api(libs.multiplatform.settings)
            api(libs.multiplatform.settings.coroutines)
            api(libs.collection)
            api(libs.qrcode.kotlin)
            // Projects in this repo
            api(projects.example.holder.ui.core.kiwaExampleHolderUiCorePublic)
            api(projects.example.holder.ui.core.kiwaExampleHolderUiCoreImpl)
            api(projects.example.holder.ui.elicense.kiwaExampleHolderUiElicensePublic)
            api(projects.example.holder.ui.elicense.kiwaExampleHolderUiElicenseImpl)
            api(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthPublic)
            api(projects.example.holder.ui.auth.kiwaExampleHolderUiAuthImpl)
            api(projects.example.holder.ui.card.kiwaExampleHolderUiCardPublic)
            api(projects.example.holder.ui.card.kiwaExampleHolderUiCardImpl)
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
    val kspConfigurations = listOf("Android", "AndroidDebug", "IosArm64", "IosX64", "IosSimulatorArm64")
    kspConfigurations.forEach { configName ->
        addKspDependencies("ksp$configName")
    }
}
