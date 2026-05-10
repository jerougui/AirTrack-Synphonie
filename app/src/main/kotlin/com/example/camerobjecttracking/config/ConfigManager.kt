package com.example.camerobjecttracking.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.example.camerobjecttracking.model.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Gère la configuration de l'application via fichier JSON + migration SharedPreferences.
 */
class ConfigManager(private val context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val configFile = File(context.filesDir, CONFIG_FILENAME)

    private val _config = MutableLiveData<AppConfig>(loadConfig())
    val config: LiveData<AppConfig> = _config

    companion object {
        private const val TAG = "ConfigManager"
        private const val CONFIG_FILENAME = "airtrack_config.json"
        private const val PREFS_MIGRATION_KEY = "prefs_migrated"
    }

    private fun loadConfig(): AppConfig {
        if (configFile.exists()) {
            try {
                val json = configFile.readText()
                return parseConfig(json) ?: getDefaultConfig()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse config, using defaults", e)
            }
        }

        if (!prefs.getBoolean(PREFS_MIGRATION_KEY, false)) {
            migrateFromPrefs()
            prefs.edit { putBoolean(PREFS_MIGRATION_KEY, true) }
        }

        val defaults = getDefaultConfig()
        saveConfig(defaults)
        return defaults
    }

    private fun parseConfig(json: String): AppConfig? {
        return try {
            val obj = JSONObject(json)
            AppConfig(
                audio = AudioConfig(
                    backend = AudioBackend.valueOf(obj.optJSONObject("audio")?.optString("backend", "MIDI") ?: "MIDI"),
                    defaultInstrument = obj.optJSONObject("audio")?.optInt("defaultInstrument", 73) ?: 73,
                    volume = obj.optJSONObject("audio")?.optDouble("volume", 0.8)?.toFloat() ?: 0.8f
                ),
                mapping = MappingConfig(
                    minSpeed = obj.optJSONObject("mapping")?.optDouble("minSpeed", 0.0)?.toFloat() ?: 0.0f,
                    maxSpeed = obj.optJSONObject("mapping")?.optDouble("maxSpeed", 3.0)?.toFloat() ?: 3.0f,
                    speedToPitch = obj.optJSONObject("mapping")?.optBoolean("speedToPitch", true) ?: true,
                    directionToInstrument = obj.optJSONObject("mapping")?.optBoolean("directionToInstrument", false) ?: false,
                    accelerationToEffect = obj.optJSONObject("mapping")?.optBoolean("accelerationToEffect", false) ?: false,
                    noteDebounceMs = obj.optJSONObject("mapping")?.optInt("noteDebounceMs", 100) ?: 100,
                    scale = obj.optJSONObject("mapping")?.optString("scale", "pentatonic") ?: "pentatonic",
                    baseNote = obj.optJSONObject("mapping")?.optInt("baseNote", 60) ?: 60,
                    instruments = obj.optJSONObject("mapping")?.optJSONArray("instruments")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: listOf("flute")
                ),
                detection = DetectionConfig(
                    confidenceThreshold = obj.optJSONObject("detection")?.optDouble("confidenceThreshold", 0.5)?.toFloat() ?: 0.5f,
                    maxDetections = obj.optJSONObject("detection")?.optInt("maxDetections", 10) ?: 10
                ),
                tracker = TrackerConfig(
                    kalmanProcessNoise = obj.optJSONObject("tracker")?.optDouble("kalmanProcessNoise", 0.05)?.toFloat() ?: 0.05f,
                    kalmanMeasurementNoise = obj.optJSONObject("tracker")?.optDouble("kalmanMeasurementNoise", 0.1)?.toFloat() ?: 0.1f,
                    iouThreshold = obj.optJSONObject("tracker")?.optDouble("iouThreshold", 0.3)?.toFloat() ?: 0.3f,
                    maxLostFrames = obj.optJSONObject("tracker")?.optInt("maxLostFrames", 5) ?: 5
                ),
                ui = UiConfig(
                    showTrajectory = obj.optJSONObject("ui")?.optBoolean("showTrajectory", true) ?: true,
                    trajectoryLength = obj.optJSONObject("ui")?.optInt("trajectoryLength", 60) ?: 60,
                    showFps = obj.optJSONObject("ui")?.optBoolean("showFps", true) ?: true,
                    showNote = obj.optJSONObject("ui")?.optBoolean("showNote", true) ?: true
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error", e)
            null
        }
    }

    fun saveConfig(config: AppConfig) {
        try {
            val json = JSONObject().apply {
                put("audio", JSONObject().apply {
                    put("backend", config.audio.backend.name)
                    put("defaultInstrument", config.audio.defaultInstrument)
                    put("volume", config.audio.volume)
                })
                put("mapping", JSONObject().apply {
                    put("minSpeed", config.mapping.minSpeed)
                    put("maxSpeed", config.mapping.maxSpeed)
                    put("speedToPitch", config.mapping.speedToPitch)
                    put("directionToInstrument", config.mapping.directionToInstrument)
                    put("accelerationToEffect", config.mapping.accelerationToEffect)
                    put("noteDebounceMs", config.mapping.noteDebounceMs)
                    put("scale", config.mapping.scale)
                    put("baseNote", config.mapping.baseNote)
                    put("instruments", org.json.JSONArray(config.mapping.instruments))
                })
                put("detection", JSONObject().apply {
                    put("confidenceThreshold", config.detection.confidenceThreshold)
                    put("maxDetections", config.detection.maxDetections)
                })
                put("tracker", JSONObject().apply {
                    put("kalmanProcessNoise", config.tracker.kalmanProcessNoise)
                    put("kalmanMeasurementNoise", config.tracker.kalmanMeasurementNoise)
                    put("iouThreshold", config.tracker.iouThreshold)
                    put("maxLostFrames", config.tracker.maxLostFrames)
                })
                put("ui", JSONObject().apply {
                    put("showTrajectory", config.ui.showTrajectory)
                    put("trajectoryLength", config.ui.trajectoryLength)
                    put("showFps", config.ui.showFps)
                    put("showNote", config.ui.showNote)
                })
            }

            FileOutputStream(configFile).use { fos ->
                fos.write(json.toString().toByteArray())
            }
            _config.postValue(config)
            Log.d(TAG, "Config saved")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config", e)
        }
    }

    fun resetToDefaults() {
        val defaults = getDefaultConfig()
        saveConfig(defaults)
    }

    fun getCurrentConfig(): AppConfig = _config.value ?: getDefaultConfig()

    private fun migrateFromPrefs() {
        val ip = prefs.getString("udp_ip", "192.168.1.100") ?: "192.168.1.100"
        val port = prefs.getString("udp_port", "5005")?.toIntOrNull() ?: 5005
        val freq = prefs.getString("udp_frequency", "30")?.toIntOrNull() ?: 30
        Log.d(TAG, "Prefs migration UDP: $ip:$port @ ${freq}Hz (prefs kept separate)")
        // UDP settings restent dans SharedPreferences pour v1
    }

    private fun getDefaultConfig(): AppConfig {
        return AppConfig(
            audio = AudioConfig(
                backend = AudioBackend.MIDI,
                defaultInstrument = 73,
                volume = 0.8f
            ),
            mapping = MappingConfig(
                minSpeed = 0.0f,
                maxSpeed = 3.0f,
                speedToPitch = true,
                directionToInstrument = false,
                accelerationToEffect = false,
                noteDebounceMs = 100,
                scale = "pentatonic",
                baseNote = 60,
                instruments = listOf("flute")
            ),
            detection = DetectionConfig(
                confidenceThreshold = 0.5f,
                maxDetections = 10
            ),
            tracker = TrackerConfig(
                kalmanProcessNoise = 0.05f,
                kalmanMeasurementNoise = 0.1f,
                iouThreshold = 0.3f,
                maxLostFrames = 5
            ),
            ui = UiConfig(
                showTrajectory = true,
                trajectoryLength = 60,
                showFps = true,
                showNote = true
            )
        )
    }
}
