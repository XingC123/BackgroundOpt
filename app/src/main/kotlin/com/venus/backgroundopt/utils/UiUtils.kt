package com.venus.backgroundopt.utils

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import com.alibaba.fastjson2.JSON

/**
 * @author XingC
 * @date 2023/9/25
 */

// 临时数据。在两个Activity之间使用。有线程安全问题
@JvmField
var TMP_DATA: Any? = null

@JvmField
var TMP_DATA_LIST: ArrayList<Any>? = null

fun putTmpListData(vararg value: Any) {
    TMP_DATA_LIST = arrayListOf(*value)
}

fun getTmpListData(): ArrayList<Any>? {
    val list = TMP_DATA_LIST
    TMP_DATA_LIST = null

    return list
}

@Suppress("UNCHECKED_CAST")
fun <E> getTmpListData(index: Int): E? {
    return TMP_DATA_LIST?.get(index) as? E
}

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

fun getView(context: Context, layoutResId: Int): View =
    LayoutInflater.from(context).inflate(layoutResId, null)
