package com.example.kirikata_sensei_android

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.Executors

// ==========================
// GroupSelection用キャリブレーション画面
// ==========================
class CheckCamera2 : BaseActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val _result = mutableStateOf<HandLandmarkerHelper.ResultBundle?>(null)
    val result: State<HandLandmarkerHelper.ResultBundle?> = _result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_SHORT).show()
            finish()
        }

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            handLandmarkerHelperListener = this
        )
        handLandmarkerHelper.setupHandLandmarker()

        setContent {
            var fingerTip by remember { mutableStateOf<Pair<Float, Float>?>(null) }

            Box(modifier = Modifier.fillMaxSize()) {
                // カメラ映像
                CameraPreview2(handHelper = handLandmarkerHelper)

                // GroupSelectionの確認ダイアログを再現
                ConfirmButtonOverlay()

                // MediaPipeハンドランドマークから指先座標を取得
                result.value?.let { LandmarkOverlayCheck(it) { coords ->
                    fingerTip = coords
                } }

                // 座標確認・保存UI
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    fingerTip?.let { (x, y) ->
                        Text("Index Tip: x=${"%.3f".format(x)}, y=${"%.3f".format(y)}")
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Button(onClick = {
                                CalibrationStore.saveCoords(this@CheckCamera2, "OK_BUTTON", x, y)
                                Toast.makeText(this@CheckCamera2, "✅ 決定ボタン座標を保存しました", Toast.LENGTH_SHORT).show()
                            }) { Text("決定を保存") }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = {
                                CalibrationStore.saveCoords(this@CheckCamera2, "CANCEL_BUTTON", x, y)
                                Toast.makeText(this@CheckCamera2, "✅ もどるボタン座標を保存しました", Toast.LENGTH_SHORT).show()
                            }) { Text("もどるを保存") }
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

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        _result.value = resultBundle
    }

    override fun onError(error: String, errorCode: Int) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}

// ==========================
// CameraX プレビュー
// ==========================
@Composable
fun CameraPreview2(handHelper: HandLandmarkerHelper) {
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
// GroupSelectionの「決定 / もどる」配置を再現
// ==========================
@Composable
fun ConfirmButtonOverlay() {
    // GroupSelection の確認ダイアログを画面中央に再現
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight(0.7f)
                .background(Color(0x88FFFFFF), RoundedCornerShape(24.dp))
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                Text(
                    "（確認ダイアログの再現）",
                    color = Color.Black,
                )

                // 「決定」と「もどる」ボタンの位置・サイズを同じに
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(90.dp)
                            .background(Color(0x8834C759), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("決定", color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(90.dp)
                            .background(Color(0x88E53935), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("もどる", color = Color.White)
                    }
                }
            }
        }
    }
}
