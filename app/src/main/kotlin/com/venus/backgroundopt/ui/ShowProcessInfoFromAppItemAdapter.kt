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

package com.venus.backgroundopt.ui

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemViewHolder
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.handle.ProcessRunningInfoMessageHandler.ProcessRunningInfo
import com.venus.backgroundopt.utils.message.messageSender
import com.venus.backgroundopt.utils.showProgressBarViewForAction

/**
 * 尽可能使从[AppItem]中展示信息变得更容易
 *
 * @author XingC
 * @date 2023/9/25
 */
abstract class ShowProcessInfoFromAppItemAdapter(
    protected val activity: Activity,
    protected val appItems: MutableList<AppItem>
) : ShowInfoFromAppItemAdapter() {
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

    protected open fun getTipText4ResId(): Int {
        return R.string.appItemTipOomAdjScore
    }

    /* *************************************************************************
     *                                                                         *
     * 具体的内容                                                                *
     *                                                                         *
     **************************************************************************/
    open fun getText0Content(appItem: AppItem): String = appItem.appName
    abstract fun getText1Content(appItem: AppItem): String?
    abstract fun getText2Content(appItem: AppItem): String
    abstract fun getText3Content(appItem: AppItem): String
    abstract fun getText4Content(appItem: AppItem): String

    /* *************************************************************************
     *                                                                         *
     * 布局                                                                     *
     *                                                                         *
     **************************************************************************/
    // 过滤后的列表。用于搜索功能
    var filterAppItems: MutableList<AppItem> = appItems

    open fun getViewHolder(view: View): ShowProcessInfoFromAppItemViewHolder {
        return ShowProcessInfoFromAppItemViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filterAppItems.size
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowProcessInfoFromAppItemViewHolder {
        val view = doOnCreateViewHolder(parent, viewType)
        return getViewHolder(view)
    }

    protected fun doOnCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): View {
        return LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.recycler_app_item,
                parent,
                false
            ).also { view ->
                // 修改tip内容
                view.findViewById<TextView>(R.id.appName)
                    ?.setText(getTipText1ResId())
                view.findViewById<TextView>(R.id.appItemTipText2)
                    ?.setText(getTipText2ResId())
                view.findViewById<TextView>(R.id.appItemTipText3)
                    ?.setText(getTipText3ResId())
                view.findViewById<TextView>(R.id.appItemTipText4)
                    ?.setText(getTipText4ResId())
            }
    }

    override fun onBindViewHolder(holder: ShowInfoFromAppItemViewHolder, position: Int) {
        holder as ShowProcessInfoFromAppItemViewHolder
        val appItem = filterAppItems[position]
        holder.appIcon.setImageDrawable(appItem.appIcon)
        holder.appNameText.text = getText0Content(appItem)
        holder.processNameText.text = getText1Content(appItem)
        holder.appItemTasksText2.text = getText2Content(appItem)
        holder.appItemTasksText3.text = getText3Content(appItem)
        holder.appItemTasksText4.text = getText4Content(appItem)

        // 设置点击事件
        setItemOnClickListener(
            view = holder.itemView,
            appItem = appItem,
            adjComponent = holder.appItemTasksText4
        )
    }

    private fun setItemOnClickListener(view: View, appItem: AppItem, adjComponent: TextView) {
        view.setOnClickListener {
            view.context.showProgressBarViewForAction(text = "正在获取...") {
                val processRunningInfo = messageSender.send(
                    type = ProcessRunningInfo::class.java,
                    key = MessageKeyConstants.getProcessRunningInfo,
                    value = appItem.pid
                ) ?: ProcessRunningInfo.singleton
                appItem.rssInBytes = processRunningInfo.rssInBytes
                appItem.curAdj = processRunningInfo.adj
                appItem.oomAdjScore = processRunningInfo.originalAdj

                activity.runOnUiThread {
                    UiUtils.createDialog(
                        context = view.context,
                        cancelable = false,
                        viewResId = R.layout.content_process_info_dialog,
                        viewBlock = {
                            // 设置app名字
                            findViewById<TextView>(R.id.appNameText)?.text = appItem.appName
                            // 设置进程名
                            findViewById<TextView>(
                                R.id.processNameText
                            )?.text = appItem.fullQualifiedProcessName
                            // 设置包名
                            findViewById<TextView>(R.id.packageNameText)?.text = appItem.packageName
                            // 设置pid
                            findViewById<TextView>(R.id.pidText)?.text = appItem.pid.toString()
                            // 设置uid
                            findViewById<TextView>(R.id.uidText)?.text = appItem.uid.toString()
                            // 设置资源占用大小
                            findViewById<TextView>(
                                R.id.rssText
                            )?.text = BaseProcessInfoKt.getRssUiText(appItem.rssInBytes)
                            // 设置原始adj
                            findViewById<TextView>(
                                R.id.originalAdjText
                            )?.text = appItem.oomAdjScore.toString()
                            // 设置当前adj
                            findViewById<TextView>(R.id.curAdjText)?.text = appItem.curAdj.toString()
                        },
                        titleStr = "进程信息",
                        enablePositiveBtn = true,
                        positiveBlock = { _: DialogInterface, _: Int ->
                            // 更新当前的adj
                            adjComponent.text = getText4Content(appItem)
                        }
                    ).show()
                }
            }
        }
    }

    open class ShowProcessInfoFromAppItemViewHolder(itemView: View) :
        ShowInfoFromAppItemViewHolder(itemView) {
        var appIcon: ImageView
        var processNameText: TextView
        var appNameText: TextView
        var appItemTasksText2: TextView
        var appItemTasksText3: TextView
        var appItemTasksText4: TextView

        init {
            appIcon = itemView.findViewById(R.id.appItemAppIcon)
            appNameText = itemView.findViewById(R.id.appName)
            processNameText = itemView.findViewById(R.id.processName)
            appItemTasksText2 = itemView.findViewById(R.id.appItemText2)
            appItemTasksText3 = itemView.findViewById(R.id.appItemText3)
            appItemTasksText4 = itemView.findViewById(R.id.appItemText4)
        }
    }
}
