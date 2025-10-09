/*
ここでは管理画面（メイン機能）を表示
    - グループ別進行度
        - ～ページまたは～手順目
    - 危険行動の通知
    - 危険通知の履歴
        - ～ページまたは～手順目
    - 教科書をみる画面
 */
package com.example.kirikata_sensei_android

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme

class ManagementScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupCount = intent.getIntExtra("GROUP_COUNT", 0)
        enableEdgeToEdge()
        setContent {
            Kirikata_Sensei_AndroidTheme {
                ManagementScreenUI(
                    groupCount = groupCount
                )
            }
        }
    }
}

@Composable
fun ManagementScreenUI(groupCount: Int) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top // 垂直方向は上部寄せ（ボタン表示のため）
    ) {
        Spacer(modifier = Modifier.weight(0.65f))//左スペース
        //Spacer(modifier = Modifier.padding(60.dp))

        Column(
            modifier = Modifier
                .weight(3f)
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.weight(1f))//上スペース

            // グリッド状に要素を効率よく表示するためのComposable
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                // グリッド全体のパディング
                contentPadding = PaddingValues(12.dp),
                // グリッド内のアイテム間のスペース
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                //modifier = Modifier.fillMaxWidth().height(650.dp)
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // groupCountの数だけループしてアイテムを表示
                items(groupCount) {
                    // SquareBoxを呼び出し、表示する番号を渡す (1から始まるように +1 する)
                    Group_management()
                }
            }

            Spacer(modifier = Modifier.weight(1f))//下スペース
        }

        Spacer(modifier = Modifier.weight(0.65f))//右スペース
        //Spacer(modifier = Modifier.padding(60.dp))

        //通知画面用
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFF0F0F0)) // 薄い灰色
                .padding(20.dp)
        ) {
            Text(
                text = "危険予測の通知履歴",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            // ここに通知リスト（LazyColumnなど）のComposableを配置
            // (省略: スクリーンショットの通知リストを実装するには追加のComposableが必要)
        }
    }

}

//グループ表示
@Composable
fun Group_management() {
    Box(
        modifier = Modifier
            .fillMaxSize(0.5f) // 親コンテナの幅いっぱいに広げる
            .aspectRatio(1f) // 【重要】縦横比を1:1（正方形）に固定する
            .clip(RoundedCornerShape(12.dp)) // 角を丸くする
            .background(Color(0xFF578A32)), // 背景色を灰色に設定
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ("%"),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}