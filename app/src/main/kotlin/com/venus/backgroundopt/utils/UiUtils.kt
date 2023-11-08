package com.venus.backgroundopt.utils

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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

object UiUtils {
    /**
     * 创建对话框
     *
     * @param context 当前上下文
     * @param viewResId 布局文件资源id
     * @param cancelable 对话框是否可以取消
     * @return [AlertDialog]
     */
    @JvmOverloads
    fun createDialog(context: Context, viewResId: Int, cancelable: Boolean = true): AlertDialog {
        return createDialog(context, viewResId, "", cancelable)
    }

    @JvmOverloads
    fun createDialog(
        context: Context,
        viewResId: Int,
        text: String,
        cancelable: Boolean = true
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setCancelable(cancelable)
            .setView(getView(context, viewResId).apply {
                findViewById<TextView>(R.id.contentCommonDialogText)?.text = text
            })
            .create()
    }

    @JvmOverloads
    fun createDialog(context: Context, text: String, cancelable: Boolean = true): AlertDialog {
        return createDialog(
            context,
            viewResId = R.layout.content_common_dailog_view,
            text,
            cancelable
        )
    }

    /**
     * 获取[Toolbar]
     *
     * @param activity [Toolbar]所在的[Activity]
     * @param toolbarResId [Toolbar]的资源id
     * @param titleStr [Toolbar]的标题
     * @param titleResId [Toolbar]的标题的资源id
     * @param enableCancelBtn 是否使用返回键
     * @param enableMenu 是否启用菜单
     * @param menuResId 菜单的资源id
     * @param menuOnClickListenerBlock 菜单项[MenuItem]被点击的事件
     * @return
     */
    @JvmOverloads
    fun getToolbar(
        activity: Activity,
        toolbarResId: Int,
        titleStr: String? = null,
        titleResId: Int? = null,
        enableCancelBtn: Boolean = true,
        enableMenu: Boolean = false,
        menuResId: Int = Int.MIN_VALUE,
        menuOnClickListenerBlock: ((menuItem: MenuItem) -> Unit)? = null
    ): Toolbar? {
        return activity.findViewById<Toolbar?>(toolbarResId)?.apply {
            // 设置标题
            findViewById<TextView>(R.id.toolbarTitleText)?.let { titleTextView ->
                titleStr?.let {
                    titleTextView.text = it
                } ?: run {
                    titleResId?.let {
                        titleTextView.setText(it)
                    } ?: run {
                        titleTextView.text = ""
                    }
                }
            }
            // 设置返回事件
            if (enableCancelBtn) {
                navigationIcon =
                    ResourcesCompat.getDrawable(
                        activity.resources,
                        R.drawable.baseline_arrow_back_24,
                        null
                    )
                setNavigationOnClickListener { activity.finish() }
            }
            // 设置菜单
            if (enableMenu && menuResId != Int.MIN_VALUE && menuOnClickListenerBlock != null) {
                inflateMenu(menuResId)
                setOnMenuItemClickListener {
                    menuOnClickListenerBlock(it)
                    true
                }
            }
        }
    }
}

inline fun AlertDialog.Builder.setNegativeBtn(
    context: Context,
    enableNegativeBtn: Boolean = false,
    negativeBtnText: String = "放弃",
    crossinline block: (DialogInterface, Int) -> Unit = { dialogInterface, _ ->
        dialogInterface.dismiss()
        (context as Activity).finish()
    }
): AlertDialog.Builder {
    if (enableNegativeBtn) {
        setNegativeButton(negativeBtnText) { dialogInterface, i ->
            block(dialogInterface, i)
        }
    }
    return this
}

