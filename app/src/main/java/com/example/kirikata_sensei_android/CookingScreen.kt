package com.example.kirikata_sensei_android.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kirikata_sensei_android.CalibrationStore
import com.example.kirikata_sensei_android.R
import com.example.kirikata_sensei_android.model.CookingStep
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import java.util.concurrent.Executors
import kotlin.math.hypot

// ==========================
// メインUI
// ==========================
@Composable
fun CookingScreen(
    steps: List<CookingStep>,
    result: State<HandLandmarkerHelper.ResultBundle?>,
    handHelper: HandLandmarkerHelper,
    onProgressChange: (Int) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    // ⚙️ 安全インデックス化（範囲外防止）
    val safeIndex = currentStep.coerceIn(0, steps.size - 1)
    val step = steps[safeIndex]

    val context = LocalContext.current
    var showFinishImage by remember { mutableStateOf(false) }

    // 保存済み座標
    val backButtonTarget = remember { CalibrationStore.loadCoords(context, "BACK_BUTTON") }
    val nextButtonTarget = remember { CalibrationStore.loadCoords(context, "NEXT_BUTTON") }

    // 空中押下の管理
    var activeTarget by remember { mutableStateOf<String?>(null) }
    var holdFrames by remember { mutableStateOf(0) }
    val requiredFrames = 7
    var fingerTip by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // ハンドトラッキング処理
    LaunchedEffect(result.value) {
        result.value?.let { bundle ->
            val hand = bundle.results.firstOrNull()?.landmarks()?.firstOrNull()
            if (hand != null && hand.size > 8) {
                fingerTip = hand[8].x() to hand[8].y()
            }
        }
    }

    // 判定処理（空中押下対応）
    LaunchedEffect(fingerTip) {
        fingerTip?.let { (fx, fy) ->
            val threshold = 0.10f
            fun isNear(target: Pair<Float, Float>?) =
                target?.let { (tx, ty) -> hypot(fx - tx, fy - ty) < threshold } ?: false

            val nearBack = isNear(backButtonTarget)
            val nearNext = isNear(nextButtonTarget)

            when {
                nearBack -> {
                    if (activeTarget == "BACK") {
                        holdFrames++
                        if (holdFrames >= requiredFrames && currentStep > 0) {
                            currentStep--
                            onProgressChange(currentStep)
                            activeTarget = null
                            holdFrames = 0
                        }
                    } else {
                        activeTarget = "BACK"; holdFrames = 0
                    }
                }
                nearNext -> {
                    if (activeTarget == "NEXT") {
                        holdFrames++
                        if (holdFrames >= requiredFrames) {
                            val isLastStep = currentStep >= steps.size - 1
                            if (isLastStep) {
                                // 🔹 サーバーには finalStep(=currentStep+1) を送信
                                onProgressChange(currentStep + 1)
                                showFinishImage = true
                            } else {
                                currentStep++
                                onProgressChange(currentStep)
                            }
                            activeTarget = null
                            holdFrames = 0
                        }
                    } else {
                        activeTarget = "NEXT"; holdFrames = 0
                    }
                }
                else -> {
                    activeTarget = null; holdFrames = 0
                }
            }
        }
    }

    // ===== 画面全体レイアウト =====
    Box(modifier = Modifier.fillMaxSize()) {
        if (showFinishImage) {
            // ✅ 終了画面表示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fin_taiki),
                    contentDescription = "終了画面",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                // 左側：調理手順 or 包丁フェーズ
                Box(modifier = Modifier.weight(1f)) {
                    if (step.requiresKnife) {
                        CameraPreview(handHelper)
                        result.value?.let { LandmarkOverlay(it) }
                    } else {
                        CameraBackground(handHelper)
                        Image(
                            painter = painterResource(step.imageResId!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 右側：操作UI
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isLastStep = currentStep >= steps.size - 1

                    // 🟢「進む」ボタン（上）
                    Button(
                        onClick = {
                            if (isLastStep) {
                                onProgressChange(currentStep + 1) // final送信
                                showFinishImage = true
                            } else {
                                currentStep++
                                onProgressChange(currentStep)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp), // ←角を丸く
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTarget == "NEXT") Color(0xFF00C853) else Color(0xFF4CAF50)
                        )
                    ) {
                        Text(if (isLastStep) "終了" else "すすむ", color = Color.White, fontSize = 20.sp)
                    }

                    //「戻る」ボタン（下）
                    Button(
                        onClick = {
                            if (currentStep > 0) {
                                currentStep--
                                onProgressChange(currentStep)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(16.dp), // ←角を丸く
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTarget == "BACK") Color(0xFFFF5252) else Color(0xFFE53935)
                        )
                    ) {
                        Text("もどる", color = Color.White,fontSize = 20.sp)
                    }
                }

            }
        }
    }
}

// ==========================
// CameraX（表示あり）
// ==========================
@Composable
fun CameraPreview(handHelper: HandLandmarkerHelper) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx: Context ->
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
}

// ==========================
// CameraX（裏処理だけ）
// ==========================
@Composable
fun CameraBackground(handHelper: HandLandmarkerHelper) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { ctx: Context ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(null) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    handHelper.detectLiveStream(imageProxy, isFrontCamera = false)
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
}

// ==========================
// LandmarkOverlay
// ==========================
@Composable
fun LandmarkOverlay(resultBundle: HandLandmarkerHelper.ResultBundle) {
    val results = resultBundle.results
    if (results.isEmpty()) return
    val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        0 to 9, 9 to 10, 10 to 11, 11 to 12,
        0 to 13, 13 to 14, 14 to 15, 15 to 16,
        0 to 17, 17 to 18, 18 to 19, 19 to 20,
        5 to 9, 9 to 13, 13 to 17
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        results.forEach { hand ->
            hand.landmarks().forEach { list ->
                val points = list.map { Offset(it.x() * size.width, it.y() * size.height) }
                connections.forEach { (s, e) ->
                    if (s < points.size && e < points.size)
                        drawLine(Color.Green, points[s], points[e], 4f)
                }
                points.forEach { pt -> drawCircle(Color.Red, 6f, pt) }
            }
        }
    }
}
