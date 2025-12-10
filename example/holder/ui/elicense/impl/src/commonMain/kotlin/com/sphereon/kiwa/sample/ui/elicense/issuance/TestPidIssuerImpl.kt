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

package com.sphereon.kiwa.sample.ui.elicense.issuance

import com.sphereon.core.compat.DateTimeUtils
import com.sphereon.crypto.core.CoseJoseKeyMappingService
import com.sphereon.crypto.core.DefaultCallbacks
import com.sphereon.crypto.core.ManagedKeyInfo
import com.sphereon.crypto.core.KeyEncoding
import com.sphereon.crypto.core.KeyVisibility
import com.sphereon.crypto.core.ManagedKeyInfoType
import com.sphereon.crypto.core.cose.CoseCryptoProviderToCallbackAdapter
import com.sphereon.crypto.core.cose.CoseKey
import com.sphereon.crypto.core.cose.CoseKeyType
import com.sphereon.crypto.core.generic.SignatureAlgorithm
import com.sphereon.crypto.core.generic.X509DistinguishedNameElements
import com.sphereon.crypto.core.jose.JwkUse
import com.sphereon.crypto.core.kms.CertificateService
import com.sphereon.crypto.core.kms.KeyManagerService
import com.sphereon.crypto.core.x509.Certificate
import com.sphereon.crypto.kms.keystore.software.SoftwareKeyStoreService
import com.sphereon.crypto.kms.provider.software.SoftwareKmsProvider
import com.sphereon.di.session.SessionScope
import com.sphereon.kiwa.sample.ui.elicense.store.SimpleMdocStore
import com.sphereon.mdoc.MdocSignService
import com.sphereon.mdoc.data.device.Document
import com.sphereon.mdoc.data.device.IssuerSigned
import com.sphereon.mdoc.data.device.IssuerSignedItem
import com.sphereon.mdoc.data.eu.Pid
import com.sphereon.mdoc.data.mso.DigestID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(SessionScope::class)
@ContributesBinding(SessionScope::class)
class TestPidIssuerImpl(
    val kms: KeyManagerService,
    val mdocSignService: MdocSignService,
    val certificateService: CertificateService,
    val storage: SimpleMdocStore
) : TestPidIssuer {

    val softwareKmsProvider: SoftwareKmsProvider = kms.getProviderById("kiwa") as SoftwareKmsProvider
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

    private suspend fun getOrCreateIssuerKeyInfo(): ManagedKeyInfoType<CoseKeyType> = withContext(Dispatchers.IO) {
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
        managedKeyPair.toManagedKeyInfo<CoseKey>(KeyVisibility.PRIVATE, KeyEncoding.COSE)
    }

    private suspend fun getOrCreateIssuerCertificate(issuerKeyInfo: ManagedKeyInfoType<CoseKeyType>): Certificate = withContext(
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
                        serialNumber = CERT_SERIAL_NUMBER
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

    private suspend fun getIssuerKeyInfoWithCert(): ManagedKeyInfoType<CoseKeyType> = withContext(Dispatchers.IO) {
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
        val expiryTime = Instant.fromEpochSeconds(
            currentTime.epochSeconds + (DAYS_UNTIL_EXPIRY * SECONDS_PER_DAY)
        )

        val personalData = generateRandomPersonalData()
        val addressData = generateRandomAddressData()
        val ageData = calculateAgeData(personalData.birthYear)

        return buildPidItems(personalData, addressData, ageData, currentTime, expiryTime)
    }

    private data class PersonalData(
        val givenName: String,
        val familyName: String,
        val birthDate: String,
        val birthYear: Int
    )

    private data class AddressData(
        val city: String,
        val street: String,
        val postalCode: String,
        val houseNumber: String
    )

    private data class AgeData(
        val ageInYears: Int,
        val ageOver18: Boolean
    )

    private fun generateRandomPersonalData(): PersonalData {
        val givenNames = listOf("John", "Emma", "Michael", "Sarah", "David", "Lisa", "James", "Anna", "Robert", "Maria")
        val familyNames =
            listOf("Doe", "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez")
        val birthDates = listOf(
            "1990-03-15" to BIRTH_YEAR_1990,
            "1985-07-22" to BIRTH_YEAR_1985,
            "1992-11-08" to BIRTH_YEAR_1992,
            "1988-05-30" to BIRTH_YEAR_1988,
            "1995-12-03" to BIRTH_YEAR_1995,
            "1987-09-14" to BIRTH_YEAR_1987
        )

        val (selectedBirthDate, birthYear) = birthDates.random()
        return PersonalData(
            givenName = givenNames.random(),
            familyName = familyNames.random(),
            birthDate = selectedBirthDate,
            birthYear = birthYear
        )
    }

    private fun generateRandomAddressData(): AddressData {
        val cities = listOf("Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Eindhoven", "Tilburg")
        val streets = listOf("Kerkstraat", "Hoofdstraat", "Schoolstraat", "Kerkweg", "Molenlaan", "Dorpsstraat")
        val postalCodes = listOf("1018 LL", "3011 AD", "2511 CV", "3521 AZ", "5611 AB", "5038 EA")

        return AddressData(
            city = cities.random(),
            street = streets.random(),
            postalCode = postalCodes.random(),
            houseNumber = (MIN_HOUSE_NUMBER..MAX_HOUSE_NUMBER).random().toString()
        )
    }

    private fun calculateAgeData(birthYear: Int): AgeData {
        val currentTime = Clock.System.now()
        val currentYear = currentTime.toString().substring(YEAR_START_INDEX, YEAR_LENGTH).toInt()
        val ageInYears = currentYear - birthYear
        return AgeData(
            ageInYears = ageInYears,
            ageOver18 = ageInYears >= AGE_THRESHOLD_18
        )
    }

    private fun buildPidItems(
        personalData: PersonalData,
        addressData: AddressData,
        ageData: AgeData,
        currentTime: Instant,
        expiryTime: Instant
    ): List<IssuerSignedItem<Any>> {
        val items = mutableListOf<IssuerSignedItem<Any>>()

        // Personal information
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(1u),
                elementDef = Pid.Def.given_name,
                elementValue = personalData.givenName
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(2u),
                elementDef = Pid.Def.family_name,
                elementValue = personalData.familyName
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(3u),
                elementDef = Pid.Def.birth_date,
                elementValue = personalData.birthDate
            )
        )

        // Age information
        addAgeItems(items, ageData, personalData)

        // Birth information
        addBirthItems(items, personalData, addressData)

        // Address information
        addAddressItems(items, addressData)

        // Additional attributes
        addAdditionalAttributes(items)

        // Document metadata
        addDocumentMetadata(items, currentTime, expiryTime)

        return items
    }

    private fun addAgeItems(items: MutableList<IssuerSignedItem<Any>>, ageData: AgeData, personalData: PersonalData) {
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(4u),
                elementDef = Pid.Def.age_over_18,
                elementValue = ageData.ageOver18
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(5u),
                elementDef = Pid.Def.age_in_years,
                elementValue = ageData.ageInYears
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(6u),
                elementDef = Pid.Def.age_birth_year,
                elementValue = personalData.birthYear
            )
        )
    }

    private fun addBirthItems(
        items: MutableList<IssuerSignedItem<Any>>,
        personalData: PersonalData,
        addressData: AddressData
    ) {
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(7u),
                elementDef = Pid.Def.family_name_birth,
                elementValue = personalData.familyName
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(8u),
                elementDef = Pid.Def.given_name_birth,
                elementValue = personalData.givenName
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(9u),
                elementDef = Pid.Def.birth_place,
                elementValue = addressData.city
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(10u),
                elementDef = Pid.Def.birth_country,
                elementValue = "NL"
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(11u),
                elementDef = Pid.Def.birth_state,
                elementValue = "Noord-Holland"
            )
        )
    }

    private fun addAddressItems(items: MutableList<IssuerSignedItem<Any>>, addressData: AddressData) {
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(12u),
                elementDef = Pid.Def.resident_city,
                elementValue = addressData.city
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(13u),
                elementDef = Pid.Def.resident_postal_code,
                elementValue = addressData.postalCode
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(14u),
                elementDef = Pid.Def.resident_street,
                elementValue = addressData.street
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(15u),
                elementDef = Pid.Def.resident_house_number,
                elementValue = addressData.houseNumber
            )
        )
    }

    private fun addAdditionalAttributes(items: MutableList<IssuerSignedItem<Any>>) {
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(16u),
                elementDef = Pid.Def.gender,
                elementValue = (MIN_GENDER..MAX_GENDER).random().toUInt()
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(17u),
                elementDef = Pid.Def.nationality,
                elementValue = "NL"
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(18u),
                elementDef = Pid.Def.issuing_country,
                elementValue = "NL"
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(19u),
                elementDef = Pid.Def.issuing_authority,
                elementValue = "NL"
            )
        )
    }

    private fun addDocumentMetadata(
        items: MutableList<IssuerSignedItem<Any>>,
        currentTime: Instant,
        expiryTime: Instant
    ) {
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(20u),
                elementDef = Pid.Def.issuance_date,
                elementValue = currentTime.toString()
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(21u),
                elementDef = Pid.Def.expiry_date,
                elementValue = expiryTime.toString()
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(22u),
                elementDef = Pid.Def.document_number,
                elementValue = (MIN_RANDOM_DOC_NUMBER..MAX_RANDOM_DOC_NUMBER).random().toString()
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(23u),
                elementDef = Pid.Def.administrative_number,
                elementValue = (MIN_RANDOM_DOC_NUMBER..MAX_RANDOM_DOC_NUMBER).random().toString()
            )
        )
        items.add(
            IssuerSignedItem.createFromDefinition(
                digestID = DigestID(24u),
                elementDef = Pid.Def.issuing_jurisdiction,
                elementValue = "NL"
            )
        )
    }

    override suspend fun issuePid(deviceKeyInfo: ManagedKeyInfoType<CoseKeyType>?): Document = withContext(
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
                ).toManagedKeyInfo<CoseKey>(KeyVisibility.PRIVATE, KeyEncoding.COSE)
            }

            val issuerKeyInfoWithCertDeferred = async { getIssuerKeyInfoWithCert() }
            val pidDataItemsDeferred = async { createPidDataItems() }

            // Await all parallel operations
            val deviceKeyInfoToUse = deviceKeyInfoDeferred.await()
            val issuerKeyInfoWithCert = issuerKeyInfoWithCertDeferred.await()
            val pidDataItems = pidDataItemsDeferred.await()

            // Pre-calculate time values once
            val currentTime = Clock.System.now()
            val signedTime = DateTimeUtils.DEFAULT.dateTime(
                epochSeconds = (currentTime.epochSeconds - TIME_OFFSET_PAST_SECONDS).toInt()
            )
            val validFromTime = DateTimeUtils.DEFAULT.dateTime(
                epochSeconds = (currentTime.epochSeconds - TIME_OFFSET_PAST_SECONDS).toInt()
            )
            val validUntilTime = DateTimeUtils.DEFAULT.dateTime(
                epochSeconds = (currentTime.epochSeconds + TIME_OFFSET_FUTURE_SECONDS).toInt()
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
            pidDataItems.forEach { item ->
                msoBuilder.addNameSpace(Pid.NAMESPACE, item)
            }

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

    @ContributesTo(SessionScope::class)
    interface Component {
        val testPidIssuer: TestPidIssuer
    }

    companion object {
        const val PID_ISSUER_KEY_ALIAS = "test-pid-issuer"
        private const val CERT_SERIAL_NUMBER = 1
        private const val DAYS_UNTIL_EXPIRY = 365
        private const val SECONDS_PER_DAY = 86400L
        private const val TIME_OFFSET_PAST_SECONDS = 144000L
        private const val TIME_OFFSET_FUTURE_SECONDS = 120000L
        private const val MIN_RANDOM_DOC_NUMBER = 100000000
        private const val MAX_RANDOM_DOC_NUMBER = 999999999
        private const val MIN_HOUSE_NUMBER = 1
        private const val MAX_HOUSE_NUMBER = 99
        private const val MIN_GENDER = 0
        private const val MAX_GENDER = 2
        private const val AGE_THRESHOLD_18 = 18
        private const val BIRTH_YEAR_1990 = 1990
        private const val BIRTH_YEAR_1985 = 1985
        private const val BIRTH_YEAR_1992 = 1992
        private const val BIRTH_YEAR_1988 = 1988
        private const val BIRTH_YEAR_1995 = 1995
        private const val BIRTH_YEAR_1987 = 1987
        private const val YEAR_START_INDEX = 0
        private const val YEAR_LENGTH = 4
    }
}
