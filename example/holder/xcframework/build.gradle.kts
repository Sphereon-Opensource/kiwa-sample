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
            xcFramework.add(this)
            export(libs.kiwa.holder.sdk.impl)
            export(libs.kiwa.holder.sdk.public)
            export(libs.sphereon.core.api.public)
            export(libs.sphereon.core.api.default)
            export(libs.sphereon.data.link.http.client)
            export(libs.sphereon.core.logger.mobile)
            export(libs.sphereon.mdoc.core)
            export(libs.sphereon.mdoc.datatransfer)
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
            api(libs.sphereon.mdoc.datatransfer)
            api(libs.sphereon.core.api.public)
            api(libs.sphereon.core.api.default)
            api(libs.sphereon.data.link.http.client)
            api(libs.sphereon.mdoc.core)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.json)
            implementation(sphereonlib.org.jetbrains.kotlinx.serialization.cbor)
            implementation(sphereonlib.org.jetbrains.kotlinx.coroutines.core)
        }
    }
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
