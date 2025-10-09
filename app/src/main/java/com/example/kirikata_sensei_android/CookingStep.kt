package com.example.kirikata_sensei_android.model

data class CookingStep(
    val imageResId: Int?,       // 調理手順画像。包丁フェーズでは null
    val requiresKnife: Boolean  // 包丁フェーズかどうか
)
