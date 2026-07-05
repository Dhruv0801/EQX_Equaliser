package com.example.fxsoundreplica

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("FxSoundBoot", "Received action: $action")
        
        val serviceIntent = Intent(context, AudioService::class.java).apply {
            this.action = action
            if (intent.hasExtra(AudioEffect.EXTRA_AUDIO_SESSION)) {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1))
            }
        }
        
        try {
            if (action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION || 
                action == AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION ||
                action == Intent.ACTION_BOOT_COMPLETED) {
                context.startForegroundService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("FxSoundBoot", "Failed to start service", e)
        }
    }
}
