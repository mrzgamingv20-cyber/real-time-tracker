package com.realtimetracker.app.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90TransformOp
import timber.log.Timber
import java.nio.MappedByteBuffer

data class YoloDetection(
    val classIndex: Int,
    val className: String,
    val confidence: Float,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
) {
    val centerX: Float get() = (x1 + x2) / 2
    val centerY: Float get() = (y1 + y2) / 2
    val width: Float get() = x2 - x1
    val height: Float get() = y2 - y1
}

class YoloDetector(private val context: Context) {

    companion object {
        private const val MODEL_NAME = "yolov5s.tflite"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.5f
        private const val NMS_THRESHOLD = 0.4f
        private const val MAX_RESULTS = 100

        private val COCO_CLASSES = arrayOf(
            "person", "bicycle", "car", "motorcycle", "airplane",
            "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench", "cat",
            "dog", "horse", "sheep", "cow", "elephant",
            "bear", "zebra", "giraffe", "backpack", "umbrella",
            "handbag", "tie", "suitcase", "frisbee", "skis",
            "snowboard", "sports ball", "kite", "baseball bat", "baseball glove",
            "skateboard", "surfboard", "tennis racket", "bottle", "wine glass",
            "cup", "fork", "knife", "spoon", "bowl",
            "banana", "apple", "sandwich", "orange", "broccoli",
            "carrot", "hot dog", "pizza", "donut", "cake",
            "chair", "couch", "potted plant", "bed", "dining table",
            "toilet", "tv", "laptop", "mouse", "remote",
            "keyboard", "microwave", "oven", "toaster", "sink",
            "refrigerator", "book", "clock", "vase", "scissors",
            "teddy bear", "hair drier", "toothbrush"
        )
    }

    private var interpreter: Interpreter? = null
    private var inputTensorImage: TensorImage? = null
    private var imageProcessor: ImageProcessor? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_NAME)
            interpreter = Interpreter(modelBuffer, Interpreter.Options().setNumThreads(4))
            inputTensorImage = TensorImage(org.tensorflow.lite.DataType.UINT8)
            imageProcessor = ImageProcessor.Builder()
                .add(Rot90TransformOp(0))
                .build()
            Timber.d("YOLO model loaded successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to load YOLO model")
            // Model not found - will use mock detections for testing
        }
    }

    fun detectObjects(bitmap: Bitmap): List<YoloDetection> {
        return try {
            if (interpreter == null) {
                return getMockDetections(bitmap.width, bitmap.height)
            }

            // Resize bitmap to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

            // Prepare input
            inputTensorImage?.load(resizedBitmap)
            val processedImage = imageProcessor?.process(inputTensorImage)

            // Run inference
            val output = Array(1) { Array(25200) { FloatArray(85) } }
            interpreter?.run(processedImage?.buffer, output)

            // Parse outputs
            val detections = parseOutput(output[0], bitmap.width.toFloat(), bitmap.height.toFloat())
            Timber.d("Detected ${detections.size} objects")
            detections
        } catch (e: Exception) {
            Timber.e(e, "Detection failed")
            emptyList()
        }
    }

    private fun parseOutput(
        output: Array<FloatArray>,
        imageWidth: Float,
        imageHeight: Float
    ): List<YoloDetection> {
        val detections = mutableListOf<YoloDetection>()
        val scaleX = imageWidth / INPUT_SIZE
        val scaleY = imageHeight / INPUT_SIZE

        for (detection in output) {
            val confidence = detection[4]
            if (confidence < CONFIDENCE_THRESHOLD) continue

            val x = detection[0] * scaleX
            val y = detection[1] * scaleY
            val w = detection[2] * scaleX
            val h = detection[3] * scaleY

            val x1 = (x - w / 2).coerceIn(0f, imageWidth)
            val y1 = (y - h / 2).coerceIn(0f, imageHeight)
            val x2 = (x + w / 2).coerceIn(0f, imageWidth)
            val y2 = (y + h / 2).coerceIn(0f, imageHeight)

            var maxClassScore = 0f
            var maxClassIndex = 0
            for (i in 5..84) {
                if (detection[i] > maxClassScore) {
                    maxClassScore = detection[i]
                    maxClassIndex = i - 5
                }
            }

            if (maxClassIndex < COCO_CLASSES.size) {
                detections.add(
                    YoloDetection(
                        classIndex = maxClassIndex,
                        className = COCO_CLASSES[maxClassIndex],
                        confidence = confidence * maxClassScore,
                        x1 = x1,
                        y1 = y1,
                        x2 = x2,
                        y2 = y2
                    )
                )
            }
        }

        // Apply Non-Maximum Suppression
        return applyNMS(detections, NMS_THRESHOLD)
    }

    private fun applyNMS(detections: List<YoloDetection>, threshold: Float): List<YoloDetection> {
        val sorted = detections.sortedByDescending { it.confidence }
        val result = mutableListOf<YoloDetection>()

        for (detection in sorted) {
            var shouldAdd = true
            for (existingDetection in result) {
                val iou = calculateIoU(detection, existingDetection)
                if (iou > threshold) {
                    shouldAdd = false
                    break
                }
            }
            if (shouldAdd && result.size < MAX_RESULTS) {
                result.add(detection)
            }
        }

        return result
    }

    private fun calculateIoU(box1: YoloDetection, box2: YoloDetection): Float {
        val intersectionArea = maxOf(0f, minOf(box1.x2, box2.x2) - maxOf(box1.x1, box2.x1)) *
                maxOf(0f, minOf(box1.y2, box2.y2) - maxOf(box1.y1, box2.y1))
        val box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1)
        val box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1)
        val unionArea = box1Area + box2Area - intersectionArea
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }

    private fun getMockDetections(width: Int, height: Int): List<YoloDetection> {
        // Mock detections for testing without actual model
        return listOf(
            YoloDetection(
                classIndex = 0,
                className = "person",
                confidence = 0.95f,
                x1 = (width * 0.2f),
                y1 = (height * 0.2f),
                x2 = (width * 0.5f),
                y2 = (height * 0.7f)
            ),
            YoloDetection(
                classIndex = 2,
                className = "car",
                confidence = 0.87f,
                x1 = (width * 0.55f),
                y1 = (height * 0.3f),
                x2 = (width * 0.85f),
                y2 = (height * 0.65f)
            )
        )
    }

    fun shutdown() {
        interpreter?.close()
        Timber.d("YoloDetector shutdown")
    }
}
