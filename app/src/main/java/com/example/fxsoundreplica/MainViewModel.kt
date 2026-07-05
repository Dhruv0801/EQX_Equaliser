package com.example.fxsoundreplica

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AudioSettings())
    val uiState: StateFlow<AudioSettings> = _uiState.asStateFlow()

    private val _presets = MutableStateFlow(listOf(
        AudioSettings.Music, 
        AudioSettings.Gaming, 
        AudioSettings.Movie,
        AudioSettings.CarDSP,
        AudioSettings.DeepFieldKTV,
        AudioSettings.Panoramic
    ))
    val presets: StateFlow<List<AudioSettings>> = _presets.asStateFlow()

    private var audioService: AudioService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioService.LocalBinder
            audioService = binder.getService()
            isBound = true
            audioService?.updateSettings(_uiState.value)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isBound = false
        }
    }

    fun bindService(context: Context) {
        val intent = Intent(context, AudioService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
        audioService?.updateSettings(_uiState.value)
    }

    fun updateClarity(value: Float) {
        _uiState.update { it.copy(clarity = value) }
        audioService?.updateSettings(_uiState.value)
    }

    fun updateAmbience(value: Float) {
        _uiState.update { it.copy(ambience = value) }
        audioService?.updateSettings(_uiState.value)
    }

    fun updateSurround(value: Float) {
        _uiState.update { it.copy(surround = value) }
        audioService?.updateSettings(_uiState.value)
    }

    fun updateDynamicBoost(value: Float) {
        _uiState.update { it.copy(dynamicBoost = value) }
        audioService?.updateSettings(_uiState.value)
    }

    fun updateBassBoost(value: Float) {
        _uiState.update { it.copy(bassBoost = value) }
        audioService?.updateSettings(_uiState.value)
    }

    fun updateReverb(value: Float) {
        _uiState.update { it.copy(reverb = value) }
        audioService?.updateSettings(_uiState.value)
    }

    fun updateEqBand(index: Int, value: Float) {
        _uiState.update { state ->
            val newBands = state.eqBands.toMutableList()
            newBands[index] = value
            state.copy(eqBands = newBands)
        }
        audioService?.updateSettings(_uiState.value)
    }

    fun applyPreset(settings: AudioSettings) {
        _uiState.value = settings.copy(isEnabled = _uiState.value.isEnabled)
        audioService?.updateSettings(_uiState.value)
    }

    fun saveCurrentAsPreset(name: String) {
        val newPreset = _uiState.value.copy(id = UUID.randomUUID().toString(), name = name)
        _presets.update { it + newPreset }
    }

    fun deletePreset(id: String) {
        _presets.update { it.filterNot { preset -> preset.id == id } }
    }
}
