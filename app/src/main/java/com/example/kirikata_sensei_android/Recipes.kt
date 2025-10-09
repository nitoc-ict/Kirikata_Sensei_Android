package com.example.kirikata_sensei_android.model

import com.example.kirikata_sensei_android.R

object Recipes {
    val sandwich = listOf(
        CookingStep(R.drawable.step1, false),
        CookingStep(R.drawable.step2, false),
        CookingStep(null, true),   // 包丁フェーズ
        CookingStep(R.drawable.step3, false),
        CookingStep(R.drawable.step4, false)
    )


    //val curry = listOf(
        //CookingStep(R.drawable.curry1, false),
        //CookingStep(null, true),
        //CookingStep(R.drawable.curry2, false)
    //)
}
