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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme

class ManagementScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Kirikata_Sensei_AndroidTheme {
                ManagementScreenUI()
            }
        }
    }
}

@Composable
fun ManagementScreenUI() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("管理画面（メイン機能）")
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
