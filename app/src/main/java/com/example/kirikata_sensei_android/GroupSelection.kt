package com.example.kirikata_sensei_android

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kirikata_sensei_android.network.GlobalSocket
import com.example.kirikata_sensei_android.network.StudentSocketManager
import com.google.mediapipe.examples.handlandmarker.HandLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.hypot

class GroupSelection : BaseActivity(), HandLandmarkerHelper.LandmarkerListener {
    lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private val _result = mutableStateOf<HandLandmarkerHelper.ResultBundle?>(null)
    val result: State<HandLandmarkerHelper.ResultBundle?> = _result

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()

        loginAndSaveToken()

        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            handLandmarkerHelperListener = this
        )

        handLandmarkerHelper.maxNumHands = 2

        setContent {
            MaterialTheme {
                HandTrackingScreen(result = result, handHelper = handLandmarkerHelper)
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

    /** 🔐 サーバーへログインしてトークンを取得 */
    private fun loginAndSaveToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 認証API（修正版）
                val url = URL("http://bitter1326.mydns.jp:3000/api/auth")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                // ✅ 生徒アカウント情報
                val requestBody = JSONObject().apply {
                    put("username", "test1")
                    put("password", "test1")
                }.toString()

                // JSON送信
                conn.outputStream.use { os ->
                    os.write(requestBody.toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }

                if (responseCode == 200) {
                    val json = JSONObject(response)
                    val token = json.getString("token")

                    Log.d("JWT", "✅ トークン取得成功: $token")

                    val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    prefs.edit()
                        .putString("token", token)
                        .putString("username", "test1")
                        .apply()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@GroupSelection, "ログイン成功", Toast.LENGTH_SHORT).show()
                    }

                } else {
                    Log.e("JWT", "❌ ログイン失敗: $response")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@GroupSelection, "ログイン失敗: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

                conn.disconnect()

            } catch (e: Exception) {
                Log.e("JWT", "❌ 通信エラー: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GroupSelection, "通信エラー: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
    val scope = rememberCoroutineScope()

    var showLandmarks by remember { mutableStateOf(true) }

    var showControls by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }

    var countdown by remember { mutableStateOf(0) }
    var targetNumber by remember { mutableStateOf<Int?>(null) }
    var detectedNumber by remember { mutableStateOf<Int?>(null) }
    var stableFrames by remember { mutableStateOf(0) }

    // 状態管理
    var waitingForSession by remember { mutableStateOf(false) }
    var joiningError by remember { mutableStateOf<String?>(null) }
    var joinedSuccessfully by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingGroupNumber by remember { mutableStateOf<Int?>(null) }

    // 表示制御
    var showTaikiImage by remember { mutableStateOf(false) } // st_taiki.png
    var showStartImage by remember { mutableStateOf(false) } // st.png

    val REQUIRED_STABLE_FRAMES = 7
    var currentRoomName by remember { mutableStateOf("0") }

    // =========================
    // CheckCamera2 で保存した座標を読む
    // =========================
    // OK / CANCEL ボタンの画面内正規化座標 [0..1] をロード
    val okButtonTarget = remember { CalibrationStore.loadCoords(context, "OK_BUTTON") }       // Pair<Float, Float>
    val cancelButtonTarget = remember { CalibrationStore.loadCoords(context, "CANCEL_BUTTON") }

    // 空中タップ（ホバー・ホールド）判定用ステート
    var activeTarget by remember { mutableStateOf<String?>(null) }  // "OK" / "CANCEL" / null
    var holdFrames by remember { mutableStateOf(0) }
    val requiredFrames = 7 // 指先がしきい値内で留まるフレーム数
    var fingerTip by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    // MediaPipeの指先座標を取得（人差し指8番の tip）
    LaunchedEffect(result.value) {
        result.value?.let { bundle ->
            val hand = bundle.results.firstOrNull()?.landmarks()?.firstOrNull()
            if (hand != null && hand.size > 8) {
                fingerTip = hand[8].x() to hand[8].y()
            } else {
                fingerTip = null
                activeTarget = null
                holdFrames = 0
            }
        }
    }

    // ==============
    // エラー文を2秒後に消す
    // ==============
    val resetForRetry: () -> Unit = {
        joiningError = null
        joinedSuccessfully = false
        waitingForSession = false
        isConnecting = false
        detectedNumber = null
        targetNumber = null
        countdown = 0
        stableFrames = 0
        activeTarget = null
        holdFrames = 0
    }
    LaunchedEffect(joiningError) {
        if (joiningError != null) {
            delay(2000)
            resetForRetry()
        }
    }

    BottomSheetScaffold(
        sheetPeekHeight = if (showControls) 80.dp else 0.dp,
        sheetContent = {
            if (showControls)
                HandTrackingControls(
                    handHelper,
                    result.value?.inferenceTime,
                    defaultRoomName = currentRoomName,
                    onRoomNameChange = { newName ->
                        currentRoomName = newName
                    }
                )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures {
                        tapCount++
                        if (tapCount >= 5) {
                            showControls = !showControls
                            tapCount = 0
                        }
                    }
                }
        ) {
            // カメラプレビュー
            CameraPreview()
            if (showLandmarks) result.value?.let { LandmarkOverlay(it) }

            // ===== メッセージ枠UI =====
            if (!showConfirmDialog) { // ← 確認画面中は非表示
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .fillMaxWidth(0.9f)
                        .background(Color.White.copy(alpha = 0.9f), shape = RoundedCornerShape(20.dp))
                        .border(3.dp, Color(0xFFFF9800), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val message = when {
                        isConnecting -> "サーバーに接続中です..."
                        joiningError != null -> joiningError ?: "エラーが発生しました"
                        waitingForSession -> "先生の開始を待機中です..."
                        detectedNumber != null -> "グループ $detectedNumber に参加します"
                        countdown > 0 -> "選択まで ${countdown} 秒..."
                        else -> "グループの番号を指の本数で教えてください。"
                    }

                    Text(
                        text = message,
                        color = if (joiningError != null) Color.Red else Color.Black,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ===== グループ選択ロジック =====
            if (!joinedSuccessfully && !waitingForSession && joiningError == null && !isConnecting && !showConfirmDialog) {
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
                                        pendingGroupNumber = totalFingers
                                        showConfirmDialog = true
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
                } else if (totalFingers == 0) {
                    detectedNumber = null
                    targetNumber = null
                    countdown = 0
                    stableFrames = 0
                }
            }

            // ===============================
            // ✋ 空中タップ（ホバーホールド）判定
            // 確認ダイアログ表示中のみ有効
            // ===============================
            if (showConfirmDialog && pendingGroupNumber != null) {
                // 指先がターゲット（OK/CANCEL）に一定フレーム重なったら「押下」
                LaunchedEffect(fingerTip, showConfirmDialog, pendingGroupNumber) {
                    fingerTip?.let { (fx, fy) ->
                        fun isNear(target: Pair<Float, Float>?, threshold: Float = 0.10f): Boolean {
                            return target?.let { (tx, ty) ->
                                hypot(fx - tx, fy - ty) < threshold
                            } ?: false
                        }
                        val nearOk = isNear(okButtonTarget)
                        val nearCancel = isNear(cancelButtonTarget)

                        when {
                            nearOk -> {
                                if (activeTarget == "OK") {
                                    holdFrames++
                                    if (holdFrames >= requiredFrames) {
                                        // 「決定」確定
                                        holdFrames = 0
                                        activeTarget = null
                                        // クリックと同じ処理
                                        detectedNumber = pendingGroupNumber
                                        showConfirmDialog = false
                                        val chosen = pendingGroupNumber
                                        pendingGroupNumber = null
                                        scope.launch {
                                            val prefs = context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                                            val token = prefs.getString("token", null)
                                            val username = prefs.getString("username", "test1") ?: "生徒"
                                            val seatIndex = (chosen ?: 1) - 1
                                            if (token != null) {
                                                isConnecting = true
                                                val socketManager = StudentSocketManager(token, username)
                                                socketManager.connect(
                                                    onConnected = {
                                                        socketManager.joinRoom(
                                                            currentRoomName,
                                                            seatIndex,
                                                            onSuccess = {
                                                                joinedSuccessfully = true
                                                                waitingForSession = true
                                                                isConnecting = false
                                                                joiningError = null
                                                                showTaikiImage = true

                                                                GlobalSocket.manager = socketManager

                                                                socketManager.onSessionStarted { recipeId ->
                                                                    scope.launch {
                                                                        showTaikiImage = false
                                                                        showStartImage = true
                                                                        delay(2000)
                                                                        showStartImage = false

                                                                        // 再取得（念のため）
                                                                        val prefs2 = context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                                                                        val token2 = prefs2.getString("token", null)
                                                                        val username2 = prefs2.getString("username", "test1") ?: "生徒"
                                                                        val seatIndex2 = (detectedNumber ?: 1) - 1

                                                                        val intent = Intent(context, CookingActivity::class.java).apply {
                                                                            putExtra("recipeName", recipeId)
                                                                            putExtra("token", token2)
                                                                            putExtra("username", username2)
                                                                            putExtra("roomName", currentRoomName)
                                                                            putExtra("seatIndex", seatIndex2)
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

                                                            },
                                                            onError = { errMsg ->
                                                                isConnecting = false
                                                                joiningError = errMsg
                                                            }
                                                        )
                                                    },
                                                    onError = { errMsg ->
                                                        isConnecting = false
                                                        joiningError = "Socket接続エラー: $errMsg"
                                                    }
                                                )
                                            } else {
                                                joiningError = "トークンがありません"
                                            }
                                        }
                                    }
                                } else {
                                    activeTarget = "OK"
                                    holdFrames = 0
                                }
                            }
                            nearCancel -> {
                                if (activeTarget == "CANCEL") {
                                    holdFrames++
                                    if (holdFrames >= requiredFrames) {
                                        // 「もどる」確定
                                        holdFrames = 0
                                        activeTarget = null
                                        showConfirmDialog = false
                                        pendingGroupNumber = null
                                        targetNumber = null
                                        stableFrames = 0
                                        countdown = 0
                                    }
                                } else {
                                    activeTarget = "CANCEL"
                                    holdFrames = 0
                                }
                            }
                            else -> {
                                // どちらにも近くない
                                activeTarget = null
                                holdFrames = 0
                            }
                        }
                    } ?: run {
                        // 指が検出されていない
                        activeTarget = null
                        holdFrames = 0
                    }
                }

                // ====== 確認ダイアログ ======
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(0.7f)
                            .background(Color.White, RoundedCornerShape(24.dp))
                            .border(3.dp, Color(0xFFFF9800), RoundedCornerShape(24.dp))
                            .padding(vertical = 48.dp, horizontal = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(48.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${pendingGroupNumber}グループですか？",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                ),
                                textAlign = TextAlign.Center
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                // 決定ボタン（クリックでもOK：空中タップと同等処理）
                                Button(
                                    onClick = {
                                        // クリック経路
                                        val chosen = pendingGroupNumber
                                        detectedNumber = chosen
                                        showConfirmDialog = false
                                        pendingGroupNumber = null

                                        scope.launch {
                                            val prefs = context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                                            val token = prefs.getString("token", null)
                                            val username = prefs.getString("username", "test1") ?: "生徒"
                                            val seatIndex = (chosen ?: 1) - 1

                                            if (token != null) {
                                                isConnecting = true
                                                val socketManager = StudentSocketManager(token, username)
                                                socketManager.connect(
                                                    onConnected = {
                                                        socketManager.joinRoom(
                                                            currentRoomName,
                                                            seatIndex,
                                                            onSuccess = {
                                                                joinedSuccessfully = true
                                                                waitingForSession = true
                                                                isConnecting = false
                                                                joiningError = null
                                                                showTaikiImage = true

                                                                GlobalSocket.manager = socketManager

                                                                // セッション開始を受信
                                                                socketManager.onSessionStarted { recipeId ->
                                                                    scope.launch {
                                                                        showTaikiImage = false
                                                                        showStartImage = true
                                                                        delay(2000)
                                                                        showStartImage = false

                                                                        val prefs2 = context.getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                                                                        val token2 = prefs2.getString("token", null)
                                                                        val username2 = prefs2.getString("username", "test1") ?: "生徒"
                                                                        val seatIndex2 = (detectedNumber ?: 1) - 1

                                                                        val intent = Intent(context, CookingActivity::class.java).apply {
                                                                            putExtra("recipeName", recipeId) // ← サーバーからのレシピ情報
                                                                            putExtra("token", token2)
                                                                            putExtra("username", username2)
                                                                            putExtra("roomName", currentRoomName)
                                                                            putExtra("seatIndex", seatIndex2)
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

                                                            },
                                                            onError = { errMsg ->
                                                                isConnecting = false
                                                                joiningError = errMsg
                                                            }
                                                        )
                                                    },
                                                    onError = { errMsg ->
                                                        isConnecting = false
                                                        joiningError = "Socket接続エラー: $errMsg"
                                                    }
                                                )
                                            } else {
                                                joiningError = "トークンがありません"
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeTarget == "OK") Color(0xFF00C853) else Color(0xFF4CAF50)
                                    ),
                                    modifier = Modifier.width(150.dp).height(90.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("決定", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }

                                // もどるボタン（クリックでもOK）
                                Button(
                                    onClick = {
                                        showConfirmDialog = false
                                        pendingGroupNumber = null
                                        targetNumber = null
                                        stableFrames = 0
                                        countdown = 0
                                        activeTarget = null
                                        holdFrames = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (activeTarget == "CANCEL") Color(0xFFFF5252) else Color(0xFFF44336)
                                    ),
                                    modifier = Modifier.width(150.dp).height(90.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("もどる", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ====== 先生の開始待機中画面 ======
            if (showTaikiImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.st_taiki),
                        contentDescription = "先生待機中",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // ====== セッション開始合図画面 ======
            if (showStartImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.st),
                        contentDescription = "セッション開始",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}


// ======== Finger 判定関数（変更なし） ========

fun countExtendedFingersImproved(landmarks: List<NormalizedLandmark>): Int {
    if (landmarks.size < 21) return 0
    var count = 0
    val isLeftHand = landmarks[17].x() < landmarks[5].x()

    if (isLeftHand) {
        if (landmarks[4].x() > landmarks[3].x() && landmarks[4].x() > landmarks[2].x()) count++
    } else {
        if (landmarks[4].x() < landmarks[3].x() && landmarks[4].x() < landmarks[2].x()) count++
    }

    if (landmarks[8].y() < landmarks[6].y() && landmarks[8].y() < landmarks[5].y()) count++
    if (landmarks[12].y() < landmarks[10].y() && landmarks[12].y() < landmarks[9].y()) count++
    if (landmarks[16].y() < landmarks[14].y() && landmarks[16].y() < landmarks[13].y()) count++
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
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        0 to 9, 9 to 10, 10 to 11, 11 to 12,
        0 to 13, 13 to 14, 14 to 15, 15 to 16,
        0 to 17, 17 to 18, 18 to 19, 19 to 20,
        5 to 9, 9 to 13, 13 to 17
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
            points.forEach { pt -> drawCircle(Color.Red, radius = 6f, center = pt) }
        }
    }
}

@Composable
fun HandTrackingControls(
    handHelper: HandLandmarkerHelper,
    inferenceTime: Long?,
    defaultRoomName: String = "5", // ← 初期ルーム名
    onRoomNameChange: (String) -> Unit = {} // ← ルーム名変更コールバック
) {
    var detectionThreshold by remember { mutableStateOf(handHelper.minHandDetectionConfidence) }
    var trackingThreshold by remember { mutableStateOf(handHelper.minHandTrackingConfidence) }
    var presenceThreshold by remember { mutableStateOf(handHelper.minHandPresenceConfidence) }
    var maxHands by remember { mutableStateOf(handHelper.maxNumHands) }
    var delegate by remember { mutableStateOf(handHelper.currentDelegate) }

    // 🆕 ルーム名入力ステート
    var roomName by remember { mutableStateOf(defaultRoomName) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("推論時間: ${inferenceTime ?: "-"} ms")

        // ===== ルーム名入力フィールド =====
        Text(
            text = "ルーム名設定",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        androidx.compose.material3.OutlinedTextField(
            value = roomName,
            onValueChange = {
                roomName = it
                onRoomNameChange(it) // ← コールバック通知
            },
            label = { Text("Room Name") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // ===== 以下は既存の閾値調整など =====
        ThresholdControl("Detection", detectionThreshold,
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

        ThresholdControl("Tracking", trackingThreshold,
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

        ThresholdControl("Presence", presenceThreshold,
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Delegate: ${if (delegate == HandLandmarkerHelper.DELEGATE_CPU) "CPU" else "GPU"}")
            Button(onClick = {
                delegate = if (delegate == HandLandmarkerHelper.DELEGATE_CPU)
                    HandLandmarkerHelper.DELEGATE_GPU else HandLandmarkerHelper.DELEGATE_CPU
                handHelper.currentDelegate = delegate
                resetHelper(handHelper)
            }) { Text("切替") }
        }
    }
}


@Composable
fun ThresholdControl(label: String, value: Float, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
