package com.venus.backgroundopt.ui.widget.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem

/**
 * 尽可能使从[AppItem]中展示信息变得更容易
 *
 * @author XingC
 * @date 2023/9/25
 */
abstract class ShowInfoFromAppItemAdapter(protected open val items: List<AppItem>) :
    RecyclerView.Adapter<ShowInfoFromAppItemAdapter.Companion.ShowInfoFromAppItemViewHolder>() {
    companion object {
        class ShowInfoFromAppItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var appIcon: ImageView
            var appItemTasksText1: TextView
            var appItemTasksText2: TextView
            var appItemTasksText3: TextView

            init {
                appIcon = itemView.findViewById(R.id.appItemAppIcon)
                appItemTasksText1 = itemView.findViewById(R.id.appItemText1)
                appItemTasksText2 = itemView.findViewById(R.id.appItemText2)
                appItemTasksText3 = itemView.findViewById(R.id.appItemText3)
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 提示信息的内容                                                             *
     *                                                                         *
     **************************************************************************/
    protected open fun getTipText1ResId(): Int {
        return R.string.appItemTipPackageName
    }

    protected open fun getTipText2ResId(): Int {
        return R.string.appItemTipPid
    }

    protected open fun getTipText3ResId(): Int {
        return R.string.appItemTipUid
    }

    /* *************************************************************************
     *                                                                         *
     * 具体的内容                                                                *
     *                                                                         *
     **************************************************************************/
    abstract fun getText1Content(appItem: AppItem): String?
    abstract fun getText2Content(appItem: AppItem): String
    abstract fun getText3Content(appItem: AppItem): String

    /* *************************************************************************
     *                                                                         *
     * 布局                                                                     *
     *                                                                         *
     **************************************************************************/
    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowInfoFromAppItemViewHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_app_item, parent, false)
        // 修改tip内容
        view.findViewById<TextView>(R.id.appItemTipText1)
            ?.setText(getTipText1ResId())
        view.findViewById<TextView>(R.id.appItemTipText2)
            ?.setText(getTipText2ResId())
        view.findViewById<TextView>(R.id.appItemTipText3)
            ?.setText(getTipText3ResId())
        return ShowInfoFromAppItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShowInfoFromAppItemViewHolder, position: Int) {
        val appItem = items[position]
        holder.appIcon.setImageDrawable(appItem.appIcon)
        holder.appItemTasksText1.text = getText1Content(appItem)
        holder.appItemTasksText2.text = getText2Content(appItem)
        holder.appItemTasksText3.text = getText3Content(appItem)
    }
}