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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme

class StandbyScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 前のActivityから渡されたグループ数を取得
        val groupCount = intent.getIntExtra("GROUP_COUNT", 0)

        enableEdgeToEdge()
        setContent {
            Kirikata_Sensei_AndroidTheme {
                StandbyScreenUI(
                    groupCount = groupCount,
                    onNextClick = {
                        startActivity(Intent(this, ManagementScreen::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun StandbyScreenUI(groupCount: Int, onNextClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "生徒の入室待ちです。",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // グリッド状に要素を効率よく表示するためのComposable
        LazyVerticalGrid(
            // 列数を指定 (ここでは3列)
            columns = GridCells.Fixed(3),
            // グリッド全体のパディング
            contentPadding = PaddingValues(8.dp),
            // グリッド内のアイテム間のスペース
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // groupCountの数だけループしてアイテムを表示
            items(groupCount) { number ->
                // SquareBoxを呼び出し、表示する番号を渡す (1から始まるように +1 する)
                SquareBox(number = number + 1)
            }
        }

        /*通知画面作成予定
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x80808080)),
        contentAlignment = Alignment.CenterEnd
        ) {
        Text(
            text = "hello",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        }
         */

        Spacer(modifier = Modifier.height(24.dp))

        // ここに「全員揃ったら表示する」処理を追加
        Button(onClick = { onNextClick() }) {
            Text("授業を始める")
        }
    }
}

//グループ表示
@Composable
fun SquareBox(number: Int) {
    Box(
        modifier = Modifier
            .size(128.dp, 128.dp)
            .clip(RoundedCornerShape(12.dp)) // 角を丸くする
            .background(Color(0x80808080)), // 背景色を灰色に設定
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}