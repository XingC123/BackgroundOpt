package com.venus.backgroundopt.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.R

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

fun <E : View> Activity.findViewById(resId: Int, enable: Boolean = true): E? {
    return if (enable) {
        this.findViewById(resId)
    } else {
        findViewById<E>(resId)?.apply {
            this.isEnabled = false
        }
    }
}

fun getToolbar(
    activity: Activity,
    toolbarResId: Int,
    titleStr: String? = null,
    titleResId: Int? = null,
    titleTextColor: Int = Color.WHITE,
    navigationIcon: Drawable? = ResourcesCompat.getDrawable(
        activity.resources,
        R.drawable.baseline_arrow_back_24,
        null
    ),
    navigationOnClickListener: (View) -> Unit = { _ -> activity.finish() }
): Toolbar? {
    return activity.findViewById<Toolbar?>(toolbarResId)?.apply {
        // 设置标题
        titleStr?.let { this.title = titleStr } ?: run {
            titleResId?.let {
                this.setTitle(titleResId)
            } ?: run {
                this.title = ""
            }
        }

        this.setTitleTextColor(titleTextColor)

        // 设置返回按钮
        navigationIcon?.let {
            this.navigationIcon = navigationIcon
        }
        setNavigationOnClickListener(navigationOnClickListener)
    }
}

