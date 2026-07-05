package com.example.fxsoundreplica

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.ConcurrentHashMap

class AudioService : Service() {

    private val TAG = "EqxService"
    private val binder = LocalBinder()
    private val activeSessions = ConcurrentHashMap<Int, AudioEffectsBundle>()
    private var currentSettings = AudioSettings()
    
    private var globalBundle: AudioEffectsBundle? = null

    inner class LocalBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }

    private val audioSessionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val sessionId = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1) ?: -1
            if (sessionId != -1 && sessionId != 0) {
                if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
                    addSession(sessionId)
                } else if (action == AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) {
                    removeSession(sessionId)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        startForegroundService()
        
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(audioSessionReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(audioSessionReceiver, filter)
        }
        
        initGlobalSession()
    }

    private fun initGlobalSession() {
        try {
            globalBundle?.release()
            globalBundle = AudioEffectsBundle(0)
            applySettingsToBundle(globalBundle!!, currentSettings)
            Log.d(TAG, "Global Session Hooked - SUPREME ULTRA Mode")
        } catch (e: Exception) {
            Log.e(TAG, "Global Hook Fail: ${e.message}")
        }
    }

    private fun startForegroundService() {
        val channelId = "eqx_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "EQX", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("EQX: SUPREME ULTRA ACTIVE")
            .setContentText("Exclusive hardware control enabled...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val sessionId = intent?.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1) ?: -1
        
        if (sessionId != -1 && sessionId != 0) {
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) {
                addSession(sessionId)
            } else if (action == AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) {
                removeSession(sessionId)
            }
        }
        initGlobalSession()
        return START_STICKY
    }

    private fun addSession(sessionId: Int) {
        if (sessionId <= 0) return
        if (!activeSessions.containsKey(sessionId)) {
            try {
                val bundle = AudioEffectsBundle(sessionId)
                applySettingsToBundle(bundle, currentSettings)
                activeSessions[sessionId] = bundle
            } catch (e: Exception) {
                Log.e(TAG, "App session $sessionId fail")
            }
        }
    }

    private fun removeSession(sessionId: Int) {
        activeSessions.remove(sessionId)?.release()
    }

    fun updateSettings(settings: AudioSettings) {
        currentSettings = settings
        if (globalBundle == null) initGlobalSession()
        globalBundle?.let { applySettingsToBundle(it, settings) }
        activeSessions.values.forEach { bundle -> applySettingsToBundle(bundle, settings) }
    }

    private fun applySettingsToBundle(bundle: AudioEffectsBundle, settings: AudioSettings) {
        try {
            val enabled = settings.isEnabled
            bundle.apply {
                // 1. Equalizer - Extreme High Fidelity
                equalizer?.let { eq ->
                    eq.enabled = false
                    if (enabled) {
                        val numBands = eq.numberOfBands.toInt()
                        for (i in 0 until numBands) {
                            val ratio = if (numBands > 1) i.toFloat() / (numBands - 1) else 0f
                            val sliderIdx = (ratio * (settings.eqBands.size - 1)).toInt()
                            var gain = settings.eqBands[sliderIdx]
                            
                            // Apply custom clarity/bass only if they are NOT zero
                            if (settings.bassBoost > 0 && ratio < 0.25f) gain += settings.bassBoost * 2.5f
                            if (settings.clarity > 0 && ratio > 0.75f) gain += settings.clarity * 3.5f
                            
                            eq.setBandLevel(i.toShort(), (gain * 100).toInt().toShort().coerceIn(-1500, 1500))
                        }
                        eq.enabled = true
                    }
                }

                // 2. Bass Boost
                bassBoost?.let { bb ->
                    bb.enabled = false
                    if (enabled && settings.bassBoost > 0) {
                        bb.setStrength(1000.toShort()) 
                        bb.enabled = true
                    }
                }

                // 3. Virtualizer (Surround) - SUPREME WIDTH
                virtualizer?.let { v ->
                    v.enabled = false
                    if (enabled && settings.surround > 0) {
                        v.setStrength(1000.toShort()) 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            try { v.forceVirtualizationMode(Virtualizer.VIRTUALIZATION_MODE_BINAURAL) } catch(e: Exception) {}
                        }
                        v.enabled = true
                    }
                }

                // 4. Environmental Reverb (Ambience) - SUPREME SPATIAL
                envReverb?.let { rev ->
                    rev.enabled = false
                    if (enabled && settings.ambience > 0) {
                        val lvl = settings.ambience
                        // SUPREME Tuning: Hardware Absolute Max
                        rev.roomLevel = 0.toShort() 
                        rev.roomHFLevel = 0.toShort()
                        rev.decayTime = (7000 + lvl * 3000).toInt().coerceIn(100, 20000) // Up to 37s trail
                        rev.decayHFRatio = 2000.toShort()
                        rev.reflectionsLevel = 1000.toShort()
                        rev.reflectionsDelay = 350
                        rev.reverbLevel = 2000.toShort()
                        rev.reverbDelay = 150
                        rev.diffusion = 1000.toShort()
                        rev.density = 1000.toShort()
                        rev.enabled = true
                    }
                }
                
                // 5. Preset Reverb (Dedicated Layer) - SUPREME ECHO
                presetReverb?.let { rev ->
                    rev.enabled = false
                    if (enabled && settings.reverb > 0) {
                        val lvl = settings.reverb
                        rev.preset = when {
                            lvl > 8f -> PresetReverb.PRESET_LARGEHALL
                            lvl > 6f -> PresetReverb.PRESET_MEDIUMHALL
                            lvl > 4f -> PresetReverb.PRESET_SMALLROOM
                            else -> PresetReverb.PRESET_PLATE
                        }
                        rev.enabled = true
                    }
                }

                // 6. Loudness Enhancer
                loudnessEnhancer?.let { le ->
                    le.enabled = false
                    if (enabled) {
                        val gainMb = (settings.dynamicBoost * 500)
                        le.setTargetGain(gainMb.toInt().coerceIn(0, 10000))
                        le.enabled = true
                    }
                }
                
                // 7. Dynamics Processing
                dynamicsProcessing?.let { dp ->
                    dp.enabled = false
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val b = DynamicsProcessing.Config.Builder(DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2, true, 1, true, 1, true, 1, true)
                        val lim = DynamicsProcessing.Limiter(true, true, 0, 0.1f, 20.0f, 50.0f, -0.5f, 0f)
                        b.setLimiterAllChannelsTo(lim)
                        val mbc = DynamicsProcessing.Mbc(true, true, 1)
                        val mb = DynamicsProcessing.MbcBand(true, 20000.0f, 1.0f, 15.0f, 25.0f, -60f, 0.0f, 0.0f, 0.0f, 0.0f, settings.dynamicBoost * 10f)
                        mbc.setBand(0, mb)
                        b.setMbcAllChannelsTo(mbc)
                        
                        try {
                            val methods = dp.javaClass.methods
                            val setMethod = methods.find { it.name == "setProperties" } ?: methods.find { it.name == "setConfig" }
                            setMethod?.invoke(dp, b.build())
                        } catch (e: Exception) {}
                        dp.enabled = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SUPREME Apply fail")
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(audioSessionReceiver) } catch (e: Exception) {}
        globalBundle?.release()
        activeSessions.values.forEach { it.release() }
        activeSessions.clear()
        super.onDestroy()
    }

    private class AudioEffectsBundle(val sessionId: Int) {
        var equalizer: Equalizer? = null
        var bassBoost: BassBoost? = null
        var virtualizer: Virtualizer? = null
        var envReverb: EnvironmentalReverb? = null
        var presetReverb: PresetReverb? = null
        var loudnessEnhancer: LoudnessEnhancer? = null
        var dynamicsProcessing: DynamicsProcessing? = null

        init {
            try {
                equalizer = Equalizer(1000, sessionId)
                bassBoost = BassBoost(1000, sessionId)
                virtualizer = Virtualizer(1000, sessionId)
                envReverb = EnvironmentalReverb(1000, sessionId)
                presetReverb = PresetReverb(1000, sessionId)
                loudnessEnhancer = LoudnessEnhancer(sessionId)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val cfg = DynamicsProcessing.Config.Builder(DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2, false, 0, false, 0, false, 0, false).build()
                    dynamicsProcessing = DynamicsProcessing(1000, sessionId, cfg)
                }
            } catch (e: Exception) {
                Log.e("EqxBundle", "Init fail $sessionId")
            }
        }

        fun release() {
            try {
                equalizer?.release()
                bassBoost?.release()
                virtualizer?.release()
                envReverb?.release()
                presetReverb?.release()
                loudnessEnhancer?.release()
                dynamicsProcessing?.release()
            } catch (e: Exception) {}
        }
    }
}
