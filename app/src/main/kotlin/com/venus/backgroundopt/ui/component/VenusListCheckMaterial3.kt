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

package com.venus.backgroundopt.ui.component

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.preference.ListPreference
import com.google.android.material.radiobutton.MaterialRadioButton
import com.venus.backgroundopt.R
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.setTextSizeSP

/**
 * 类似于[ListPreference]
 *
 * @author XingC
 * @date 2024/4/27
 */
class VenusListCheckMaterial3 : LinearLayout {
    lateinit var title: TextView
    lateinit var summary: TextView
    var entries: Array<CharSequence>? = null
        set(value) {
            field = value

            // 重计算summary
            summary.text = this.value ?: value?.get(0)
        }
    var entryValues: Array<CharSequence>? = null

    private var dialogConfirmClickListener: DialogConfirmClickListener? = null

    var value: CharSequence? = null
        set(value) {
            field = value

            summary.text = value ?: "null"
        }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context = context, attrs = attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        // 加载自定义布局
        inflate(context, R.layout.component_list_check, this)

        // 控件初始化
        title = findViewById(R.id.title)
        summary = findViewById(R.id.summary)

        // 设置自定义属性
        context.obtainStyledAttributes(
            attrs,
            R.styleable.VenusListCheckMaterial3
        ).use { typedArray ->
            /*
             * 弹窗显示的选项
             */
            entries = typedArray.getTextArray(R.styleable.VenusListCheckMaterial3_entries)
            entryValues = typedArray.getTextArray(R.styleable.VenusListCheckMaterial3_entryValues)

            /*
             * title
             */
            typedArray.getString(R.styleable.VenusListCheckMaterial3_title)?.let {
                title.text = it
            }
            title.setTextSizeSP(
                typedArray = typedArray,
                index = R.styleable.VenusListCheckMaterial3_titleTextSize,
                defValue = 16f
            )

            /*
             * summary
             */
            typedArray.getString(R.styleable.VenusListCheckMaterial3_defaultValue)?.let {
                summary.text = it
                value = it
            } ?: run {
                summary.text = typedArray.getString(
                    R.styleable.VenusListCheckMaterial3_summary
                ) ?: entries?.get(0) ?: "summary"
            }
            summary.setTextSizeSP(
                typedArray = typedArray,
                index = R.styleable.VenusListCheckMaterial3_summaryTextSize,
                defValue = 12f
            )

            // 点击事件
            setOnClickListener(::showChooseDialog)
        }
    }

    private fun showChooseDialog(parent: View) {
        var configRadioGroup: RadioGroup? = null
        UiUtils.createDialog(
            context = parent.context,
            viewResId = R.layout.component_list_check_dialog,
            viewBlock = {
                val view = this
                val context = view.context

                configRadioGroup = view.findViewById<RadioGroup>(
                    R.id.radio_group
                )?.let { radioGroup ->
                    // 添加单选按钮
                    entries?.forEachIndexed { index, title ->
                        val radioButton = MaterialRadioButton(context).apply {
                            id = index
                            text = title

                            if (title == value) {
                                isChecked = true
                            }
                        }
                        radioGroup.addView(radioButton)
                    }

                    radioGroup
                }
            },
            cancelable = false,
            enablePositiveBtn = true,
            positiveBlock = { dialogInterface: DialogInterface, i: Int ->
                configRadioGroup?.let { radioGroup ->
                    val index = radioGroup.checkedRadioButtonId
                    val entry = entries?.get(index)
                    val entryValue = entryValues?.get(index)
                    dialogConfirmClickListener?.onClick(index, entry, entryValue)

                    // 更改summary
                    summary.text = entry ?: "null"

                    value = entry

                    dialogInterface.dismiss()
                }
            },
            enableNegativeBtn = true,
        ).show()
    }

    fun setDialogConfirmClickListener(listener: DialogConfirmClickListener) {
        dialogConfirmClickListener = listener
    }

    fun interface DialogConfirmClickListener {
        fun onClick(id: Int, entry: CharSequence?, entryValue: CharSequence?)
    }
}