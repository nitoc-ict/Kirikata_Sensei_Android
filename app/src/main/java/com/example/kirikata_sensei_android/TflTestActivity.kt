package com.example.kirikata_sensei_android

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import android.graphics.Color as GColor

class TflTestActivity : BaseActivity(), HandLandmarkerHelper.LandmarkerListener {

    private lateinit var handHelper: HandLandmarkerHelper
    private lateinit var tflite: Interpreter
    private val _handResult = mutableStateOf<HandLandmarkerHelper.ResultBundle?>(null)
    private val _detections = mutableStateOf<List<DetectionBox>>(emptyList())
    private val yoloBusy = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
            finish()
        }

        // MediaPipe 初期化
        handHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            handLandmarkerHelperListener = this
        )
        handHelper.setupHandLandmarker()

        // YOLO モデル読み込み
        val model = FileUtil.loadMappedFile(this, "model1_float32.tflite")
        tflite = Interpreter(model, Interpreter.Options())

        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraWithDetection(handHelper, tflite, _detections, _handResult, yoloBusy)
                Text(
                    text = "YOLO + MediaPipe (擬似包丁線 & 危険判定)",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handHelper.clearHandLandmarker()
        tflite.close()
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        runOnUiThread { _handResult.value = resultBundle }
    }

    override fun onError(error: String, errorCode: Int) {
        Log.e("MP", "Error: $error ($errorCode)")
    }
}

