package com.example.kirikata_sensei_android

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.kirikata_sensei_android.model.CookingStep
import com.example.kirikata_sensei_android.model.Recipes
import com.example.kirikata_sensei_android.ui.CookingScreen
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode

class CookingActivity : ComponentActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val _result = mutableStateOf<HandLandmarkerHelper.ResultBundle?>(null)
    val result: State<HandLandmarkerHelper.ResultBundle?> = _result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Intent から設定値を取得
        val delegate = intent.getIntExtra("delegate", HandLandmarkerHelper.DELEGATE_CPU)
        val maxHands = intent.getIntExtra("maxHands", 2)
        val detectionThreshold = intent.getFloatExtra("detectionThreshold", 0.5f)
        val trackingThreshold = intent.getFloatExtra("trackingThreshold", 0.5f)
        val presenceThreshold = intent.getFloatExtra("presenceThreshold", 0.5f)

        Log.d("CookingActivity", "delegate=$delegate, maxHands=$maxHands")

        // HandLandmarker のセットアップ
        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            handLandmarkerHelperListener = this
        ).apply {
            currentDelegate = delegate
            maxNumHands = maxHands
            minHandDetectionConfidence = detectionThreshold
            minHandTrackingConfidence = trackingThreshold
            minHandPresenceConfidence = presenceThreshold
        }

        handLandmarkerHelper.setupHandLandmarker()

        val steps: List<CookingStep> = Recipes.sandwich

        setContent {
            CookingScreen(
                steps = steps,
                result = result,
                handHelper = handLandmarkerHelper
            )
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
