/*
ここでは待機画面を表示する
    - 入室状況が分かるようにする
    - 全員入ったら、授業開始のボタンを表示
授業開始ボタンを押したらManagementScreen.kt（管理画面）へ遷移
 */
package com.example.kirikata_sensei_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme
import androidx.compose.material3.Button


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
                        val intent = Intent(this@StandbyScreen, ManagementScreen::class.java).apply {
                            // "GROUP_COUNT"というキーで groupCount の値をセット
                            putExtra("GROUP_COUNT", groupCount)
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun StandbyScreenUI(groupCount: Int, onNextClick: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top // 垂直方向は上部寄せ（ボタン表示のため）
    ) {
        // 1. 【左側の余白】 weight(1f)
        Spacer(modifier = Modifier.weight(1f))

        // 2. 【中央のコンテンツ】 weight(2f)
        Column(
            modifier = Modifier
                .weight(2f) // 画面全体の約50%の幅を占有
                .fillMaxHeight() // 高さはすべて使用
                .padding(vertical = 16.dp), // 左右の余白は Spacer で確保されるため、上下のみパディング
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // ボタン表示のため Top に設定
        ) {
            Text(
                text = "生徒の入室待ちです。",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            // LazyVerticalGrid
            LazyVerticalGrid(
                // ... (中略: LazyVerticalGrid の引数はそのまま) ...
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                // 高さを明示的に指定しない（コンテンツのサイズに合わせる）か、または適度な高さに制限する
                modifier = Modifier.fillMaxWidth().height(650.dp) // 幅いっぱいに広がり、高さを制限
            ) {
                items(groupCount) { number ->
                    SquareBox(number = number + 1)
                }
            }

            // 下部への配置のため、Spacer に weight を設定
            Spacer(modifier = Modifier.weight(1f))

            // 授業開始ボタン //全員揃ったら押せるようにする
            Button(onClick = { onNextClick(groupCount) }) {
                Text("授業を始める")
            }
        }

        // 3. 【右側の余白】 weight(1f)
        Spacer(modifier = Modifier.weight(1f))
    }
}

//グループ表示
@Composable
fun SquareBox(number: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize() // 親コンテナの幅いっぱいに広げる
            .aspectRatio(1f) // 縦横比を1:1（正方形）に固定する
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