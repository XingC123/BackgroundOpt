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

package com.venus.backgroundopt.ui.base

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 此[RecyclerView.Adapter]实现了滑动动画
 *
 * [滑动动画代码](https://blog.csdn.net/weixin_47592544/article/details/124089281)
 *
 * @author XingC
 * @date 2024/5/31
 */
abstract class RecyclerViewAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    /* *************************************************************************
     *                                                                         *
     * 滑动动画                                                                  *
     *                                                                         *
     **************************************************************************/
    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        addAnimation(holder)
    }

    private fun addAnimation(holder: VH) {
        for (anim in getAnimators(holder.itemView)) {
            anim.setDuration(200).start()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    fun getAnimators(view: View?): Array<ObjectAnimator> {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        return arrayOf(scaleX, scaleY, alpha)
    }
}