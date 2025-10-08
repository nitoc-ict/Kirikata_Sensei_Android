package com.example.kirikata_sensei_android

import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/*
class LoginActivity : ComponentActivity() {
    private var socket: Socket? = null
    private val SOCKET_URL = "https://deploy-backend-u68u.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            Kirikata_Sensei_AndroidTheme {
                LoginScreen(
                    onLoginSuccess = { context ->
                        // CreateClass.ktへ遷移
                        // SharedPreferences保存ロジックは移動
                        val sharedPref = context.getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putBoolean("isLoggedIn", true)
                            apply()
                        }

                        context.startActivity(Intent(context, CreateClass::class.java))
                        finish()
                    },
                    onTeacherLoginAttempt = { username, password, context, onSuccess ->
                        // ログイン試行（JWT取得とSocket接続）
                        scope.launch {
                            performTeacherLogin(username, password, context, onSuccess)
                        }
                    }
                )
            }
        }
    }

    // 先生用ログイン処理（HTTP認証とSocket.IO接続を兼ねる）
    private suspend fun performTeacherLogin(
        username: String,
        password: String,
        context: Context,
        onSuccess: (Context) -> Unit
    ) = withContext(Dispatchers.IO) {
        // --- 1. HTTP APIで認証を行い、JWTトークンを取得するフェーズ（今回はモック） ---
        // 実際にはここでKtorやRetrofitなどを使って、ユーザー名とパスワードをREST APIにPOSTします。
        // 成功した場合のみ、JWTトークンが返されます。

        if (username != "test1" || password != "1test") {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ユーザーネームまたはパスワードが間違っています", Toast.LENGTH_SHORT).show()
            }
            return@withContext
        }

        // 認証成功と仮定し、モックトークンを使用
        val mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.TEST_TOKEN_FOR_$username"
        Log.d("SocketConnect", "Mock Token Acquired: $mockJwtToken")

        // --- 2. 取得したJWTトークンを使用してSocket.IOに接続するフェーズ ---
        try {
            val options = IO.Options.builder()
                .setReconnection(true)
                // 認証トークンを設定（APIドキュメントの要件）
                .setAuth(mapOf("token" to mockJwtToken))
                .build()

            // 既存の接続があれば切断
            socket?.disconnect()
            socket = IO.socket(SOCKET_URL, options)

            // 接続リスナーを設定
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketConnect", "Socket Connected Successfully with JWT")
                // メインスレッドで成功コールバックを実行し、画面遷移
                onSuccess(context)
            }?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args[0] as Exception
                Log.e("SocketConnect", "Connection Error: ${error.message}")

                // 認証エラーのハンドリング（APIドキュメントのエラーケース）
                val errorMessage = when (error.message) {
                    "トークンが必要です" -> "認証トークンが提供されていません"
                    "無効なトークンです" -> "認証トークンが無効です"
                    else -> "サーバーに接続できません: ${error.message}"
                }
                /*
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                 */
            }

            // 接続開始
            socket?.connect()

        } catch (e: Exception) {
            Log.e("SocketConnect", "Socket Initialization Error", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Socket.IOの初期化エラー", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// LoginScreen のシグネチャを変更し、ログイン処理を渡せるようにします
@Composable
fun LoginScreen(
    onLoginSuccess: (Context) -> Unit,
    onTeacherLoginAttempt: (username: String, password: String, context: Context, onSuccess: (Context) -> Unit) -> Unit
) {
    var showTeacherDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (既存のボタン)

            Button(
                onClick = { showTeacherDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("先生用ログイン")
            }
        }

        //先生用ログインダイアログ
        if (showTeacherDialog) {
            TeacherLoginDialog(
                onDismiss = { showTeacherDialog = false },
                onLoginAttempt = onTeacherLoginAttempt,
                onLoginSuccess = { context ->
                    showTeacherDialog = false
                    onLoginSuccess(context)
                }
            )
        }
    }
}

// TeacherLoginDialog のシグネチャを変更し、ログイン試行の処理を受け取るようにします
@Composable
fun TeacherLoginDialog(
    onDismiss: () -> Unit,
    onLoginAttempt: (username: String, password: String, context: Context, onSuccess: (Context) -> Unit) -> Unit,
    onLoginSuccess: (Context) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current // Contextを取得

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("先生用ログイン") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("ユーザーネーム") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                errorMessage = "" // エラーメッセージをリセット
                // 認証ロジックを Activity 側に委譲
                onLoginAttempt(username, password, context, onLoginSuccess)
            }) {
                Text("ログイン")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onDismiss() }) {
                Text("キャンセル")
            }
        }
    )
}
*/

