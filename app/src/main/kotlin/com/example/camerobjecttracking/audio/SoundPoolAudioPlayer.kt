package com.example.camerobjecttracking.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.camerobjecttracking.model.MusicEvent
import java.io.File

/**
 * Lecteur audio utilisant SoundPool pour des samples personnalisés.
 * Optionnel: non implémenté en détail car nécessite des assets samples.
 */
class SoundPoolAudioPlayer(private val context: Context) : AudioPlayer {

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>() // instrument → soundId

    init {
        initSoundPool()
        loadDefaultSamples()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()
    }

    private fun loadDefaultSamples() {
        // Chercher les fichiers WAV/OGG dans assets/samples/instrument_<id>.wav
        // Non implémenté : besoin de convention de nommage
        Log.d(TAG, "SoundPool initialized, waiting for sample files")
    }

    override fun play(event: MusicEvent) {
        val sp = soundPool ?: return
        val soundId = soundMap[event.instrument]
        if (soundId != null) {
            // volume = event.velocity / 127f
            sp.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            Log.w(TAG, "No sample for instrument ${event.instrument}")
        }
    }

    override fun stop() {
        soundPool?.autoPause()
    }

    override fun setVolume(volume: Float) {
        soundPool?.setVolume(0, volume, volume) // set volume for all streams? (approximate)
    }

    override fun close() {
        soundPool?.release()
        soundPool = null
    }

    companion object {
        private const val TAG = "SoundPoolAudioPlayer"
    }
}
