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

package com.venus.backgroundopt.app.ui.component

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.ListPreference
import com.venus.backgroundopt.R
import com.venus.backgroundopt.app.utils.obtainStyledAttributesAutoClose
import com.venus.backgroundopt.app.utils.setTextSizeSP

/**
 * 未点击时与[ListPreference]相同
 *
 * @author XingC
 * @date 2024/7/24
 */
class VenusListPreference : LinearLayout {
    lateinit var titleTextView: TextView
    lateinit var summaryTextView: TextView

    var title: CharSequence?
        get() = titleTextView.text
        set(value) {
            titleTextView.text = value
        }
    var summary: CharSequence?
        get() = summaryTextView.text
        set(value) {
            summaryTextView.text = value
        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context = context, attrs = attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        // 加载自定义布局
        inflate(context, R.layout.component_list_check, this)

        // 控件初始化
        titleTextView = findViewById(R.id.title)
        summaryTextView = findViewById(R.id.summary)

        // 设置自定义属性
        context.obtainStyledAttributesAutoClose(
            attrs,
            R.styleable.VenusListPreference
        ) { typedArray ->
            /*
             * title
             */
            title = typedArray.getString(R.styleable.VenusListPreference_title) ?: "null"
            titleTextView.setTextSizeSP(
                typedArray = typedArray,
                index = R.styleable.VenusListPreference_titleTextSize,
                defValue = 16f
            )

            /*
             * summary
             */
            summary = typedArray.getString(R.styleable.VenusListPreference_summary) ?: "null"
            summaryTextView.setTextSizeSP(
                typedArray = typedArray,
                index = R.styleable.VenusListPreference_summaryTextSize,
                defValue = 12f
            )
        }
    }
}