/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.venus.backgroundopt.app.utils

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.StyleableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import com.alibaba.fastjson2.JSON
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.R
import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.common.util.concurrent.newThreadTask
import com.venus.backgroundopt.common.util.ifTrue
import com.venus.backgroundopt.common.util.log.logErrorAndroid
import java.lang.ref.SoftReference

/**
 * @author XingC
 * @date 2023/9/25
 */

// 临时数据。在两个Activity之间使用。有线程安全问题
private var tmpData: SoftReference<Any>? = null

fun setTmpData(any: Any?) {
    tmpData = SoftReference(any)
}

fun getTmpData(): Any? {
    val o = tmpData?.get()
    tmpData = null
    return o
}

private var tmpListData: ArrayList<Any>? = null

fun setTmpListData(vararg value: Any) {
    tmpListData = arrayListOf(*value)
}

fun getTmpListData(): ArrayList<Any>? {
    val list = tmpListData
    tmpListData = null

    return list
}

@Suppress("UNCHECKED_CAST")
fun <E> getTmpListData(index: Int): E? {
    return tmpListData?.get(index) as? E
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

fun Context.getView(
    layoutResId: Int,
    parent: ViewGroup? = null,
    attachToRoot: Boolean = false,
): View = LayoutInflater.from(this).inflate(layoutResId, parent, attachToRoot)

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
     * @param context Context 当前上下文
     * @param viewResId Int 布局文件资源id
     * @param viewBlock Function1<View, Unit> 要对布局文件创建的[View]执行的操作
     * @param titleResId 对话框标题的内容的资源文件id
     * @param titleStr 对话框的标题内容
     * @param cancelable Boolean 点击空白处是否可以取消对话框
     * @param enableNegativeBtn Boolean 启用返回按钮
     * @param negativeBtnText String 返回按钮文字
     * @param negativeBlock Function2<DialogInterface, Int, Unit> 返回按钮执行的操作
     * @param enablePositiveBtn Boolean 启用确认按钮
     * @param positiveBtnText String 确认按钮文本
     * @param positiveBlock Function2<DialogInterface, Int, Unit> 确认按钮执行的操作
     * @return AlertDialog
     */
    @JvmOverloads
    fun createDialog(
        context: Context,
        text: String? = null,
        @StringRes textResId: Int? = null,
        viewResId: Int? = null,
        viewBlock: (View.() -> Unit)? = null,
        useDialogPreferredPaddingHorizontal: Boolean = true,
        titleResId: Int? = null,
        titleStr: String? = null,
        icon: Drawable? = null,
        iconResId: Int? = null,
        cancelable: Boolean = true,
        autoDismissAfterClickButton: Boolean = true,
        enableNegativeBtn: Boolean = false,
        negativeBtnText: String = "放弃",
        negativeBlock: (DialogInterface, Int) -> Unit = { dialogInterface, _ ->
            dialogInterface.dismiss()
        },
        enablePositiveBtn: Boolean = false,
        positiveBtnText: String = "确认",
        positiveBlock: (DialogInterface, Int) -> Unit = { dialogInterface, _ ->
            dialogInterface.dismiss()
        },
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context)
        titleResId?.let { builder.setTitle(it) } ?: titleStr?.let { builder.setTitle(it) }
        iconResId?.let { builder.setIcon(it) } ?: icon?.let { builder.setIcon(it) }

        // 默认的对话框布局
        if (viewResId == null) {
            // 设置文本内容
            text?.let { builder.setMessage(it) } ?: textResId?.let { builder.setMessage(it) }
        } else {
            val view = context.getView(viewResId)
            // 通用对话框布局
            if (viewResId == R.layout.content_common_dailog_view) {
                // 设置文本内容
                text?.let {
                    view.findViewById<TextView>(R.id.contentCommonDialogText)?.text = text
                }
            }
            // 使用主题定义的对话框水平内边距
            useDialogPreferredPaddingHorizontal.ifTrue {
                val outValue = TypedValue()
                val theme = context.theme
                theme.resolveAttribute(
                    android.R.attr.dialogPreferredPadding,
                    outValue,
                    true
                )
                val dialogPreferredPadding = view.resources.getDimensionPixelSize(
                    outValue.resourceId
                )
                val originalPaddingTop = view.paddingTop
                val originalPaddingBottom = view.paddingBottom
                view.setPadding(
                    dialogPreferredPadding,
                    originalPaddingTop,
                    dialogPreferredPadding,
                    originalPaddingBottom
                )
            }
            // 应用对view的自定义操作
            viewBlock?.invoke(view)

            builder.setView(view)
        }

        return builder
            .setCancelable(cancelable)
            .setNegativeBtn(
                context,
                enableNegativeBtn,
                negativeBtnText,
                block = if (autoDismissAfterClickButton) negativeBlock else null
            )
            .setPositiveBtn(
                context,
                enablePositiveBtn,
                positiveBtnText,
                if (autoDismissAfterClickButton) positiveBlock else null
            )
            .create().apply {
                if (!autoDismissAfterClickButton) {
                    setOnShowListener { dialogInterface ->
                        /*
                         * 在此处设置点击事件以覆盖掉原生设置, 其会导致点击按钮后自动关闭对话框
                         */
                        setNegativeBtn(
                            dialog = this,
                            dialogInterface = dialogInterface,
                            enableNegativeBtn = enableNegativeBtn,
                            negativeBtnText = negativeBtnText,
                            block = negativeBlock
                        )
                        setPositiveBtn(
                            dialog = this,
                            dialogInterface = dialogInterface,
                            enablePositiveBtn = enablePositiveBtn,
                            positiveBtnText = positiveBtnText,
                            block = positiveBlock
                        )
                    }
                }
            }
    }

    fun createExceptionDialog(
        context: Context,
        throwable: Throwable,
        titleStr: String? = "错误",
        @StringRes titleResId: Int? = null,
        text: String = throwable.stackTraceToString(),
    ): AlertDialog {
        return createDialog(
            context = context,
            titleStr = titleStr,
            titleResId = titleResId,
            text = text,
            enableNegativeBtn = true,
            enablePositiveBtn = true,
            positiveBtnText = "复制错误信息",
            positiveBlock = { _: DialogInterface, _: Int ->
                // 将错误信息写入剪切板
                val clipboardManager = SystemServices.getClipboardManager(context)
                val clipData = ClipData.newPlainText(
                    BuildConfig.APPLICATION_ID,
                    text
                )
                clipboardManager.setPrimaryClip(clipData)
            }
        )
    }

    @JvmStatic
    fun createProgressBarView(
        context: Context,
        text: String? = null,
        textResId: Int? = R.string.loading,
        cancelable: Boolean = false,
        enableNegativeBtn: Boolean = true,
        negativeBtnText: String = "放弃",
    ): AlertDialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress_bar, null)

        view.findViewById<TextView>(R.id.progressBarText)?.let { bar ->
            text?.let { bar.text = text } ?: textResId?.let { bar.setText(textResId) }
        }

        return MaterialAlertDialogBuilder(context)
            .setCancelable(cancelable)
            .setView(view)
            .setNegativeBtn(
                context,
                enableNegativeBtn,
                negativeBtnText
            )
            .create()
    }

    @JvmStatic
    fun showProgressBarViewForAction(
        context: Context,
        text: String? = null,
        textResId: Int? = R.string.loading,
        cancelable: Boolean = false,
        enableNegativeBtn: Boolean = true,
        negativeBtnText: String = "放弃",
        action: () -> Unit,
    ) {
        val dialog = createProgressBarView(
            context = context,
            text = text,
            textResId = textResId,
            cancelable = cancelable,
            enableNegativeBtn = enableNegativeBtn,
            negativeBtnText = negativeBtnText
        )
        dialog.show()
        newThreadTask {
            runCatching {
                action()
            }.onFailure {
                logErrorAndroid(
                    logStr = "showProgressBarViewForAction: 进度条事件执行出错",
                    t = it
                )
                (context as Activity).runOnUiThread {
                    createExceptionDialog(
                        context = context,
                        throwable = it,
                        titleStr = "加载出错",
                    ).show()
                }
            }
            dialog.dismiss()
        }
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
        menuOnClickListenerBlock: ((menuItem: MenuItem) -> Unit)? = null,
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

    /**
     * 设置组件的可见性
     * 当不可见时, 会从布局空间中移除(不是移除控件)
     * @param component View 要设置的组件
     * @param isVisible Boolean 可见性
     */
    fun setComponentVisible(component: View, isVisible: Boolean) {
        if (isVisible) {
            component.visibility = View.VISIBLE
        } else {
            component.visibility = View.GONE
        }
    }

    fun dpToPx(dp: Float, context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}

internal fun AlertDialog.Builder.setNegativeBtn(
    context: Context,
    enableNegativeBtn: Boolean = false,
    negativeBtnText: String = "放弃",
    block: ((DialogInterface, Int) -> Unit)? = { dialogInterface, _ ->
        dialogInterface.dismiss()
        (context as Activity).finish()
    },
): AlertDialog.Builder {
    if (enableNegativeBtn) {
        setNegativeButton(negativeBtnText, block)
    }
    return this
}

internal fun AlertDialog.Builder.setPositiveBtn(
    context: Context,
    enablePositiveBtn: Boolean = false,
    positiveBtnText: String = "确认",
    block: ((DialogInterface, Int) -> Unit)? = { dialogInterface, _ ->
        dialogInterface.dismiss()
        (context as Activity).finish()
    },
): AlertDialog.Builder {
    if (enablePositiveBtn) {
        setPositiveButton(positiveBtnText, block)
    }
    return this
}

internal inline fun AlertDialog.setNegativeBtn(
    dialog: AlertDialog,
    dialogInterface: DialogInterface,
    enableNegativeBtn: Boolean = false,
    negativeBtnText: String = "放弃",
    crossinline block: (DialogInterface, Int) -> Unit = { _, _ ->
        dialogInterface.dismiss()
    },
): AlertDialog {
    if (enableNegativeBtn) {
        dialog.findViewById<Button>(android.R.id.button2)?.let { button ->
            button.text = negativeBtnText
            button.setOnClickListener {
                block(dialogInterface, android.R.id.button2)
            }
        }
    }
    return this
}

internal inline fun AlertDialog.setPositiveBtn(
    dialog: AlertDialog,
    dialogInterface: DialogInterface,
    enablePositiveBtn: Boolean = false,
    positiveBtnText: String = "确认",
    crossinline block: (DialogInterface, Int) -> Unit = { _, _ ->
        dialogInterface.dismiss()
    },
): AlertDialog {
    if (enablePositiveBtn) {
        dialog.findViewById<Button>(android.R.id.button1)?.let { button ->
            button.text = positiveBtnText
            button.setOnClickListener {
                block(dialogInterface, android.R.id.button1)
            }
        }
    }
    return this
}

fun Context.createExceptionDialog(
    throwable: Throwable,
    titleStr: String? = "错误",
    titleResId: Int? = null,
    text: String = throwable.stackTraceToString(),
): AlertDialog {
    return UiUtils.createExceptionDialog(
        context = this,
        throwable = throwable,
        titleStr = titleStr,
        titleResId =
        titleResId,
        text = text
    )
}

fun Context.showProgressBarViewForAction(
    text: String? = null,
    textResId: Int? = R.string.loading,
    cancelable: Boolean = false,
    enableNegativeBtn: Boolean = true,
    negativeBtnText: String = "放弃",
    action: () -> Unit,
) {
    UiUtils.showProgressBarViewForAction(
        context = this,
        text = text,
        textResId = textResId,
        cancelable = cancelable,
        enableNegativeBtn = enableNegativeBtn,
        negativeBtnText = negativeBtnText,
        action = action
    )
}

inline fun Context.obtainStyledAttributesAutoClose(set: AttributeSet?, @StyleableRes attrs: IntArray, block: (TypedArray)->Unit) {
    val typedArray = obtainStyledAttributes(set, attrs)
    if (OsUtils.isR) {
        block(typedArray)
        typedArray.close()
    } else {
        typedArray.use(block)
    }
}

/* *************************************************************************
 *                                                                         *
 * 文本                                                                     *
 *                                                                         *
 **************************************************************************/
fun TextView.setTextSizeSP(typedArray: TypedArray, index: Int, defValue: Float = 16f) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, typedArray.getDimension(index, defValue))
}