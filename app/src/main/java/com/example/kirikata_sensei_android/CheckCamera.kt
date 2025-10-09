package com.example.kirikata_sensei_android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.Executors

// ==========================
// 座標保存ユーティリティ
// ==========================
object CalibrationStore {
    private const val PREF_NAME = "calibration_prefs"
    private const val TAG = "CalibrationStore"

    fun saveCoords(context: Context, key: String, x: Float, y: Float) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("${key}_x", x)
            .putFloat("${key}_y", y)
            .apply()
        Log.d(TAG, "Saved [$key] -> x=$x, y=$y")
    }

    fun loadCoords(context: Context, key: String): Pair<Float, Float>? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains("${key}_x")) return null
        val x = prefs.getFloat("${key}_x", 0f)
        val y = prefs.getFloat("${key}_y", 0f)
        return x to y
    }
}

// ==========================
// カメラ + ハンドトラッキング
// ==========================
class CheckCamera : BaseActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val _result = mutableStateOf<HandLandmarkerHelper.ResultBundle?>(null)
    val result: State<HandLandmarkerHelper.ResultBundle?> = _result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // パーミッション確認
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
            finish()
        }

        // HandLandmarker 初期化
        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            handLandmarkerHelperListener = this
        )
        handLandmarkerHelper.setupHandLandmarker()

        // ==========================
        // Compose UI
        // ==========================
        setContent {
            var fingerTip by remember { mutableStateOf<Pair<Float, Float>?>(null) }

            Box(modifier = Modifier.fillMaxSize()) {
                // カメラプレビュー
                CameraPreview(handHelper = handLandmarkerHelper)

                // 🟩 CookingScreenと同じ位置のボタンを半透明で表示
                CalibrationOverlay()

                // ハンドトラッキングのオーバーレイ
                result.value?.let { LandmarkOverlayCheck(it) { coords ->
                    fingerTip = coords
                } }

                // 座標表示・保存ボタンUI
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    fingerTip?.let { (x, y) ->
                        Text("Index Tip: x=${"%.2f".format(x)}, y=${"%.2f".format(y)}")

                        Spacer(Modifier.height(8.dp))

                        Row {
                            Button(onClick = {
                                CalibrationStore.saveCoords(this@CheckCamera, "BACK_BUTTON", x, y)
                                Toast.makeText(
                                    this@CheckCamera,
                                    "戻るボタン座標を保存しました",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text("戻るを保存")
                            }

                            Spacer(Modifier.width(8.dp))

                            Button(onClick = {
                                CalibrationStore.saveCoords(this@CheckCamera, "NEXT_BUTTON", x, y)
                                Toast.makeText(
                                    this@CheckCamera,
                                    "進むボタン座標を保存しました",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Text("進むを保存")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handLandmarkerHelper.clearHandLandmarker()
    }

    // MediaPipe コールバック
    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        _result.value = resultBundle
    }

    override fun onError(error: String, errorCode: Int) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}

// ==========================
// CameraX Preview
// ==========================
@Composable
fun CameraPreview(handHelper: HandLandmarkerHelper) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    handHelper.detectLiveStream(imageProxy, isFrontCamera = false)
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        ctx as ComponentActivity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

// ==========================
// Calibration Overlay（CookingScreenのボタン位置）
// ==========================
@Composable
fun BoxScope.CalibrationOverlay() {
    Column(
        modifier = Modifier
            .align(Alignment.CenterEnd) // 右端に固定
            .padding(end = 16.dp)
            .width(200.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 「すすむ」ボタン（上）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0x8834C759), RoundedCornerShape(16.dp)), // 半透明グリーン
            contentAlignment = Alignment.Center
        ) {
            Text("すすむ", color = Color.White)
        }

        // 「もどる」ボタン（下）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color(0x88E53935), RoundedCornerShape(16.dp)), // 半透明レッド
            contentAlignment = Alignment.Center
        ) {
            Text("もどる", color = Color.White)
        }
    }
}

// ==========================
// Landmark Overlay
// ==========================
@Composable
fun LandmarkOverlayCheck(
    resultBundle: HandLandmarkerHelper.ResultBundle,
    onTipDetected: (Pair<Float, Float>) -> Unit
) {
    val results = resultBundle.results
    if (results.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        results.forEach { hand ->
            hand.landmarks().forEach { landmarkList ->
                val points = landmarkList.map { lm ->
                    Offset(
                        x = lm.x() * size.width,
                        y = lm.y() * size.height
                    )
                }

                // 人差し指先端 (ランドマーク8)
                val tip = landmarkList[8]
                onTipDetected(tip.x() to tip.y()) // 正規化座標をコールバック

                // 骨格を描画
                val connections = listOf(
                    0 to 1, 1 to 2, 2 to 3, 3 to 4,
                    0 to 5, 5 to 6, 6 to 7, 7 to 8,
                    0 to 9, 9 to 10, 10 to 11, 11 to 12,
                    0 to 13, 13 to 14, 14 to 15, 15 to 16,
                    0 to 17, 17 to 18, 18 to 19, 19 to 20,
                    5 to 9, 9 to 13, 13 to 17
                )
                connections.forEach { (s, e) ->
                    if (s < points.size && e < points.size) {
                        drawLine(Color.Green, points[s], points[e], strokeWidth = 4f)
                    }
                }

                // ランドマーク点
                points.forEach { pt ->
                    drawCircle(Color.Red, radius = 6f, center = pt)
                }
            }
        }
    }
}
