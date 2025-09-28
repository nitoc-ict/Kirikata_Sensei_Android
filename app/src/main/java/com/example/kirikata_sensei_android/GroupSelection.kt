package com.example.kirikata_sensei_android

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class GroupSelection : ComponentActivity(), HandLandmarkerHelper.LandmarkerListener {
    lateinit var handLandmarkerHelper: HandLandmarkerHelper

    private val _result = mutableStateOf<HandLandmarkerHelper.ResultBundle?>(null)
    val result: State<HandLandmarkerHelper.ResultBundle?> = _result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            handLandmarkerHelperListener = this
        )

        setContent {
            MaterialTheme {
                HandTrackingScreen(
                    result = result,
                    handHelper = handLandmarkerHelper
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handLandmarkerHelper.clearHandLandmarker()
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        _result.value = resultBundle
    }

    override fun onError(error: String, errorCode: Int) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandTrackingScreen(
    result: State<HandLandmarkerHelper.ResultBundle?>,
    handHelper: HandLandmarkerHelper
) {
    val context = LocalContext.current
    var showLandmarks by remember { mutableStateOf(true) }

    var countdown by remember { mutableStateOf(0) }
    var targetNumber by remember { mutableStateOf<Int?>(null) }
    var detectedNumber by remember { mutableStateOf<Int?>(null) }
    var stableFrames by remember { mutableStateOf(0) }
    val REQUIRED_STABLE_FRAMES = 7

    val scope = rememberCoroutineScope()

    BottomSheetScaffold(
        sheetPeekHeight = 80.dp,
        sheetContent = {
            HandTrackingControls(handHelper, result.value?.inferenceTime)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CameraPreview()

            if (showLandmarks) {
                result.value?.let { LandmarkOverlay(it) }
            }

            val totalFingers = result.value?.results
                ?.firstOrNull()
                ?.landmarks()
                ?.sumOf { hand -> countExtendedFingersImproved(hand) } ?: 0

            if (detectedNumber == null) {
                if (totalFingers > 0) {
                    if (targetNumber == totalFingers) {
                        stableFrames++
                        if (stableFrames >= REQUIRED_STABLE_FRAMES && countdown == 0) {
                            countdown = 3
                            scope.launch {
                                while (countdown > 0 && targetNumber == totalFingers && detectedNumber == null) {
                                    delay(1000)
                                    countdown--
                                }
                                if (countdown == 0 && targetNumber == totalFingers && detectedNumber == null) {
                                    detectedNumber = totalFingers
                                    targetNumber = null

                                    // ✅ グループ番号確定 → 数秒後にCookingActivityへ遷移
                                    scope.launch {
                                        delay(2000) // 2秒待つ
                                        val intent = Intent(context, CookingActivity::class.java).apply {
                                            putExtra("recipeName", "sandwich")
                                            // HandLandmarkerHelper の設定値を渡す
                                            putExtra("delegate", handHelper.currentDelegate)
                                            putExtra("maxHands", handHelper.maxNumHands)
                                            putExtra("detectionThreshold", handHelper.minHandDetectionConfidence)
                                            putExtra("trackingThreshold", handHelper.minHandTrackingConfidence)
                                            putExtra("presenceThreshold", handHelper.minHandPresenceConfidence)
                                        }
                                        context.startActivity(intent)
                                        (context as? ComponentActivity)?.finish()
                                    }
                                }
                            }
                        }
                    } else {
                        targetNumber = totalFingers
                        stableFrames = 1
                        countdown = 0
                    }
                } else {
                    targetNumber = null
                    stableFrames = 0
                    countdown = 0
                }
            } else {
                if (totalFingers == 0) {
                    detectedNumber = null
                    targetNumber = null
                    countdown = 0
                    stableFrames = 0
                }
            }

            // ✅ オーバーレイ表示
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    countdown > 0 -> Text(
                        text = countdown.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Red
                    )
                    detectedNumber != null -> Text(
                        text = "グループ: $detectedNumber",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Green
                    )
                    else -> Text(
                        text = "何グループですか？ 指で教えてください",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = { showLandmarks = !showLandmarks },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(if (showLandmarks) "ランドマーク非表示" else "ランドマーク表示")
            }
        }
    }
}

fun countExtendedFingersImproved(landmarks: List<NormalizedLandmark>): Int {
    if (landmarks.size < 21) return 0
    var count = 0

    // 手の向き判定（左手か右手か）
    val isLeftHand = landmarks[17].x() < landmarks[5].x()

    // 親指
    if (isLeftHand) {
        if (landmarks[4].x() > landmarks[3].x() && landmarks[4].x() > landmarks[2].x()) count++
    } else {
        if (landmarks[4].x() < landmarks[3].x() && landmarks[4].x() < landmarks[2].x()) count++
    }

    // 人差し指
    if (landmarks[8].y() < landmarks[6].y() && landmarks[8].y() < landmarks[5].y()) count++

    // 中指
    if (landmarks[12].y() < landmarks[10].y() && landmarks[12].y() < landmarks[9].y()) count++

    // 薬指
    if (landmarks[16].y() < landmarks[14].y() && landmarks[16].y() < landmarks[13].y()) count++

    // 小指
    if (landmarks[20].y() < landmarks[18].y() && landmarks[20].y() < landmarks[17].y()) count++

    return count
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current

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
                    (ctx as? GroupSelection)?.handLandmarkerHelper?.detectLiveStream(
                        imageProxy,
                        isFrontCamera = false
                    )
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

@Composable
fun LandmarkOverlay(resultBundle: HandLandmarkerHelper.ResultBundle) {
    val result = resultBundle.results.firstOrNull() ?: return

    val connections = listOf(
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
        Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
        Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
        Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
        Pair(5, 9), Pair(9, 13), Pair(13, 17)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        result.landmarks().forEach { hand ->
            val points = hand.map { lm ->
                Offset(
                    x = lm.x() * size.width,
                    y = lm.y() * size.height
                )
            }

            connections.forEach { (s, e) ->
                if (s < points.size && e < points.size) {
                    drawLine(Color.Green, points[s], points[e], strokeWidth = 4f)
                }
            }

            points.forEach { pt ->
                drawCircle(Color.Red, radius = 6f, center = pt)
            }
        }
    }
}

@Composable
fun HandTrackingControls(
    handHelper: HandLandmarkerHelper,
    inferenceTime: Long?
) {
    var detectionThreshold by remember { mutableStateOf(handHelper.minHandDetectionConfidence) }
    var trackingThreshold by remember { mutableStateOf(handHelper.minHandTrackingConfidence) }
    var presenceThreshold by remember { mutableStateOf(handHelper.minHandPresenceConfidence) }
    var maxHands by remember { mutableStateOf(handHelper.maxNumHands) }
    var delegate by remember { mutableStateOf(handHelper.currentDelegate) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("推論時間: ${inferenceTime ?: "-"} ms")

        Spacer(Modifier.height(12.dp))

        ThresholdControl(
            label = "Detection",
            value = detectionThreshold,
            onMinus = {
                if (detectionThreshold >= 0.2f) {
                    detectionThreshold -= 0.1f
                    handHelper.minHandDetectionConfidence = detectionThreshold
                    resetHelper(handHelper)
                }
            },
            onPlus = {
                if (detectionThreshold <= 0.8f) {
                    detectionThreshold += 0.1f
                    handHelper.minHandDetectionConfidence = detectionThreshold
                    resetHelper(handHelper)
                }
            }
        )

        ThresholdControl(
            label = "Tracking",
            value = trackingThreshold,
            onMinus = {
                if (trackingThreshold >= 0.2f) {
                    trackingThreshold -= 0.1f
                    handHelper.minHandTrackingConfidence = trackingThreshold
                    resetHelper(handHelper)
                }
            },
            onPlus = {
                if (trackingThreshold <= 0.8f) {
                    trackingThreshold += 0.1f
                    handHelper.minHandTrackingConfidence = trackingThreshold
                    resetHelper(handHelper)
                }
            }
        )

        ThresholdControl(
            label = "Presence",
            value = presenceThreshold,
            onMinus = {
                if (presenceThreshold >= 0.2f) {
                    presenceThreshold -= 0.1f
                    handHelper.minHandPresenceConfidence = presenceThreshold
                    resetHelper(handHelper)
                }
            },
            onPlus = {
                if (presenceThreshold <= 0.8f) {
                    presenceThreshold += 0.1f
                    handHelper.minHandPresenceConfidence = presenceThreshold
                    resetHelper(handHelper)
                }
            }
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Max Hands: $maxHands")
            Row {
                Button(onClick = {
                    if (maxHands > 1) {
                        maxHands--
                        handHelper.maxNumHands = maxHands
                        resetHelper(handHelper)
                    }
                }) { Text("-") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (maxHands < 2) {
                        maxHands++
                        handHelper.maxNumHands = maxHands
                        resetHelper(handHelper)
                    }
                }) { Text("+") }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Delegate: ${if (delegate == HandLandmarkerHelper.DELEGATE_CPU) "CPU" else "GPU"}")
            Button(onClick = {
                delegate = if (delegate == HandLandmarkerHelper.DELEGATE_CPU)
                    HandLandmarkerHelper.DELEGATE_GPU else HandLandmarkerHelper.DELEGATE_CPU
                handHelper.currentDelegate = delegate
                resetHelper(handHelper)
            }) {
                Text("切替")
            }
        }
    }
}

@Composable
fun ThresholdControl(label: String, value: Float, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$label: %.2f".format(value))
        Row {
            Button(onClick = onMinus) { Text("-") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onPlus) { Text("+") }
        }
    }
}

private fun resetHelper(helper: HandLandmarkerHelper) {
    helper.clearHandLandmarker()
    helper.setupHandLandmarker()
}