class LoginActivity : ComponentActivity() {
    private var socket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            Kirikata_Sensei_AndroidTheme {
                LoginScreen(
                    onLoginSuccess = { context ->
                        // CreateClass.ktへ遷移
                        // SharedPreferences保存ロジックは移動
                        val sharedPref = context.getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                        sharedPref.edit().apply {
                            putBoolean("isLoggedIn", true)
                            apply()
                        }

                        context.startActivity(Intent(context, CreateClass::class.java))
                        finish()
                    },
                    onTeacherLoginAttempt = { username, password, context, onSuccess,->
                        scope.launch {
                            performTeacherLogin(username, password, context, onSuccess)
                        }
                    }
                )
            }
        }
    }



    // 先生用ログイン処理（HTTP認証とSocket.IO接続を兼ねる）
    private val auth_url = "http://163.43.209.9:3000/api/auth"//DNS解決してくれない
    private val json_media_type = "application/json; charset=utf-8".toMediaType()

    private suspend fun performTeacherLogin(
        username: String,
        password: String,
        context: Context,
        onSuccess: (Context) -> Unit
    ) = withContext(Dispatchers.IO) {

        //OkHttpクライアントの準備
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // 接続タイムアウトを30秒に延長
            .readTimeout(30, TimeUnit.SECONDS)    // 読み取りタイムアウトを30秒に延長
            .writeTimeout(30, TimeUnit.SECONDS)   // 書き込みタイムアウトを30秒に延長
            .build()

        //認証リクエストのJSONボディを作成
        val authJson = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        //認証リクエストの構築
        val requestBody = authJson.toString().toRequestBody(json_media_type)
        Log.d("RequestDebug", "Sending JSON: ${authJson.toString()}")

        val request = Request.Builder()
            .url(auth_url)
            .post(requestBody)
            .build()

        try {
            //リクエストの実行とレスポンスの処理
            client.newCall(request).execute().use { response ->

                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    //認証成功：レスポンスからJWTトークンを抽出
                    val jsonResponse = JSONObject(responseBody)
                    val jwtToken = jsonResponse.getString("token")

                    Log.d("Auth", "JWT Token Acquired: $jwtToken")

                    //JWTトークンを使ってSocket.IOに接続（既存ロジック）
                    connectSocketWithJwt(jwtToken, context, onSuccess)

                } else {
                    // 認証失敗：HTTPエラーコードやサーバーメッセージを表示
                    val errorMsg = "ログイン失敗: ${response.code} ${response.message}"
                    Log.e("Auth", errorMsg)
                }
            }
        } catch (e: Exception) {
            // ネットワークエラー、タイムアウト
            Log.e("Auth", "Network Error during Login JWT", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "ネットワークエラーが発生しました", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Socket.IO接続ロジックを分離（既存のロジックから抽出）
    private fun connectSocketWithJwt(jwtToken: String, context: Context, onSuccess: (Context) -> Unit) {
        val socket_url = "http://163.43.209.9:3000"

        try {
            val options = IO.Options.builder()
                .setReconnection(true)
                .setAuth(mapOf("token" to jwtToken))
                .build()

            socket?.disconnect()
            socket = IO.socket(socket_url, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("SocketConnect", "Socket Connected Successfully with JWT")
                onSuccess(context) // 成功したら画面遷移
            }?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args[0] as Exception
                Log.e("SocketConnect", "Connection Error: ${error.message}")

                // 認証エラーのハンドリング（エラーメッセージはUIに表示すべき）
                val errorMessage = when (error.message) {
                    "トークンが必要です" -> "認証トークンが提供されていません"
                    "無効なトークンです" -> "認証トークンが無効です"
                    else -> "Socket接続エラー: ${error.message}"
                }

                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }

            }

            socket?.connect()

        } catch (e: Exception) {
            //Socket.IO初期化エラー処理
            Log.e("Socket.IO Error", "Socket.IO Error")
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (Context) -> Unit,
    onTeacherLoginAttempt: (username: String, password: String, context: Context, onSuccess: (Context) -> Unit) -> Unit
) {
    var showTeacherDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { /* 生徒用は後で実装 */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("生徒用ログイン（後日実装）")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showTeacherDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("先生用ログイン")
            }
        }

        //先生用ログインダイアログ
        if (showTeacherDialog) {
            TeacherLoginDialog(
                onDismiss = { showTeacherDialog = false },
                onLoginAttempt = onTeacherLoginAttempt,
                onLoginSuccess = { context ->
                    showTeacherDialog = false
                    onLoginSuccess(context)
                }
            )
        }
    }
}

@Composable
fun TeacherLoginDialog(
    onDismiss: () -> Unit,
    onLoginAttempt: (username: String, password: String, context: Context, onSuccess: (Context) -> Unit) -> Unit,
    onLoginSuccess: (Context) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current // Contextを取得

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("先生用ログイン") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("ユーザーネーム") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("パスワード") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                errorMessage = "" // エラーメッセージをリセット
                // 認証ロジックを Activity 側に委譲
                onLoginAttempt(username, password, context, onLoginSuccess)
            }) {
                Text("ログイン")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onDismiss() }) {
                Text("キャンセル")
            }
        }
    )
}