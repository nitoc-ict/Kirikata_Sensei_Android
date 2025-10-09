package com.example.kirikata_sensei_android

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.kirikata_sensei_android.model.CookingStep
import com.example.kirikata_sensei_android.model.Recipes
import com.example.kirikata_sensei_android.network.GlobalSocket
import com.example.kirikata_sensei_android.ui.CookingScreen
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import com.google.mediapipe.tasks.vision.core.RunningMode

class CookingActivity : ComponentActivity(), HandLandmarkerHelper.LandmarkerListener {

    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val _result = mutableStateOf<HandLandmarkerHelper.ResultBundle?>(null)
    val result: State<HandLandmarkerHelper.ResultBundle?> = _result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 横画面固定
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // フルスクリーン設定
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // ===== Intent受け取り =====
        val token = intent.getStringExtra("token")
        val username = intent.getStringExtra("username")
        val roomName = intent.getStringExtra("roomName")
        val recipeName = intent.getStringExtra("recipeName") ?: "sandwich"
        val seatIndex = intent.getIntExtra("seatIndex", -1)

        val delegate = intent.getIntExtra("delegate", HandLandmarkerHelper.DELEGATE_CPU)
        val maxHands = intent.getIntExtra("maxHands", 2)
        val detectionThreshold = intent.getFloatExtra("detectionThreshold", 0.5f)
        val trackingThreshold = intent.getFloatExtra("trackingThreshold", 0.5f)
        val presenceThreshold = intent.getFloatExtra("presenceThreshold", 0.5f)

        Log.d(
            "CookingActivity",
            """
            === CookingActivity 受信 ===
            token=$token
            username=$username
            room=$roomName
            recipeName=$recipeName
            seatIndex=$seatIndex
            ============================
            """.trimIndent()
        )

        // ===== HandLandmarker 初期化 =====
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

        // ===== ローカルレシピ読み込み =====
        val steps: List<CookingStep> = when (recipeName.lowercase()) {
            "sandwich" -> Recipes.sandwich
            else -> {
                Log.w("CookingActivity", "⚠ 未知のレシピ名: $recipeName → sandwichを使用")
                Recipes.sandwich
            }
        }

        // ===== 先生からセッション終了イベントを監視 =====
        GlobalSocket.manager?.onSessionEnded {
            runOnUiThread {
                Log.d("CookingActivity", "📩 先生からセッション終了通知を受信")
                showSessionEndedAndReturnToLogin()
            }
        }

        // ===== Compose画面描画 =====
        setContent {
            CookingScreen(
                steps = steps,
                result = result,
                handHelper = handLandmarkerHelper,
                onProgressChange = { currentStep ->
                    // ✅ サーバーへ studentProgress イベント送信
                    GlobalSocket.manager?.emitProgress(
                        room = roomName ?: "",
                        userId = GlobalSocket.manager?.socket?.id() ?: "",
                        username = username ?: "unknown",
                        seatIndex = seatIndex,
                        currentStep = currentStep,
                        recipeId = recipeName
                    )
                }
            )
        }
    }

    /**
     * 🔚 先生からセッション終了を受信したときの処理
     * fin.png → 3秒表示 → Socket切断 → LoginActivityへ戻る
     */
    private fun showSessionEndedAndReturnToLogin() {
        // fin.png 表示用のActivityを起動
        val intent = Intent(this, SessionEndedActivity::class.java)
        startActivity(intent)

        // 通信を安全に切断
        GlobalSocket.manager?.disconnect()

        // 3秒後にログイン画面へ戻る
        Handler(Looper.getMainLooper()).postDelayed({
            val loginIntent = Intent(this, LoginActivity::class.java)
            loginIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(loginIntent)
            finish()
        }, 3000)
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
