package com.venus.backgroundopt.utils

import android.content.Intent
import com.alibaba.fastjson2.JSON

/**
 * @author XingC
 * @date 2023/9/25
 */

// 临时数据。在两个Activity之间使用。有线程安全问题
@JvmField
var TMP_DATA: Any? = null

fun setIntentData(intent: Intent, data: String?) {
    intent.putExtra("data", data)
}

fun setIntentData(intent: Intent, data: Any?) {
    intent.putExtra("data", JSON.toJSONString(data))
}

fun getIntentData(intent: Intent): String? {
    return intent.getStringExtra("data")
}

inline fun <reified E> getIntentData(intent: Intent): E? {
    return getIntentData(intent)?.let {
        JSON.parseObject(it, E::class.java)
    }
}

inline fun <reified E> getIntentDataToList(intent: Intent): List<E>? {
    return getIntentData(intent)?.let {
        JSON.parseArray(it, E::class.java)
    }
}
