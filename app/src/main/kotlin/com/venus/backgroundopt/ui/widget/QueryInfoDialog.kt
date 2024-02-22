package com.venus.backgroundopt.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.venus.backgroundopt.R
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.message.sendMessage

/**
 * 查询信息对话框工具类
 *
 * @author XingC
 * @date 2023/9/23
 */
class QueryInfoDialog {
    companion object {
        /* *************************************************************************
         *                                                                         *
         * 预定义方法                                                                *
         *                                                                         *
         **************************************************************************/
        @JvmStatic
        fun createQueryIntDataDialog(context: Context, messageKey: String): AlertDialog {
            return createQueryInfoDialog(context) { alertDialog, _ ->
                // 数据检查
                val dataStr: String? = getQueryData(alertDialog)?.trim()
                if (dataStr.isNullOrBlank()) {
                    return@createQueryInfoDialog
                }

                try {
                    val data = dataStr.toInt()
                    val message = sendMessage(alertDialog.context, messageKey, data)
                    setMessage(alertDialog, message)
                } catch (t: Throwable) {
//                    Toast.makeText(context, "数据不合法", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /* *************************************************************************
         *                                                                         *
         * 创建对话框                                                                *
         *                                                                         *
         **************************************************************************/
        @JvmStatic
        inline fun createQueryInfoDialog(
            context: Context,
            crossinline queryAction: (rootView: View, View) -> Unit
        ): AlertDialog {
            return createQueryInfoDialog(context, null, null, queryAction)
        }

        /**
         * 获取查询对话框
         *
         * @param context 发起者的上下文
         * @param queryInfoTipText 查询的提示信息。null表示默认
         * @param queryInfoEditTextHint 输入框的提示信息。null表示默认
         * @param queryAction 查询动作
         * @return 对话框
         */
        @JvmStatic
        inline fun createQueryInfoDialog(
            context: Context,
            queryInfoTipText: String?,
            queryInfoEditTextHint: String?,
            crossinline queryAction: (rootView: View, View) -> Unit
        ): AlertDialog {
            return UiUtils.createDialog(
                context = context,
                viewResId = R.layout.query_info,
                viewBlock = {
                    // 设置输入框的提示文本
                    queryInfoEditTextHint?.let { str ->
                        findViewById<TextView>(R.id.queryInfoEditText)?.text = str
                    }
                    // 设置按钮点击事件
                    findViewById<Button>(R.id.doQueryBtn)?.setOnClickListener { view ->
                        queryAction(rootView, view)
                    }
                }
            )
        }

        /* *************************************************************************
         *                                                                         *
         * 数据的获取与设置                                                           *
         *                                                                         *
         **************************************************************************/
        /**
         * 获取要查询的数据
         *
         * @param rootView 所在对话框
         * @return 字符串
         */
        @JvmStatic
        fun getQueryData(rootView: View): String? {
            return rootView.findViewById<TextInputEditText>(R.id.queryInfoEditText)?.text?.toString()?.trim()
        }

        /**
         * 设置信息到查询对话框
         *
         * @param rootView 所在对话框
         * @param message 要设置的信息
         */
        @JvmStatic
        fun setMessage(rootView: View, message: String?) {
            rootView.findViewById<TextView>(R.id.queryResultText)?.text = message
        }
    }
}