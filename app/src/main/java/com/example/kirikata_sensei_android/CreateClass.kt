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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kirikata_sensei_android.ui.theme.Kirikata_Sensei_AndroidTheme

class CreateClass : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Kirikata_Sensei_AndroidTheme {
                CreateClassScreen(
                    onNextClick = {
                        startActivity(Intent(this, StandbyScreen::class.java))
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun CreateClassScreen(
    onNextClick: () -> Unit
) {

    var className by remember { mutableStateOf("") }
    var groupCount by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        /*
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ここにクラス作成画面")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onNextClick() }) {
                Text("次の画面へ")
            }
        }
         */
        Column {
            //クラス名
            OutlinedTextField(
                value = className,
                onValueChange = { className = it },
                label = { Text("クラス名を入力してください") },
                singleLine = true
            )
            //グループ数
            Row(
                modifier = Modifier.wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (groupCount > 0) {
                            groupCount--
                        }
                    },
                    enabled = groupCount > 0
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_remove_24),
                        contentDescription = "グループ数を減らす",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = groupCount.toString(),
                    fontSize = 24.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                IconButton(
                    onClick = {
                        groupCount++
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus_circle),
                        contentDescription = "グループ数を増やす",
                        modifier = Modifier.size(24.dp)
                    )
                }
                /**
                OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("パスワード") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
                )
                 */
            }
            //レシピ選択画面
            OutlinedTextField(
                value = className,
                onValueChange = { className = it },
                label = { Text("レシピを選択") },
                singleLine = true
            )
        }
    }
}
