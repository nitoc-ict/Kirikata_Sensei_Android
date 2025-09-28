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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kirikata_sensei_android.model.CookingStep
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import java.util.concurrent.Executors

// ==========================
// メインUI
// ==========================
@Composable
fun CookingScreen(
    steps: List<CookingStep>,
    result: State<HandLandmarkerHelper.ResultBundle?>,
    handHelper: HandLandmarkerHelper
) {
    var currentStep by remember { mutableStateOf(0) }
    val step = steps[currentStep]

    Row(modifier = Modifier.fillMaxSize()) {
        // 左側：調理手順 or 包丁フェーズ
        Box(modifier = Modifier.weight(1f)) {
            if (step.requiresKnife) {
                CameraPreview(handHelper)
                result.value?.let { LandmarkOverlay(it) }
            } else {
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
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 戻る（赤）
            Button(
                onClick = { if (currentStep > 0) currentStep-- },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) { Text("戻る", color = Color.White) }

            // 進む（青）
            Button(
                onClick = { if (currentStep < steps.size - 1) currentStep++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
            ) { Text("進む", color = Color.White) }
        }
    }
}

// ==========================
// CameraX + MediaPipe
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
// LandmarkOverlay (可視化)
// ==========================
@Composable
fun LandmarkOverlay(resultBundle: HandLandmarkerHelper.ResultBundle) {
    val results = resultBundle.results
    android.util.Log.d("LandmarkOverlay", "描画対象の手の数: ${results.size}")
    if (results.isEmpty()) return

    val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,   // 親指
        0 to 5, 5 to 6, 6 to 7, 7 to 8,   // 人差し指
        0 to 9, 9 to 10, 10 to 11, 11 to 12, // 中指
        0 to 13, 13 to 14, 14 to 15, 15 to 16, // 薬指
        0 to 17, 17 to 18, 18 to 19, 19 to 20, // 小指
        5 to 9, 9 to 13, 13 to 17          // 掌
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        results.forEach { hand ->
            hand.landmarks().forEach { landmarkList ->
                val points = landmarkList.map { lm ->
                    Offset(
                        x = lm.x() * size.width,
                        y = lm.y() * size.height
                    )
                }

                // 線を描画
                connections.forEach { (s, e) ->
                    if (s < points.size && e < points.size) {
                        drawLine(Color.Green, points[s], points[e], strokeWidth = 4f)
                    }
                }

                // 点を描画
                points.forEach { pt ->
                    drawCircle(Color.Red, radius = 6f, center = pt)
                }
            }
        }
    }
}
