package com.example.camerobjecttracking.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Implémentation TensorFlow Lite de ObjectDetectorInterface.
 * Utilise le modèle MobileNet SSD v2 (COCO 80 classes) embarqué dans les assets.
 *
 * @param context Context Android pour accéder aux assets
 * @param modelPath Chemin du modèle TFLite dans assets (défaut: mobilenet_ssd_v2.tflite)
 */
class TFLiteObjectDetector(
    context: Context,
    private val modelPath: String = "mobilenet_ssd_v2.tflite"
) : ObjectDetectorInterface {

    private var interpreter: Interpreter
    private val inputSize = 300
    private val numDetections = 10

    // Labels COCO (80 classes)
    private val classLabels = listOf(
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck",
        "boat", "traffic light", "fire hydrant", "stop sign", "parking meter", "bench",
        "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra",
        "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
        "skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
        "skateboard", "surfboard", "tennis racket", "bottle", "wine glass", "cup",
        "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich", "orange",
        "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
        "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse",
        "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink",
        "refrigerator", "book", "clock", "vase", "scissors", "teddy bear", "hair drier",
        "toothbrush"
    )

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(modelBuffer)
        Log.d(TAG, "Model loaded. Input tensors: ${interpreter.inputTensorCount}, Output tensors: ${interpreter.outputTensorCount}")
    }

    override fun detect(bitmap: Bitmap): List<ObjectDetectorInterface.Detection> {
        val inputShape = interpreter.getInputTensor(0).shape()
        val inputType = interpreter.getInputTensor(0).dataType
        val width = inputShape[2]
        val height = inputShape[1]

        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val buffer: ByteBuffer = if (inputType == org.tensorflow.lite.DataType.UINT8) {
            val buf = ByteBuffer.allocateDirect(1 * width * height * 3).order(ByteOrder.nativeOrder())
            val pixels = IntArray(width * height)
            resized.getPixels(pixels, 0, width, 0, 0, width, height)
            for (pixel in pixels) {
                buf.put((pixel shr 16 and 0xFF).toByte())
                buf.put((pixel shr 8 and 0xFF).toByte())
                buf.put((pixel and 0xFF).toByte())
            }
            buf.rewind()
            buf
        } else {
            val buf = ByteBuffer.allocateDirect(4 * width * height * 3).order(ByteOrder.nativeOrder())
            val pixels = IntArray(width * height)
            resized.getPixels(pixels, 0, width, 0, 0, width, height)
            for (pixel in pixels) {
                buf.putFloat((pixel shr 16 and 0xFF) / 255.0f)
                buf.putFloat((pixel shr 8 and 0xFF) / 255.0f)
                buf.putFloat((pixel and 0xFF) / 255.0f)
            }
            buf.rewind()
            buf
        }

        // Préparer les sorties
        val outputs = mutableMapOf<Int, Any>()
        for (i in 0 until interpreter.outputTensorCount) {
            when (i) {
                0 -> outputs[i] = Array(1) { Array(numDetections) { FloatArray(4) } }
                1 -> outputs[i] = Array(1) { FloatArray(numDetections) }
                2 -> outputs[i] = Array(1) { FloatArray(numDetections) }
                3 -> outputs[i] = FloatArray(1)
                else -> Log.w(TAG, "Output index $i non géré")
            }
        }

        // Inférence
        interpreter.runForMultipleInputsOutputs(arrayOf(buffer), outputs)

        // Extraction des résultats
        val boxes = outputs[0] as? Array<Array<FloatArray>>
        val classes = outputs[1] as? Array<FloatArray>
        val scores = outputs[2] as? Array<FloatArray>
        val numDet = (outputs[3] as? FloatArray)?.get(0)?.toInt() ?: 0

        val detections = mutableListOf<ObjectDetectorInterface.Detection>()
        val count = minOf(numDet, numDetections)
        for (i in 0 until count) {
            val score = scores?.get(0)?.get(i) ?: 0f
            if (score > 0.5f) {
                val box = boxes?.get(0)?.get(i) ?: continue
                val cls = classes?.get(0)?.get(i)?.toInt() ?: continue
                val label = if (cls in classLabels.indices) classLabels[cls] else "unknown"
                // Convertir coordonnées normalisées [0,1] (box=[y1, x1, y2, x2] => RectF(left, top, right, bottom))
                detections.add(
                    ObjectDetectorInterface.Detection(
                        boundingBox = RectF(box[1], box[0], box[3], box[2]),
                        label = label,
                        confidence = score
                    )
                )
                Log.d(TAG, "Detected: $label ($score)")
            }
        }
        return detections
    }

    override fun findDetectionAtPoint(x: Float, y: Float, detections: List<ObjectDetectorInterface.Detection>): ObjectDetectorInterface.Detection? {
        return detections.sortedByDescending { it.confidence }.firstOrNull { it.boundingBox.contains(x, y) }
    }

    override fun matchDetection(selected: RectF, detections: List<ObjectDetectorInterface.Detection>, threshold: Float): ObjectDetectorInterface.Detection? {
        var best: ObjectDetectorInterface.Detection? = null
        var bestIoU = 0f
        for (d in detections) {
            val iou = calculateIoU(selected, d.boundingBox)
            if (iou >= threshold && iou > bestIoU) {
                bestIoU = iou
                best = d
            }
        }
        return best
    }

    private fun calculateIoU(r1: RectF, r2: RectF): Float {
        val xL = maxOf(r1.left, r2.left)
        val yT = maxOf(r1.top, r2.top)
        val xR = minOf(r1.right, r2.right)
        val yB = minOf(r1.bottom, r2.bottom)
        if (xR <= xL || yB <= yT) return 0f
        val inter = (xR - xL) * (yB - yT)
        val area1 = (r1.right - r1.left) * (r1.bottom - r1.top)
        val area2 = (r2.right - r2.left) * (r2.bottom - r2.top)
        return inter / (area1 + area2 - inter)
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        private const val TAG = "TFLiteObjectDetector"
    }
}
