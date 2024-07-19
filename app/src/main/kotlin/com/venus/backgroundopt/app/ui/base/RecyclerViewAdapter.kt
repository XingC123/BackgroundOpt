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

package com.venus.backgroundopt.app.ui.base

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.common.util.ne
import java.util.concurrent.ConcurrentHashMap

/**
 * 此[RecyclerView.Adapter]实现了滑动动画
 *
 * [滑动动画代码](https://blog.csdn.net/weixin_47592544/article/details/124089281)
 *
 * @author XingC
 * @date 2024/5/31
 */
abstract class RecyclerViewAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)

        addScrollAnimationMethod(holder)
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)

        removeShowingViewHolderMethod(holder)
    }

    /* *************************************************************************
     *                                                                         *
     * 列表滑动动画                                                              *
     *                                                                         *
     **************************************************************************/
    // 正在显示的ViewHolder
    private val showingViewHoldersMap = ConcurrentHashMap<Int, VH>(2)

    // 是否启用滑动动画
    var enableScrollAnimation: Boolean = true
        set(value) {
            field = value

            if (value) {
                addScrollAnimationMethod = ::handleAddScrollAnimation
                removeShowingViewHolderMethod = ::handleRemoveShowingViewHolder
            } else {
                addScrollAnimationMethod = ::viewHolderConsumer
                removeShowingViewHolderMethod = ::viewHolderConsumer
            }
        }
    private var addScrollAnimationMethod: (VH) -> Unit = ::handleAddScrollAnimation
    private var removeShowingViewHolderMethod: (VH) -> Unit = ::handleRemoveShowingViewHolder

    /**
     * 滑动[RecyclerView]时, item进入视野之前, [showingViewHoldersMap].get(position)返回的是null, 此时应该添加动画。
     *
     * 执行[notifyItemRangeChanged]以后:
     * 1) [onViewAttachedToWindow]内的日志(多条日志):
     *     - vh: ShowAllInstalledAppsViewHolder{4da7948 position=23 id=-1, oldPos=-1, pLpos:-1 scrap [changeScrap] update tmpDetached no parent}, adapterPosition: 23, needsUpdate: true
     *     - vh: ShowAllInstalledAppsViewHolder{261367b position=23 id=-1, oldPos=-1, pLpos:-1}, adapterPosition: 23, needsUpdate: false
     *
     *     综上: 只在[showingViewHoldersMap].get(position)为null时再添加动画
     * 2) [onViewDetachedFromWindow]内的日志:
     *     -  vh: ShowAllInstalledAppsViewHolder{4da7948 position=23 id=-1, oldPos=-1, pLpos:-1 update}
     *
     * @param holder VH
     */
    private fun handleAddScrollAnimation(holder: VH) {
        val adapterPosition = holder.adapterPosition
        val lastVh = showingViewHoldersMap[adapterPosition]
        /*
         * 当前holder未处于显示状态, 则添加动画。
         * 详细逻辑请看handleAddAnimation的文档注释
         */
        lastVh ?: addAnimation(holder)
        lastVh.ne(holder) {
            showingViewHoldersMap[adapterPosition] = holder
        }
    }

    private fun handleRemoveShowingViewHolder(holder: VH) {
        val adapterPosition = holder.adapterPosition
        // 只有map内的holder和当前的一样, 才进行移除
        if (showingViewHoldersMap[adapterPosition] == holder) {
            showingViewHoldersMap.remove(adapterPosition)
        }
    }

    private fun addAnimation(holder: VH) {
        for (anim in getAnimators(holder.itemView)) {
            anim.setDuration(200).start()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun getAnimators(view: View?): Array<ObjectAnimator> {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        return arrayOf(scaleX, scaleY, alpha)
    }

    companion object {
        @JvmStatic
        fun viewHolderConsumer(holder: RecyclerView.ViewHolder) {
            // do nothing
        }
    }
}