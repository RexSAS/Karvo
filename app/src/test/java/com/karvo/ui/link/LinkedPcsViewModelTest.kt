package com.karvo.ui.link

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.karvo.link.LinkControl
import com.karvo.link.LinkCrypto
import com.karvo.link.LinkQr
import com.karvo.link.LinkState
import com.karvo.link.PairResult
import com.karvo.link.PairingStore
import com.karvo.testing.FakeAuthenticator
import com.karvo.testing.FakeSecretBox
import com.karvo.testing.FakeSettingsRepository
import com.karvo.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LinkedPcsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private val roomId = LinkCrypto.randomBytes(16)
    private val qrText = "karvo:link?v=1&r=${LinkCrypto.b64url(roomId)}&k=${LinkCrypto.b64url(LinkCrypto.randomBytes(32))}&n=Chrome%20on%20Windows"
    private val store by lazy { PairingStore(PreferenceDataStoreFactory.create { File(tmp.root, "link.preferences_pb") }, FakeSecretBox()) }
    private val settings = FakeSettingsRepository()
    private val auth = FakeAuthenticator()

    private inner class FakeControl(var result: PairResult = PairResult.Linked("Chrome on Windows")) : LinkControl {
        override val states = MutableStateFlow<Map<String, LinkState>>(emptyMap())
        val paired = mutableListOf<LinkQr>(); val removed = mutableListOf<String>()
        override suspend fun pair(qr: LinkQr): PairResult {
            paired += qr
            if (result is PairResult.Linked) store.add(qr)
            return result
        }
        override suspend fun remove(roomId: String) { removed += roomId; store.remove(roomId) }
    }

    @Test fun `a code that is not Karvo's shows an error and no dialog`() = runTest {
        val vm = LinkedPcsViewModel(store, FakeControl(), settings, auth, "title")
        vm.onScanned("https://example.com")
        assertEquals(LinkEvent.NotKarvo, vm.events.first())
        assertNull(vm.uiState.value.confirm)
    }

    @Test fun `scan, allow, linked`() = runTest {
        val control = FakeControl()
        val vm = LinkedPcsViewModel(store, control, settings, auth, "title")
        vm.onScanned(qrText)
        assertEquals("Chrome on Windows", vm.uiState.first { it.confirm != null }.confirm?.pcName)
        vm.onAllow()
        assertEquals(LinkEvent.Linked("Chrome on Windows"), vm.events.first())
        assertEquals(1, control.paired.size)
        assertEquals(listOf("Chrome on Windows"), vm.uiState.first { it.pcs.isNotEmpty() }.pcs.map { it.name })
        assertNull(vm.uiState.value.confirm)
    }

    @Test fun `a failed biometric check links nothing`() = runTest {
        auth.succeeds = false
        val control = FakeControl()
        val vm = LinkedPcsViewModel(store, control, settings, auth, "title")
        vm.onScanned(qrText); vm.uiState.first { it.confirm != null }; vm.onAllow()
        vm.uiState.first { it.confirm == null && !it.busy }
        assertEquals(1, auth.calls)
        assertTrue(control.paired.isEmpty())
    }

    @Test fun `an expired code says so`() = runTest {
        val vm = LinkedPcsViewModel(store, FakeControl(PairResult.Expired), settings, auth, "title")
        vm.onScanned(qrText); vm.uiState.first { it.confirm != null }; vm.onAllow()
        assertEquals(LinkEvent.Expired, vm.events.first())
    }

    @Test fun `deny links nothing`() = runTest {
        val control = FakeControl()
        val vm = LinkedPcsViewModel(store, control, settings, auth, "title")
        vm.onScanned(qrText); vm.uiState.first { it.confirm != null }; vm.onDeny()
        vm.uiState.first { it.confirm == null }
        assertTrue(control.paired.isEmpty())
    }

    @Test fun `remove drops the PC`() = runTest {
        val control = FakeControl()
        val vm = LinkedPcsViewModel(store, control, settings, auth, "title")
        vm.onScanned(qrText); vm.uiState.first { it.confirm != null }; vm.onAllow(); vm.events.first()
        vm.remove(LinkCrypto.b64url(roomId))
        vm.uiState.first { it.pcs.isEmpty() }
        assertEquals(listOf(LinkCrypto.b64url(roomId)), control.removed)
    }
}
