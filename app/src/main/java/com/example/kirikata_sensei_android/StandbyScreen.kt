/*
ここでは待機画面を表示する
    - 入室状況が分かるようにする
    - 全員入ったら、授業開始のボタンを表示
授業開始ボタンを押したらManagementScreen.kt（管理画面）へ遷移
 */
package com.example.kirikata_sensei_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme

class StandbyScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Kirikata_Sensei_AndroidTheme {
                StandbyScreenUI(onNextClick = {
                    startActivity(Intent(this, ManagementScreen::class.java))
                    finish()
                })
            }
        }
    }
}

@Composable
fun StandbyScreenUI(onNextClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ここに待機画面作成")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onNextClick() }) {
                Text("次の画面へ")
            }

        }
    }
}
