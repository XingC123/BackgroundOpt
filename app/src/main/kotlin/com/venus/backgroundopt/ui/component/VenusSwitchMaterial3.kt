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
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.venus.backgroundopt.R
import com.venus.backgroundopt.utils.setTextSizeSP


/**
 * @author XingC
 * @date 2024/4/19
 */
class VenusSwitchMaterial3 : LinearLayout {
    private lateinit var switch: MaterialSwitch
    private lateinit var button: MaterialButton

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
        inflate(context, R.layout.component_switch, this)

        // 初始化控件
        switch = findViewById(R.id.switch_component)
        button = findViewById(R.id.button_component)

        // 设置自定义属性
        context.obtainStyledAttributes(
            attrs,
            R.styleable.VenusSwitchMaterial3
        ).use { typedArray ->
            /*
                switch
             */
            typedArray.getString(R.styleable.VenusSwitchMaterial3_switchText)?.let {
                switch.text = it
            }
            typedArray.getString(R.styleable.VenusSwitchMaterial3_switchTextOn)?.let {
                switch.textOn = it
            }
            typedArray.getString(R.styleable.VenusSwitchMaterial3_switchTextOff)?.let {
                switch.textOff = it
            }

            switch.isChecked =
                typedArray.getBoolean(R.styleable.VenusSwitchMaterial3_switchChecked, false)
            switch.setTextSizeSP(typedArray = typedArray, R.styleable.VenusSwitchMaterial3_switchTextSize, 16f)

            /*
                button
             */
            typedArray.getString(R.styleable.VenusSwitchMaterial3_buttonText)?.let {
                button.text = it
            }
            switch.setTextSizeSP(typedArray = typedArray, R.styleable.VenusSwitchMaterial3_buttonTextSize, 16f)
            typedArray.getDrawable(R.styleable.VenusSwitchMaterial3_buttonIcon)?.let {
                button.icon = it
            }
            button.visibility =
                if (typedArray.getBoolean(R.styleable.VenusSwitchMaterial3_buttonVisible, false)) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    var isChecked: Boolean
        get() = switch.isChecked
        set(value) {
            switch.isChecked = value
        }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        switch.setOnCheckedChangeListener(listener)
    }

    fun setButtonOnClickedListener(listener: OnClickListener) {
        button.setOnClickListener(listener)
    }
}