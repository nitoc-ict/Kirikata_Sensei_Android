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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
                    // onNextClickが呼ばれたときに、3つの値を受け取るように変更
                    onNextClick = { className, groupCount, recipe ->
                        // Intentに値を追加して次の画面に渡す
                        val intent = Intent(this, StandbyScreen::class.java).apply {
                            putExtra("CLASS_NAME", className)
                            putExtra("GROUP_COUNT", groupCount)
                            putExtra("RECIPE_NAME", recipe)
                        }
                        startActivity(intent)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun CreateClassScreen(
    // ボタンが押されたときに、入力された値を渡すように引数の型を変更
    onNextClick: (String, Int, String) -> Unit
) {
    var className by remember { mutableStateOf("") }
    var groupCount by remember { mutableIntStateOf(0) }
    var recipe by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column {
            //クラス名
            OutlinedTextField(
                value = className,
                onValueChange = { className = it },
                label = { Text("クラス名を入力してください") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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
            }

            Spacer(modifier = Modifier.height(16.dp))

            //レシピ選択画面
            OutlinedTextField(
                value = recipe,
                onValueChange = { recipe = it },
                label = { Text("レシピを選択") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ボタンが押されたら、現在の3つの状態をonNextClickに渡す
            Button(onClick = { onNextClick(className, groupCount, recipe) }) {
                Text("クラスを作成して次へ")
            }
        }
    }
}