@Composable
fun CameraWithDetection(
    handHelper: HandLandmarkerHelper,
    tflite: Interpreter,
    detections: MutableState<List<DetectionBox>>,
    handResult: MutableState<HandLandmarkerHelper.ResultBundle?>,
    yoloBusy: AtomicBoolean
) {
    val scope = rememberCoroutineScope()
    var frameCount by remember { mutableIntStateOf(0) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                val executor = Executors.newSingleThreadExecutor()

                analysis.setAnalyzer(executor) { image ->
                    frameCount++
                    try {
                        val bmp = image.toBitmapRGBA8888()
                        if (bmp != null) {
                            // YOLOは数フレームに1回のみ実行（負荷軽減）
                            if (frameCount % 2 == 0 && yoloBusy.compareAndSet(false, true)) {
                                scope.launch(Dispatchers.Default) {
                                    try {
                                        val boxes = detectYolo(tflite, bmp)
                                        withContext(Dispatchers.Main) { detections.value = boxes }
                                    } finally { yoloBusy.set(false) }
                                }
                            }
                        }
                        image.planes.forEach { it.buffer.rewind() }
                        handHelper.detectLiveStream(image, false)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally { image.close() }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    ctx as ComponentActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )

    Box(Modifier.fillMaxSize()) {
        OverlayCanvas(handResult.value, detections.value)
    }
}

@Composable
fun OverlayCanvas(resultBundle: HandLandmarkerHelper.ResultBundle?, boxes: List<DetectionBox>) {
    val knifeHolderHandIndex = remember { mutableStateOf<Int?>(null) }
    val lastKnifeDetectedTime = remember { mutableStateOf(0L) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val hands = resultBundle?.results ?: return@Canvas
        val handCenters = mutableListOf<Offset>()

        // 🖐 手のランドマーク描画＋中心記録（空チェック付き）
        hands.forEach { hand ->
            val lmList = hand.landmarks()
            if (lmList.isEmpty()) return@forEach  // ←★★空ならスキップ

            val lms = lmList.first()
            if (lms.isEmpty()) return@forEach    // ←★★さらに安全

            val pts = lms.map { Offset(it.x() * size.width, it.y() * size.height) }
            pts.forEach { drawCircle(Color.Red, 5f, it) }

            val connections = listOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 4,
                0 to 5, 5 to 6, 6 to 7, 7 to 8,
                5 to 9, 9 to 10, 10 to 11, 11 to 12,
                9 to 13, 13 to 14, 14 to 15, 15 to 16,
                13 to 17, 17 to 18, 18 to 19, 19 to 20,
                0 to 17
            )
            for ((start, end) in connections) {
                val path = Path().apply {
                    moveTo(pts[start].x, pts[start].y)
                    lineTo(pts[end].x, pts[end].y)
                }
                drawPath(path, Color.Red, style = Stroke(width = 3f))
            }

            val cx = pts.map { it.x }.average().toFloat()
            val cy = pts.map { it.y }.average().toFloat()
            handCenters.add(Offset(cx, cy))
        }

        // 📦 YOLOと手の対応づけ
        var bestBox: DetectionBox? = null
        var bestIndex: Int? = null
        var minDist = Float.MAX_VALUE
        if (boxes.isNotEmpty() && handCenters.isNotEmpty()) {
            for (box in boxes.sortedByDescending { it.confidence }) {
                val bx = (box.left + box.right) / 2f * size.width
                val by = (box.top + box.bottom) / 2f * size.height
                val boxCenter = Offset(bx, by)
                handCenters.forEachIndexed { i, handCenter ->
                    val d = handCenter.minus(boxCenter).getDistance()
                    if (d < minDist) {
                        minDist = d
                        bestBox = box
                        bestIndex = i
                    }
                }
            }
        }

        if (bestBox != null && bestIndex != null) {
            knifeHolderHandIndex.value = bestIndex
            lastKnifeDetectedTime.value = System.currentTimeMillis()
        } else {
            if (System.currentTimeMillis() - lastKnifeDetectedTime.value > 1000)
                knifeHolderHandIndex.value = null
        }

        // ✅ 擬似包丁線の描画（空リスト対策込み）
        knifeHolderHandIndex.value?.let { idx ->
            if (idx >= hands.size) return@let
            val lmList = hands[idx].landmarks()
            if (lmList.isEmpty()) return@let
            val lms = lmList.first()
            if (lms.size < 9) return@let // 主要点取れないなら描かない

            val wrist = lms.toOffset(0, size.width, size.height)
            val thumbBase = lms.toOffset(2, size.width, size.height)
            val indexBase = lms.toOffset(5, size.width, size.height)
            val indexTip = lms.toOffset(8, size.width, size.height)

            val gripStart = (thumbBase + indexBase) * 0.5f
            val handDir = (indexTip - wrist).normalize()
            val knifeDir = rotate(handDir, -20f)
            val gripLength = (indexTip - gripStart).getDistance().coerceAtLeast(1f)
            val knifeLength = gripLength * 3f
            val gripEnd = gripStart + handDir * gripLength
            val bladeStart = gripStart + handDir * (gripLength * 0.2f)
            val knifeEnd = bladeStart + knifeDir * knifeLength

            drawLine(Color.Gray, gripStart, gripEnd, 8f)
            drawLine(Color.Red, bladeStart, knifeEnd, 6f)
            drawCircle(Color.Red, 6f, knifeEnd)

            // ⚠️危険判定
            val otherIdx = if (hands.size > 1) (1 - idx) else null
            if (otherIdx != null && otherIdx < hands.size) {
                val otherLmList = hands[otherIdx].landmarks()
                if (otherLmList.isNotEmpty()) {
                    val otherLms = otherLmList.first()
                    val cat = isCatHand(otherLms)
                    if (!cat) {
                        val tipIds = listOf(8, 12, 16, 20)
                        val danger = tipIds.any { id ->
                            if (id >= otherLms.size) return@any false
                            val tip = otherLms.toOffset(id, size.width, size.height)
                            distancePointToSegment(tip, bladeStart, knifeEnd) < 40f
                        }
                        if (danger) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "⚠️ 危険！",
                                100f, 100f,
                                Paint().apply { color = GColor.RED; textSize = 64f }
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==================== データ/補助関数群 ====================

data class DetectionBox(
    val left: Float, val top: Float,
    val right: Float, val bottom: Float,
    val label: String, val confidence: Float
)

/**
 * YOLO (640) forward。出力形式は [1, 5, 8400] を想定（cx, cy, w, h, conf）。
 */
fun detectYolo(tflite: Interpreter, bitmap: Bitmap): List<DetectionBox> {
    val inputSize = 640
    val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
    val input = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        .order(ByteOrder.nativeOrder())

    val pixels = IntArray(inputSize * inputSize)
    resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
    for (v in pixels) {
        input.putFloat(((v shr 16) and 0xFF) / 255f) // R
        input.putFloat(((v shr 8) and 0xFF) / 255f)  // G
        input.putFloat((v and 0xFF) / 255f)          // B
    }
    input.rewind()

    val confThresh = 0.25f
    val out = Array(1) { Array(5) { FloatArray(8400) } }
    val outputs: MutableMap<Int, Any> = HashMap()
    outputs[0] = out

    return try {
        tflite.runForMultipleInputsOutputs(arrayOf(input), outputs)

        val boxes = mutableListOf<DetectionBox>()
        for (i in 0 until 8400) {
            val cx = out[0][0][i]
            val cy = out[0][1][i]
            val w = out[0][2][i]
            val h = out[0][3][i]
            val conf = out[0][4][i]
            if (conf > confThresh) {
                val left = (cx - w / 2).coerceIn(0f, 1f)
                val top = (cy - h / 2).coerceIn(0f, 1f)
                val right = (cx + w / 2).coerceIn(0f, 1f)
                val bottom = (cy + h / 2).coerceIn(0f, 1f)
                boxes.add(DetectionBox(left, top, right, bottom, "Knife", conf))
            }
        }
        nms(boxes, 0.45f)
    } catch (e: Exception) {
        Log.e("TFLITE", "❌ YOLO run failed: ${e.message}")
        emptyList()
    }
}

fun nms(boxes: List<DetectionBox>, iouThresh: Float): List<DetectionBox> {
    val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
    val keep = mutableListOf<DetectionBox>()
    while (sorted.isNotEmpty()) {
        val best = sorted.removeAt(0)
        keep.add(best)
        val remain = sorted.filter { iou(best, it) < iouThresh }
        sorted.clear()
        sorted.addAll(remain)
    }
    return keep
}

fun iou(a: DetectionBox, b: DetectionBox): Float {
    val x1 = max(a.left, b.left)
    val y1 = max(a.top, b.top)
    val x2 = min(a.right, b.right)
    val y2 = min(a.bottom, b.bottom)
    val inter = max(0f, x2 - x1) * max(0f, y2 - y1)
    val areaA = (a.right - a.left) * (a.bottom - a.top)
    val areaB = (b.right - b.left) * (b.bottom - b.top)
    return inter / (areaA + areaB - inter + 1e-6f)
}

/**
 * ImageProxy → RGBA8888 Bitmap（OUTPUT_IMAGE_FORMAT_RGBA_8888 前提）
 */
private fun ImageProxy.toBitmapRGBA8888(): Bitmap? = try {
    val plane = planes[0]
    val buffer = plane.buffer
    val width = width
    val height = height
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val out = ByteArray(width * height * 4)
    val row = ByteArray(rowStride)
    var offset = 0
    for (y in 0 until height) {
        if (buffer.remaining() < rowStride) break
        buffer.get(row, 0, rowStride)
        System.arraycopy(row, 0, out, offset, min(rowStride, width * pixelStride))
        offset += width * 4
    }
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bmp.copyPixelsFromBuffer(ByteBuffer.wrap(out))
    bmp
} catch (e: Exception) { null }

/**
 * MediaPipe NormalizedLandmark → 画面座標 Offset 変換のユーティリティ
 */
private fun List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>.toOffset(
    idx: Int,
    w: Float,
    h: Float
): Offset = Offset(this[idx].x() * w, this[idx].y() * h)

/**
 * ベクトル回転（deg度）
 */
fun rotate(v: Offset, deg: Float): Offset {
    val rad = Math.toRadians(deg.toDouble())
    val c = cos(rad).toFloat()
    val s = kotlin.math.sin(rad).toFloat()
    return Offset(v.x * c - v.y * s, v.x * s + v.y * c)
}

/**
 * 正規化（長さが0の場合はそのまま）
 */
fun Offset.normalize(): Offset {
    val len = sqrt(x * x + y * y)
    return if (len == 0f) this else Offset(x / len, y / len)
}

/**
 * 点と線分の最短距離
 */
fun distancePointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val ap = p - a
    val ab2 = ab.x.pow(2) + ab.y.pow(2)
    if (ab2 == 0f) return (p - a).getDistance()
    var t = ((ap.x * ab.x) + (ap.y * ab.y)) / ab2
    t = t.coerceIn(0f, 1f)
    val proj = Offset(a.x + ab.x * t, a.y + ab.y * t)
    return (p - proj).getDistance()
}

/**
 * 猫の手（指を丸めている）判定：
 * 指先(Tip) と 第二関節(PIP) の距離が短い指が 3 本以上で true
 * Tip: 8,12,16,20 / PIP: 6,10,14,18
 */
fun isCatHand(lms: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Boolean {
    val pairs = listOf(8 to 6, 12 to 10, 16 to 14, 20 to 18)
    var folded = 0
    for ((tip, pip) in pairs) {
        val dx = lms[tip].x() - lms[pip].x()
        val dy = lms[tip].y() - lms[pip].y()
        val dist = sqrt(dx * dx + dy * dy)
        if (dist < 0.05f) folded++
    }
    return folded >= 3
}
