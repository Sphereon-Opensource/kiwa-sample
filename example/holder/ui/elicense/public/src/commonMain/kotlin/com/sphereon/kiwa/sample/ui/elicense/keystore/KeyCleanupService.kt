/*
 * © 2026 Sphereon International B.V.
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

/*
 * © 2026 Sphereon International B.V.
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

package com.sphereon.kiwa.sample.ui.elicense.keystore

/**
 * Service responsible for cleaning up ephemeral keys from the keystore.
 * 
 * This service manages the lifecycle of cryptographic keys by removing
 * keys that are no longer associated with stored documents, while preserving
 * essential system keys like wallet certificates and issuer keys.
 */
interface KeyCleanupService {
    
    /**
     * Performs cleanup of ephemeral keys from the keystore.
     * 
     * This method:
     * - Lists all key aliases from the SimpleMdocStore
     * - Lists all keys from the keystore
     * - Retains keys that are either:
     *   - Associated with stored documents
     *   - System keys (KIWA_WALLET_CERT_ALIAS, PID_ISSUER_KEY_ALIAS)
     * - Deletes all other keys
     * 
     * All deleted keys are logged for audit purposes.
     */
    suspend fun cleanupEphemeralKeys()
}
