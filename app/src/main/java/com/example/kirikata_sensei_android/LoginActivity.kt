package com.example.kirikata_sensei_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme
import io.socket.client.IO
import io.socket.client.Socket

class LoginActivity : BaseActivity() {

    private lateinit var socket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // Socket.IOサーバに接続
        try {
            val options = IO.Options()
            options.reconnection = true
            socket = IO.socket("https://deploy-backend-u68u.onrender.com", options)
            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            Kirikata_Sensei_AndroidTheme {
                LoginScreen(onLoginSuccess = {
                    val sharedPref = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                    sharedPref.edit().apply {
                        putBoolean("isLoggedIn", true)
                        apply()
                    }
                    startActivity(Intent(this, CreateClass::class.java))
                    finish()
                })
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var showTeacherDialog by remember { mutableStateOf(false) }

    // ✅ 隠しボタン制御用
    var showCameraButton by remember { mutableStateOf(false) }
    var showTflTestButton by remember { mutableStateOf(false) }
    var showCamera2Button by remember { mutableStateOf(false) } // ← 追加！
    var tapCount by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable {
                tapCount++
                if (tapCount >= 5) {
                    // 5回タップで全部トグル切り替え
                    showCameraButton = !showCameraButton
                    showTflTestButton = !showTflTestButton
                    showCamera2Button = !showCamera2Button  // ← 追加！
                    tapCount = 0
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showTeacherDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("先生用ログイン")
            }

            Spacer(Modifier.height(16.dp))

            val context = LocalContext.current
            Button(
                onClick = {
                    context.startActivity(Intent(context, GroupSelection::class.java))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("生徒用ログイン")
            }
        }

        // ✅ 隠し「カメラ確認」ボタン
        if (showCameraButton) {
            val context = LocalContext.current
            Button(
                onClick = {
                    context.startActivity(Intent(context, CheckCamera::class.java))
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text("カメラ確認")
            }
        }

        // ✅ 新規追加：隠し「カメラ確認２」ボタン
        if (showCamera2Button) {
            val context = LocalContext.current
            Button(
                onClick = {
                    context.startActivity(Intent(context, CheckCamera2::class.java))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            ) {
                Text("カメラ確認２")
            }
        }

        // ✅ 隠し「TFLテスト」ボタン
        if (showTflTestButton) {
            val context = LocalContext.current
            Button(
                onClick = {
                    context.startActivity(Intent(context, TflTestActivity::class.java))
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text("TFLテスト")
            }
        }

        if (showTeacherDialog) {
            TeacherLoginDialog(
                onDismiss = { showTeacherDialog = false },
                onLoginSuccess = {
                    showTeacherDialog = false
                    onLoginSuccess()
                }
            )
        }
    }
}



@Composable
fun TeacherLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

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
                if (username == "test1" && password == "1test") {
                    onLoginSuccess()
                } else {
                    errorMessage = "ユーザーネームまたはパスワードが間違っています"
                }
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
