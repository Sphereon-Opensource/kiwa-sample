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

package com.sphereon.ui.elicense.issuance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import com.sphereon.core.compat.DateTimeUtils
import com.sphereon.crypto.core.CoseJoseKeyMappingService
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.crypto.core.IManagedKeyInfo
import com.sphereon.crypto.core.KeyEncoding
import com.sphereon.crypto.core.KeyVisibility
import com.sphereon.crypto.core.ManagedKeyInfo
import com.sphereon.crypto.core.cose.CoseCryptoProviderToCallbackAdapter
import com.sphereon.crypto.core.cose.ICoseKeyCbor
import com.sphereon.crypto.core.generic.SignatureAlgorithm
import com.sphereon.crypto.core.generic.X509DistinguishedNameElements
import com.sphereon.crypto.core.jose.JwkUse
import com.sphereon.crypto.core.kms.ICertificateService
import com.sphereon.crypto.core.kms.IKeyManagerService
import com.sphereon.crypto.core.x509.Certificate
import com.sphereon.crypto.kms.keystore.software.SoftwareKeyStoreService
import com.sphereon.crypto.kms.provider.software.ISoftwareKmsProvider
import com.sphereon.di.session.SureSessionScope
import com.sphereon.mdoc.IMdocSignService
import com.sphereon.mdoc.data.device.Document
import com.sphereon.mdoc.data.device.IssuerSigned
import com.sphereon.mdoc.data.device.IssuerSignedItem
import com.sphereon.mdoc.data.eu.Pid
import com.sphereon.mdoc.data.mso.DigestID
import com.sphereon.ui.elicense.store.ISimpleMdocStore
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Inject
@SingleIn(SureSessionScope::class)
@ContributesBinding(SureSessionScope::class)
class TestPidIssuer(
    val kms: IKeyManagerService,
    val mdocSignService: IMdocSignService,
    val certificateService: ICertificateService,
    val storage: ISimpleMdocStore
) : ITestPidIssuer {

    val softwareKmsProvider: ISoftwareKmsProvider = kms.getProviderById("kiwa") as ISoftwareKmsProvider
    val keyStore = softwareKmsProvider.keyStore as SoftwareKeyStoreService

    // Cache only for certificate to avoid repeated lookups (certificates are public information)
    private var cachedIssuerCertificate: Certificate? = null

    // Mutex to ensure thread-safe initialization of cached certificate
    private val initMutex = Mutex()

    init {
        DefaultCallbacks.setCoseCryptoDefault(CoseCryptoProviderToCallbackAdapter(keyManagerService = kms))
        /*registerMemoryKeyStoreSerialization()
        registerSoftwareKeyStoreSerialization()
        registerSoftwareKmsSerialization()*/
    }

    private suspend fun getOrCreateIssuerKeyInfo(): IManagedKeyInfo<ICoseKeyCbor> = withContext(Dispatchers.IO) {
        var keyInfo = keyStore.listKeys().firstOrNull { it.alias == PID_ISSUER_KEY_ALIAS }
        if (keyInfo != null) {
            return@withContext ManagedKeyInfo(
                alias = PID_ISSUER_KEY_ALIAS,
                providerId = keyStore.id,
                resolvedKeyInfo = CoseJoseKeyMappingService.toResolvedCoseKeyInfo(keyInfo)
            )
        }
        val managedKeyPair = kms.generateKeyAsync(
            alias = PID_ISSUER_KEY_ALIAS,
            alg = SignatureAlgorithm.ECDSA_SHA256,
            keyVisibility = KeyVisibility.PRIVATE
        )
        managedKeyPair.toManagedKeyInfo<ICoseKeyCbor>(KeyVisibility.PRIVATE, KeyEncoding.COSE)
    }

    private suspend fun getOrCreateIssuerCertificate(issuerKeyInfo: IManagedKeyInfo<ICoseKeyCbor>): Certificate = withContext(
        Dispatchers.IO
    ) {
        // Return cached certificate if available (certificates are public information, safe to cache)
        cachedIssuerCertificate?.let { return@withContext it }

        initMutex.withLock {
            // Double-check pattern for certificate only
            cachedIssuerCertificate?.let { return@withLock it }

            if (!keyStore.listCertificateAliases().contains(PID_ISSUER_KEY_ALIAS)) {
                val issuerCn = X509DistinguishedNameElements(
                    commonName = PID_ISSUER_KEY_ALIAS,
                    organizationName = "Test Issuer Org",
                    organizationUnit = "Test Issuer Org Unit",
                    country = "NL"
                )
                val issuerCertResult =
                    certificateService.createCertificate(
                        issuerKeyInfo = issuerKeyInfo,
                        issuer = issuerCn,
                        subjectKeyInfo = issuerKeyInfo,
                        subject = issuerCn,
                        serialNumber = 1
                    )
                keyStore.storeCertificateChain(
                    PID_ISSUER_KEY_ALIAS,
                    arrayOf(issuerCertResult.certificate),
                    issuerKeyInfo
                )
            }
            val certificate = keyStore.getCertificate(PID_ISSUER_KEY_ALIAS)
            cachedIssuerCertificate = certificate
            certificate
        }
    }

    private suspend fun getIssuerKeyInfoWithCert(): IManagedKeyInfo<ICoseKeyCbor> = withContext(Dispatchers.IO) {
        // Always create fresh key info with certificate - no caching of private key material
        coroutineScope {
            val issuerKeyInfoDeferred = async { getOrCreateIssuerKeyInfo() }
            val issuerKeyInfo = issuerKeyInfoDeferred.await()
            val issuerCert = getOrCreateIssuerCertificate(issuerKeyInfo)

            ManagedKeyInfo(
                alias = PID_ISSUER_KEY_ALIAS,
                providerId = "kiwa",
                resolvedKeyInfo = issuerCert.amendCoseKeyInfo(issuerKeyInfo)
            )
        }
    }

    private fun createPidDataItems(): List<IssuerSignedItem<Any>> {
        // Pre-calculate timestamp values once
        val currentTime = Clock.System.now()
        val expiryTime = currentTime.plus(365.days)

        // Random data pools
        val givenNames = listOf("John", "Emma", "Michael", "Sarah", "David", "Lisa", "James", "Anna", "Robert", "Maria")
        val familyNames =
            listOf("Doe", "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez")
        val birthDates = listOf(
            "1990-03-15" to 1990,
            "1985-07-22" to 1985,
            "1992-11-08" to 1992,
            "1988-05-30" to 1988,
            "1995-12-03" to 1995,
            "1987-09-14" to 1987
        )
        val cities = listOf("Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven", "Tilburg")
        val streets = listOf("Kerkstraat", "Hoofdstraat", "Schoolstraat", "Kerkweg", "Molenlaan", "Dorpsstraat")
        val postalCodes = listOf("1018 LL", "3011 AD", "2511 CV", "3521 AZ", "5611 AB", "5038 EA")

        // Select random values
        val selectedGivenName = givenNames.random()
        val selectedFamilyName = familyNames.random()
        val (selectedBirthDate, birthYear) = birthDates.random()
        val selectedCity = cities.random()
        val selectedStreet = streets.random()
        val selectedPostalCode = postalCodes.random()
        val houseNumber = (1..99).random().toString()

        // Calculate derived age fields
        val currentYear = currentTime.toString().substring(0, 4).toInt() // Extract year from current time
        val ageInYears = currentYear - birthYear
        val ageOver18 = ageInYears >= 18

        return listOf(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(1u),
                elementDef = Pid.Def.given_name,
                elementValue = selectedGivenName
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(2u),
                elementDef = Pid.Def.family_name,
                elementValue = selectedFamilyName
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(3u),
                elementDef = Pid.Def.birth_date,
                elementValue = selectedBirthDate
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(4u),
                elementDef = Pid.Def.age_over_18,
                elementValue = ageOver18
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(5u),
                elementDef = Pid.Def.age_in_years,
                elementValue = ageInYears
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(6u),
                elementDef = Pid.Def.age_birth_year,
                elementValue = birthYear
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(7u),
                elementDef = Pid.Def.family_name_birth,
                elementValue = selectedFamilyName
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(8u),
                elementDef = Pid.Def.given_name_birth,
                elementValue = selectedGivenName
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(9u),
                elementDef = Pid.Def.birth_place,
                elementValue = selectedCity
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(10u),
                elementDef = Pid.Def.birth_country,
                elementValue = "NL"
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(11u),
                elementDef = Pid.Def.birth_state,
                elementValue = "Noord-Holland"
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(12u),
                elementDef = Pid.Def.resident_city,
                elementValue = selectedCity
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(13u),
                elementDef = Pid.Def.resident_postal_code,
                elementValue = selectedPostalCode
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(14u),
                elementDef = Pid.Def.resident_street,
                elementValue = selectedStreet
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(15u),
                elementDef = Pid.Def.resident_house_number,
                elementValue = houseNumber
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(16u),
                elementDef = Pid.Def.gender,
                elementValue = (0..2).random().toUInt()
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(17u),
                elementDef = Pid.Def.nationality,
                elementValue = "NL"
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(18u),
                elementDef = Pid.Def.issuing_country,
                elementValue = "NL"
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(19u),
                elementDef = Pid.Def.issuing_authority,
                elementValue = "NL"
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(20u),
                elementDef = Pid.Def.issuance_date,
                elementValue = currentTime.toString()
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(21u),
                elementDef = Pid.Def.expiry_date,
                elementValue = expiryTime.toString()
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(22u),
                elementDef = Pid.Def.document_number,
                elementValue = (100000000..999999999).random().toString()
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(23u),
                elementDef = Pid.Def.administrative_number,
                elementValue = (100000000..999999999).random().toString()
            ),
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(24u),
                elementDef = Pid.Def.issuing_jurisdiction,
                elementValue = "NL"
            )
        )
    }

    override suspend fun issuePid(deviceKeyInfo: IManagedKeyInfo<ICoseKeyCbor>?): Document = withContext(
        Dispatchers.IO
    ) {
        // Use coroutineScope to parallelize operations
        coroutineScope {
            // Start device key creation and issuer setup in parallel
            val deviceKeyInfoDeferred = async {
                deviceKeyInfo ?: kms.generateKeyAsync(
                    providerId = "kiwa",
                    alias = "dk-${Clock.System.now()}",
                    use = JwkUse.sig,
                    alg = SignatureAlgorithm.ECDSA_SHA256,
                    keyVisibility = KeyVisibility.PRIVATE
                ).toManagedKeyInfo<ICoseKeyCbor>(KeyVisibility.PRIVATE, KeyEncoding.COSE)
            }

            val issuerKeyInfoWithCertDeferred = async { getIssuerKeyInfoWithCert() }
            val pidDataItemsDeferred = async { createPidDataItems() }

            // Await all parallel operations
            val deviceKeyInfoToUse = deviceKeyInfoDeferred.await()
            val issuerKeyInfoWithCert = issuerKeyInfoWithCertDeferred.await()
            val pidDataItems = pidDataItemsDeferred.await()

            // Pre-calculate time values once
            val currentTime = Clock.System.now()
            val signedTime = DateTimeUtils.DEFAULT.dateTime(epochSeconds = (currentTime.epochSeconds - 144000).toInt())
            val validFromTime = DateTimeUtils.DEFAULT.dateTime(
                epochSeconds = (currentTime.epochSeconds - 144000).toInt()
            )
            val validUntilTime = DateTimeUtils.DEFAULT.dateTime(
                epochSeconds = (currentTime.epochSeconds + 120000).toInt()
            )

            // Build mdoc with pre-calculated data
            val msoBuilder = IssuerSigned.MsoBuilder()
                .withDocType(Pid.DOCTYPE)
                .withDeviceKeyInfo(deviceKeyInfoToUse)
                .withSigningKeyInfo(issuerKeyInfoWithCert)
                .withSigned(signedTime)
                .withValidFrom(validFromTime)
                .withValidUntil(validUntilTime)

            // Add namespace with all items at once
            msoBuilder.addNameSpace(Pid.NAMESPACE, *pidDataItems.toTypedArray())

            // Build and sign mdoc, then store it in parallel
            val buildMdocDeferred = async {
                msoBuilder.buildAndSignMdoc(mdocSignService = mdocSignService, requireDeviceX5Chain = false)
            }

            val issuerMdoc = buildMdocDeferred.await()

            // Store document asynchronously (fire and forget for better performance, but wait for completion)
            val storeDeferred = async {
                storage.storeDocument(mdoc = issuerMdoc, keyInfo = deviceKeyInfoToUse)
            }

            // Wait for storage to complete before returning
            storeDeferred.await()

            issuerMdoc
        }
    }

    @ContributesTo(SureSessionScope::class)
    interface Component {
        val testPidIssuer: ITestPidIssuer
    }

    companion object {
        const val PID_ISSUER_KEY_ALIAS = "test-pid-issuer"
    }
}
