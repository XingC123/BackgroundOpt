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

package com.venus.backgroundopt.app.ui.widget

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.utils.UiUtils
import com.venus.backgroundopt.common.util.message.sendMessage

/**
 * 查询信息对话框工具类
 *
 * @author XingC
 * @date 2023/9/23
 */
object QueryInfoDialog {
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
        crossinline queryAction: (rootView: View, View) -> Unit,
    ): AlertDialog {
        return createQueryInfoDialog(
            context = context,
            queryInfoEditTextHint = null,
            queryAction = queryAction
        )
    }

    /**
     * 获取查询对话框
     *
     * @param context 发起者的上下文
     * @param queryInfoEditTextHint 输入框的提示信息。null表示默认
     * @param queryAction 查询动作
     * @return 对话框
     */
    @JvmStatic
    inline fun createQueryInfoDialog(
        context: Context,
        queryInfoEditTextHint: String?,
        crossinline queryAction: (rootView: View, View) -> Unit,
    ): AlertDialog {
        return UiUtils.createDialog(
            context = context,
            viewResId = R.layout.query_info,
            viewBlock = {
                val queryInfoEditText = findViewById<TextView>(R.id.queryInfoEditText)
                // 设置输入框的提示文本
                queryInfoEditTextHint?.let { str ->
                    queryInfoEditText.hint = str
                } ?: queryInfoEditText.setHint(R.string.queryInfoEditTextHint)
                // 输入内容的长度
                queryInfoEditText.maxEms = 10
                // 输入内容的类型
                queryInfoEditText.inputType = InputType.TYPE_CLASS_NUMBER
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
        return rootView.findViewById<TextInputEditText>(R.id.queryInfoEditText)?.text?.toString()
            ?.trim()
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