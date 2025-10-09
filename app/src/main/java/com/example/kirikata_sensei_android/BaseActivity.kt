package com.example.kirikata_sensei_android

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

open class BaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 横画面に固定
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // EdgeToEdge 有効化
        WindowCompat.setDecorFitsSystemWindows(window, false)

        //フルスクリーンモードに入る
        enterImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        // Activityに戻った時もフルスクリーンに
        enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars()) // ステータスバー＆ナビバー非表示
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
