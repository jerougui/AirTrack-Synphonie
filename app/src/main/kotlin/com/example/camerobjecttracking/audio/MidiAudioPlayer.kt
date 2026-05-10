package com.example.camerobjecttracking.audio

import android.content.Context
import android.media.midi.*
import android.util.Log
import com.example.camerobjecttracking.model.MusicEvent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Lecteur audio utilisant l'API Android MIDI (General MIDI).
 * Nécessite un synthétiseur MIDI disponible sur le device.
 */
class MidiAudioPlayer(private val context: Context) : AudioPlayer {

    private var midiManager: MidiManager? = null
    private var midiDevice: MidiDevice? = null
    private var outputPort: MidiOutputPort? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    companion object {
        private const val TAG = "MidiAudioPlayer"
        private const val DEFAULT_CHANNEL = 0 // channel 0 (1 en MIDI 1-indexed for some messages)
    }

    init {
        openDevice()
    }

    private fun openDevice() {
        try {
            midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
            val infos = midiManager?.devices ?: emptyArray()
            if (infos.isEmpty()) {
                Log.e(TAG, "No MIDI devices found")
                return
            }
            // Chercher un synthétiseur (type synthesizer) ou prendre le premier
            val synthInfo = infos.firstOrNull { it.type == MidiDeviceInfo.TYPE_SYNTHESIZER } ?: infos[0]
            midiManager?.openDevice(synthInfo, object : MidiManager.OnDeviceOpenedListener {
                override fun onDeviceOpened(device: MidiDevice?) {
                    if (device == null) {
                        Log.e(TAG, "Failed to open MIDI device")
                        return
                    }
                    midiDevice = device
                    outputPort = device.openOutputPort(0) // port 0 usually main output
                    Log.d(TAG, "MIDI device opened: ${synthInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)}")
                }
            }, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening MIDI device", e)
        }
    }

    override fun play(event: MusicEvent) {
        val port = outputPort ?: run {
            Log.w(TAG, "No output port, cannot play note")
            return
        }

        // Envoi PROGRAM_CHANGE si instrument différent du précédent? Pour l'instant on envoie à chaque fois.
        sendProgramChange(event.instrument)

        // NOTE_ON: status byte = 0x90 | channel, data1 = note, data2 = velocity
        val noteOn = byteArrayOf((0x90 or DEFAULT_CHANNEL).toByte(), event.note.toByte(), event.velocity.toByte())
        port.send(noteOn, 0, noteOn.size, System.nanoTime() / 1000)

        // Programmer NOTE_OFF après durationMs
        scheduler.schedule({
            val noteOff = byteArrayOf((0x80 or DEFAULT_CHANNEL).toByte(), event.note.toByte(), 0.toByte())
            port.send(noteOff, 0, noteOff.size, System.nanoTime() / 1000)
        }, event.durationMs.toLong(), TimeUnit.MILLISECONDS)
    }

    private fun sendProgramChange(program: Int) {
        val port = outputPort ?: return
        val msg = byteArrayOf((0xC0 or DEFAULT_CHANNEL).toByte(), program.toByte())
        port.send(msg, 0, msg.size, System.nanoTime() / 1000)
        Log.d(TAG, "Program change: $program")
    }

    override fun stop() {
        // Arrêter tous les notes en cours (all notes off)
        val port = outputPort ?: return
        for (note in 0..127) {
            val noteOff = byteArrayOf((0x80 or DEFAULT_CHANNEL).toByte(), note.toByte(), 0.toByte())
            port.send(noteOff, 0, noteOff.size, System.nanoTime() / 1000)
        }
    }

    override fun setVolume(volume: Float) {
        // MIDI volume contrôle via Control Change (7) ou channel volume (11)
        val port = outputPort ?: return
        val msg = byteArrayOf(
            (0xB0 or DEFAULT_CHANNEL).toByte(), // Control Change
            7, // Main volume
            (volume * 127).toInt().toByte()
        )
        port.send(msg, 0, msg.size, System.nanoTime() / 1000)
    }

    override fun close() {
        scheduler.shutdownNow()
        outputPort?.close()
        midiDevice?.close()
        midiManager?.close()
    }
}
