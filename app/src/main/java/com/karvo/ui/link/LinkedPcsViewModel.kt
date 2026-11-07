package com.karvo.ui.link

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.karvo.KarvoApp
import com.karvo.R
import com.karvo.data.Authenticator
import com.karvo.data.SettingsRepository
import com.karvo.data.mayRevealEarnings
import com.karvo.link.LinkControl
import com.karvo.link.LinkQr
import com.karvo.link.LinkState
import com.karvo.link.PairResult
import com.karvo.link.PairingStore
import com.karvo.link.parseLinkQr
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PcRow(val roomId: String, val name: String, val state: LinkState?, val broken: Boolean)

data class LinkedPcsUi(val pcs: List<PcRow> = emptyList(), val confirm: LinkQr? = null, val busy: Boolean = false)

sealed interface LinkEvent {
    data object NotKarvo : LinkEvent
    data object Outdated : LinkEvent
    data object Expired : LinkEvent
    data object Failed : LinkEvent
    data class Linked(val pcName: String) : LinkEvent
}

class LinkedPcsViewModel(
    store: PairingStore,
    private val control: LinkControl,
    private val settings: SettingsRepository,
    private val authenticator: Authenticator,
    private val biometricTitle: String,
) : ViewModel() {
    private val local = MutableStateFlow(LinkedPcsUi())
    private val _events = Channel<LinkEvent>(Channel.BUFFERED)
    val events: Flow<LinkEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<LinkedPcsUi> = combine(store.pcs, control.states, local) { pcs, states, l ->
        l.copy(pcs = pcs.map { PcRow(it.roomId, it.pcName, states[it.roomId], it.secret == null) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LinkedPcsUi())

    fun onScanned(text: String) {
        val qr = try { parseLinkQr(text) } catch (e: IllegalArgumentException) {
            _events.trySend(if (e.message == "version") LinkEvent.Outdated else LinkEvent.NotKarvo); return
        }
        local.update { it.copy(confirm = qr) }
    }

    fun onDeny() = local.update { it.copy(confirm = null) }

    fun onAllow() {
        val qr = local.value.confirm ?: return
        local.update { it.copy(confirm = null) }
        viewModelScope.launch {
            // Linking gives a PC your lessons and earnings: the same check as revealing earnings.
            if (!mayRevealEarnings(settings, authenticator, biometricTitle)) return@launch
            local.update { it.copy(busy = true) }
            val event = when (val r = control.pair(qr)) {
                is PairResult.Linked -> LinkEvent.Linked(r.pcName)
                PairResult.Expired -> LinkEvent.Expired
                PairResult.Outdated -> LinkEvent.Outdated
                PairResult.Failed -> LinkEvent.Failed
            }
            local.update { it.copy(busy = false) }
            _events.send(event)
        }
    }

    fun remove(roomId: String) { viewModelScope.launch { control.remove(roomId) } }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KarvoApp
                val c = app.container
                LinkedPcsViewModel(c.pairingStore, c.linkManager, c.settingsRepository, c.authenticator, app.getString(R.string.link_biometric_title))
            }
        }
    }
}
