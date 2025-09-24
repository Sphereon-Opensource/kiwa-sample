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

package com.sphereon.mdoc.testapp

import com.sphereon.data.link.nfc.INfcApduDispatcher
import com.sphereon.data.link.nfc.SessionNfcApduDispatcher
import com.sphereon.di.session.ISureSessionComponent
import com.sphereon.mdoc.AbstractMdocNfcService

class MdocNfcService : AbstractMdocNfcService() {
    // Since this class will be instantiated from the NfcAdapter by a class reference, let's hookup the session component
    override val sessionComponent: ISureSessionComponent = MainActivity.sessionComponentFlow.value
    override val apduService: INfcApduDispatcher = (MainActivity.sessionComponentFlow.value as SessionNfcApduDispatcher.Component).sessionNfcApduDispatcher
}
