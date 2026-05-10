package com.example.camerobjecttracking

import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log

class ObjectDetector(context: android.content.Context, private val modelPath: String = "mobilenet_ssd_v2.tflite") {

    private var interpreter: Interpreter
    private val inputSize = 300
    private val numDetections = 10

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

    data class Detection(val boundingBox: RectF, val label: String, val confidence: Float)

    init {
        val modelBuffer = FileUtil.loadMappedFile(context, modelPath)
        interpreter = Interpreter(modelBuffer)
        Log.d("ObjectDetector", "Model loaded. Input tensors: ${interpreter.inputTensorCount}, Output tensors: ${interpreter.outputTensorCount}")
        for (i in 0 until interpreter.inputTensorCount) {
            val t = interpreter.getInputTensor(i)
            Log.d("ObjectDetector", "Input $i: shape=${t.shape().contentToString()}, type=${t.dataType()}")
        }
        for (i in 0 until interpreter.outputTensorCount) {
            val t = interpreter.getOutputTensor(i)
            Log.d("ObjectDetector", "Output $i: shape=${t.shape().contentToString()}, type=${t.dataType()}")
        }
    }

    fun detect(bitmap: Bitmap): List<Detection> {
        val inputShape = interpreter.getInputTensor(0).shape()
        val inputType = interpreter.getInputTensor(0).dataType()
        val width = inputShape[2]
        val height = inputShape[1]

        Log.d("ObjectDetector", "Input shape: ${inputShape.contentToString()}, type=$inputType, resize to ${width}x$height")

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
            Log.d("ObjectDetector", "UINT8 buffer: ${buf.capacity()} bytes")
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
            Log.d("ObjectDetector", "FLOAT32 buffer: ${buf.capacity()} bytes")
            buf
        }

        // Préparer les sorties selon le nombre de tensors
        val outputs = mutableMapOf<Int, Any>()
        for (i in 0 until interpreter.outputTensorCount) {
            when (i) {
                0 -> outputs[i] = Array(1) { Array(numDetections) { FloatArray(4) } }
                1 -> outputs[i] = Array(1) { FloatArray(numDetections) }
                2 -> outputs[i] = Array(1) { FloatArray(numDetections) }
                3 -> outputs[i] = FloatArray(1)
                else -> Log.w("ObjectDetector", "Output index $i non géré")
            }
        }

        // Inférence
        interpreter.runForMultipleInputsOutputs(arrayOf(buffer), outputs)

        // Extraction
        val boxes = outputs[0] as? Array<Array<FloatArray>>
        val classes = outputs[1] as? Array<FloatArray>
        val scores = outputs[2] as? Array<FloatArray>
        val numDet = (outputs[3] as? FloatArray)?.get(0)?.toInt() ?: 0

        Log.d("ObjectDetector", "Raw outputs: numDet=$numDet, scores=${scores?.get(0)?.contentToString()}")

        val detections = mutableListOf<Detection>()
        val count = minOf(numDet, numDetections)
        for (i in 0 until count) {
            val score = scores?.get(0)?.get(i) ?: 0f
            if (score > 0.5f) {
                val box = boxes?.get(0)?.get(i) ?: continue
                val cls = classes?.get(0)?.get(i)?.toInt() ?: continue
                val label = if (cls in classLabels.indices) classLabels[cls] else "unknown"
                detections.add(Detection(RectF(box[1], box[0], box[3], box[2]), label, score))
                Log.d("ObjectDetector", "Detected: $label ($score)")
            }
        }
        return detections
    }

    fun findDetectionAtPoint(x: Float, y: Float, detections: List<Detection>) =
        detections.sortedByDescending { it.confidence }.firstOrNull { it.boundingBox.contains(x, y) }

    fun calculateIoU(r1: RectF, r2: RectF): Float {
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

    fun matchDetection(selected: RectF, detections: List<Detection>, threshold: Float = 0.5f): Detection? {
        var best: Detection? = null
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

    fun close() = interpreter.close()
}
