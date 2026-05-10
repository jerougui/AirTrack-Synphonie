package com.example.camerobjecttracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camerobjecttracking.audio.AudioPlayer
import com.example.camerobjecttracking.audio.MidiAudioPlayer
import com.example.camerobjecttracking.audio.SoundPoolAudioPlayer
import com.example.camerobjecttracking.camera.CameraCaptureImpl
import com.example.camerobjecttracking.config.ConfigManager
import com.example.camerobjecttracking.detection.ObjectDetectorInterface
import com.example.camerobjecttracking.detection.TFLiteObjectDetector
import com.example.camerobjecttracking.mapping.MusicMapperImpl
import com.example.camerobjecttracking.model.AudioBackend
import com.example.camerobjecttracking.tracking.TrackingEngine
import com.example.camerobjecttracking.utils.toBitmap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var trackingOverlayView: TrackingOverlayView
    private lateinit var toggleTrackingButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var udpStatusTextView: TextView
    private lateinit var settingsButton: View

    // Modules
    private lateinit var cameraCapture: CameraCaptureImpl
    private lateinit var objectDetector: ObjectDetectorInterface
    private lateinit var trackingEngine: TrackingEngine
    private lateinit var configManager: ConfigManager
    private lateinit var musicMapper: MusicMapperImpl
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var analysisExecutor: ExecutorService

    private lateinit var udpSender: UdpSender

    private var isTrackingEnabled = false
    private var trackedLabel: String? = null

    // Dernières détections pour sélection tactile
    private var latestDetections: List<ObjectDetectorInterface.Detection> = emptyList()

    companion object {
        private const val SETTINGS_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        trackingOverlayView = findViewById(R.id.overlayView)
        toggleTrackingButton = findViewById(R.id.toggleTrackingButton)
        statusTextView = findViewById(R.id.statusTextView)
        udpStatusTextView = findViewById(R.id.udpStatusTextView)
        settingsButton = findViewById(R.id.settingsButton)

        // Initialisation des modules
        configManager = ConfigManager(this)
        objectDetector = TFLiteObjectDetector(this)
        analysisExecutor = Executors.newSingleThreadExecutor()
        cameraCapture = CameraCaptureImpl()
        trackingEngine = TrackingEngine(
            detector = objectDetector,
            config = configManager.getCurrentConfig().tracker
        )
        musicMapper = MusicMapperImpl(configManager)
        // Audio backend selon config
        val audioBackend = configManager.getCurrentConfig().audio.backend
        audioPlayer = when (audioBackend) {
            AudioBackend.MIDI -> MidiAudioPlayer(this)
            AudioBackend.SOUNDPOOL -> SoundPoolAudioPlayer(this)
        }
        audioPlayer.setVolume(configManager.getCurrentConfig().audio.volume)

        udpSender = UdpSender(this)
        udpSender.initialize()

        updateUdpStatus()

        // Permission caméra
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // UI listeners
        toggleTrackingButton.setOnClickListener {
            isTrackingEnabled = !isTrackingEnabled
            toggleTrackingButton.text = if (isTrackingEnabled) "Arrêter suivi" else "Démarrer suivi"
            statusTextView.text = if (isTrackingEnabled) "Suivi en cours..." else "Prêt"
            updateUdpStatus()

            if (isTrackingEnabled) {
                udpSender.startSending()
                trackingEngine // TODO: reset ou start?
            } else {
                udpSender.stopSending()
                trackingOverlayView.clearTracking()
                trackedLabel = null
                trackingEngine.reset()
            }
        }

        settingsButton.setOnClickListener {
            startActivityForResult(Intent(this, SettingsActivity::class.java), SETTINGS_REQUEST_CODE)
        }

        // Sélection d'objet par toucher
        // Sélection d'objet par toucher
        previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val normalizedX = event.x / previewView.width
                val normalizedY = event.y / previewView.height

                val selectedDetection = objectDetector.findDetectionAtPoint(normalizedX, normalizedY, latestDetections)

                if (selectedDetection != null) {
                    trackedLabel = selectedDetection.label
                    isTrackingEnabled = true
                    toggleTrackingButton.text = "Arrêter suivi"
                    statusTextView.text = "Suivi: ${selectedDetection.label} (${(selectedDetection.confidence * 100).toInt()}%)"
                    updateUdpStatus()

                    // Réinitialiser le moteur de tracking avec l'objet sélectionné
                    trackingEngine.reset()
                    trackingEngine.update(latestDetections, System.currentTimeMillis())

                    udpSender.startSending()
                } else {
                    statusTextView.text = "Aucun objet détecté à cet endroit"
                }
                true
            } else false
        }
            false
        }
    }

    private fun updateUdpStatus() {
        udpStatusTextView.text = if (udpSender.isReady()) "UDP: ✓" else "UDP: ✗"
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private fun startCamera() {
        cameraCapture.start(this, previewView) { imageProxy ->
            // Callback sur thread d'analyse
            val bitmap = imageProxy.toBitmap()
            latestDetections = objectDetector.detect(bitmap)

            val trackingData = if (isTrackingEnabled) {
                trackingEngine.update(latestDetections, imageProxy.timestamp)
            } else null

            // Mapping mouvement → musique
            val musicEvent = trackingData?.let { musicMapper.map(it) }

            imageProxy.close()

            // UI thread
            runOnUiThread {
                if (trackingData != null) {
                    trackingData.boundingBox?.let { box ->
                        val left = box.left * trackingOverlayView.width
                        val top = box.top * trackingOverlayView.height
                        val right = box.right * trackingOverlayView.width
                        val bottom = box.bottom * trackingOverlayView.height
                        val centerX = trackingData.normalizedX * trackingOverlayView.width
                        val centerY = trackingData.normalizedY * trackingOverlayView.height
                        trackingOverlayView.updateTracking(
                            left, top, right, bottom,
                            centerX, centerY,
                            trackingData.normalizedX, trackingData.normalizedY,
                            cameraCapture.getCurrentFps()
                        )
                    }
                    udpSender.updateCoordinates(trackingData.normalizedX, trackingData.normalizedY)
                    statusTextView.text = "Suivi: ${trackedLabel ?: "objet"} (vitesse: %.2f)".format(trackingData.speed)
                } else if (isTrackingEnabled) {
                    statusTextView.text = "Objet perdu..."
                }

                // Jouer la note (non bloquant)
                musicEvent?.let { audioPlayer.play(it) }
            }
        }
    }
                    udpSender.updateCoordinates(trackingData.normalizedX, trackingData.normalizedY)
                    statusTextView.text = "Suivi: ${trackedLabel ?: "objet"} (vitesse: %.2f)".format(trackingData.speed)
                } else if (isTrackingEnabled) {
                    statusTextView.text = "Objet perdu..."
                }
            }
            }
        }
    }

}

    private fun processImage(image: ImageProxy) {
        frameCount++
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFpsTime >= 1000) {
            currentFps = frameCount
            frameCount = 0
            lastFpsTime = currentTime
        }
        
        if (isTrackingEnabled && trackedObjectRect != null) {
            if (currentTime - lastDetectionTime < detectionIntervalMs) {
                return
            }
        }
        
        lastDetectionTime = currentTime
        
        val bitmap = image.toBitmap()
        
        val detections = objectDetector.detect(bitmap)
        latestDetections = detections
        
        runOnUiThread {
            if (isTrackingEnabled && trackedObjectRect != null && detections.isNotEmpty()) {
                val matchedDetection = objectDetector.matchDetection(trackedObjectRect!!, detections)
                
                if (matchedDetection != null) {
                    trackedObjectRect = matchedDetection.boundingBox
                    trackedLabel = matchedDetection.label
                    
                    val viewRect = normalizeRectToView(matchedDetection.boundingBox)
                    val centerX = (matchedDetection.boundingBox.left + matchedDetection.boundingBox.right) / 2f
                    val centerY = (matchedDetection.boundingBox.top + matchedDetection.boundingBox.bottom) / 2f
                    trackingOverlayView.updateTracking(
                        viewRect.left, viewRect.top, viewRect.right, viewRect.bottom,
                        centerX * previewView.width,
                        centerY * previewView.height,
                        centerX, centerY, currentFps
                    )
                    
                    statusTextView.text = "Suivi: ${matchedDetection.label} (${(matchedDetection.confidence * 100).toInt()}%)"
                    
                    udpSender.updateCoordinates(centerX, centerY)
                } else {
                    statusTextView.text = "Objet perdu..."
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED && cameraProviderFuture == null) {
            startCamera()
        }
        updateUdpStatus()
    }

    override fun onPause() {
        super.onPause()
        if (isTrackingEnabled) {
            isTrackingEnabled = false
            toggleTrackingButton.text = "Démarrer suivi"
            statusTextView.text = "Suivi en pause"
            udpStatusTextView.text = "UDP: ✗"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        udpSender.release()
        objectDetector.close()
        analysisExecutor.shutdown()
        cameraCapture.stop()
        audioPlayer.close()
    }
}

/**
 * Extension function to convert ImageProxy (YUV format) to Bitmap.
 */
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = this.planes[0].buffer // Y
    val uBuffer = this.planes[1].buffer // U
    val vBuffer = this.planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)  // V then U for NV21
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
