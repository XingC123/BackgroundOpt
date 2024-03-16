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
                    
 package com.venus.backgroundopt.ui.style

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R

/**
 * [RecyclerView]中item的边距
 * 此类仅设置上下边距
 *
 * @author XingC
 * @date 2024/2/2
 */
class RecycleViewItemSpaceDecoration() : RecyclerView.ItemDecoration() {
    private var itemSpace: Float = 5F

    constructor(itemSpace: Float) : this() {
        this.itemSpace = itemSpace
    }

    constructor(context: Context) : this() {
        this.itemSpace = context.resources.getDimension(R.dimen.item_space)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val int = itemSpace.toInt()

        outRect.top = int
        outRect.bottom = int
    }
}