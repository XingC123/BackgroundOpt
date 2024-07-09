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
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.PopupMenu.OnDismissListener
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener
import com.google.android.material.button.MaterialButton
import com.venus.backgroundopt.R

/**
 * 点击按钮可弹出菜单
 *
 * @author XingC
 * @date 2024/7/8
 */
class VenusPopupMenuButton : MaterialButton {
    private lateinit var menu: PopupMenu
    private var _selectedItemResId: Int = Int.MIN_VALUE
    val selectedItemResId: Int get() = _selectedItemResId

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(
        context,
        attrs,
        com.google.android.material.R.attr.materialButtonStyle
    )

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        // 设置自定义属性
        context.obtainStyledAttributes(
            attrs,
            R.styleable.VenusPopMenuButton
        ).use { typedArray ->
            // 添加图标
            icon ?: run {
                setIconResource(R.drawable.baseline_arrow_drop_down_24_dynamic)
                iconGravity = ICON_GRAVITY_END
            }

            // 加载menu
            val menuResId =
                typedArray.getResourceId(R.styleable.VenusPopMenuButton_menu, Int.MIN_VALUE)
            _selectedItemResId = typedArray.getResourceId(
                R.styleable.VenusPopMenuButton_defaultMenuItemResId,
                Int.MIN_VALUE
            )
            setMenu(menuResId = menuResId, menuItemResId = _selectedItemResId)
            setOnMenuItemClickListener(::defaultMenuItemClickAction)
            setOnDismissListener(::defaultDismissAction)
            // 设置点击事件
            this.setOnClickListener {
                showMenu()
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 响应事件                                                                  *
     *                                                                         *
     **************************************************************************/
    fun defaultMenuItemClickAction(item: MenuItem): Boolean {
        _selectedItemResId = item.itemId
        // 设置文本
        text = item.title

        return true
    }

    fun setOnMenuItemClickListener(listener: OnMenuItemClickListener) {
        menu.setOnMenuItemClickListener(listener)
    }

    fun setExtraOnMenuItemClickListener(extraAction: (MenuItem) -> Boolean) {
        menu.setOnMenuItemClickListener { item ->
            // 默认行为
            defaultMenuItemClickAction(item)
            // 附加行为
            extraAction(item)
        }
    }

    fun defaultDismissAction(menu: PopupMenu) {

    }

    fun setOnDismissListener(listener: OnDismissListener) {
        menu.setOnDismissListener(listener)
    }

    fun setExtraOnDismissListener(extraAction: (PopupMenu) -> Unit) {
        menu.setOnDismissListener { menu ->
            // 默认行为
            defaultDismissAction(menu)
            // 附加行为
            extraAction(menu)
        }
    }

    fun setMenu(@MenuRes menuResId: Int = Int.MIN_VALUE, menuItemResId: Int = Int.MIN_VALUE) {
        if (menuResId == Int.MIN_VALUE) {
            return
        }
        val popup = PopupMenu(context!!, this)
        popup.menuInflater.inflate(menuResId, popup.menu)
        // 默认选中项的资源id
        text = popup.menu.findItem(menuItemResId)?.title

        menu = popup
    }

    fun showMenu() {
        menu.show()
    }
}