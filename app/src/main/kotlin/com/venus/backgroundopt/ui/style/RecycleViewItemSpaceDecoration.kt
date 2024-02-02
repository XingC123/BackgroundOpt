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