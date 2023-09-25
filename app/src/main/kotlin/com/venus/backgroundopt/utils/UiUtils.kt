package com.venus.backgroundopt.utils

import android.content.Intent

/**
 * @author XingC
 * @date 2023/9/25
 */

fun setIntentData(intent: Intent, data: String?) {
    intent.putExtra("data", data)
}

fun getIntentData(intent: Intent): String? {
    return intent.getStringExtra("data")
}