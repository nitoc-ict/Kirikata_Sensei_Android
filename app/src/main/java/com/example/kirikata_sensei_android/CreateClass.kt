/*
ここではクラスを作成する画面を表示
    - グループ数(机の数)
    - クラス名の作成
    - 使う教材(レシピ)の選択
クラスを作れたらStandbyScreen.kt(待機画面)へ遷移。
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

class CreateClass : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Kirikata_Sensei_AndroidTheme {
                CreateClassScreen(onNextClick = {
                    startActivity(Intent(this, StandbyScreen::class.java))
                    finish()
                })
            }
        }
    }
}

@Composable
fun CreateClassScreen(onNextClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ここにクラス作成画面")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onNextClick() }) {
                Text("次の画面へ")
            }
        }
    }
}
