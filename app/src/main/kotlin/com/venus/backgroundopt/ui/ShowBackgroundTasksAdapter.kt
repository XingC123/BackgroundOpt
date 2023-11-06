package com.venus.backgroundopt.ui

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.ui.base.ShowInfoFromAppItemAdapter
import com.venus.backgroundopt.utils.message.handle.BackgroundTasksMessageHandler
import com.venus.backgroundopt.utils.preference.pref

/**
 * @author XingC
 * @date 2023/9/23
 */
class ShowBackgroundTasksAdapter(
    override val items: List<AppItem>,
    private val backgroundTaskMessage: BackgroundTasksMessageHandler.BackgroundTaskMessage
) : ShowInfoFromAppItemAdapter(items) {
    private var enableForegroundProcTrimMem = false

    override fun getText1Content(appItem: AppItem): String? {
        return appItem.processName
    }

    override fun getText2Content(appItem: AppItem): String {
        return appItem.pid.toString()
    }

    override fun getText3Content(appItem: AppItem): String {
        return appItem.uid.toString()
    }

    override fun getText4Content(appItem: AppItem): String {
        return appItem.curAdj.toString()
    }

    override fun getTipText1ResId(): Int {
        return R.string.appItemTipProcessName
    }

    override fun getViewHolder(view: View): ShowInfoFromAppItemViewHolder {
        return ShowBackgroundTasksViewHolder(view)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShowInfoFromAppItemViewHolder {
        return super.onCreateViewHolder(parent, viewType).apply {
            // 上次运行结果
            itemView.findViewById<LinearLayout>(R.id.appItemLastProcessingResultLayout)?.let {
                it.visibility = View.GONE
            }
            // 设置前台可见性
            enableForegroundProcTrimMem =
                this.itemView.context.pref(PreferenceNameConstants.MAIN_SETTINGS).getBoolean(
                    PreferenceKeyConstants.ENABLE_FOREGROUND_PROC_TRIM_MEM_POLICY,
                    false
                )
        }
    }

    override fun onBindViewHolder(holder: ShowInfoFromAppItemViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val appItem = items[position]
        val packageName = appItem.packageName
        val viewHolder = holder as ShowBackgroundTasksViewHolder

        var enablePolicyForegroundTrim = false
        var enablePolicyBackgroundTrim = true
        var enablePolicyBackgroundGc = true

        backgroundTaskMessage.appOptimizePolicyMap[packageName]?.let { appOptimizePolicy ->
            enablePolicyForegroundTrim = !appOptimizePolicy.disableForegroundTrimMem
            enablePolicyBackgroundTrim = !appOptimizePolicy.disableBackgroundTrimMem
            enablePolicyBackgroundGc = !appOptimizePolicy.disableBackgroundGc
        }

        // 设置提示文字的可见性
        if (!enableForegroundProcTrimMem) {
            setAppOptimizePolicyTextVisible(viewHolder.appItemForegroundTrimMemText, false)
        } else {
            setAppOptimizePolicyTextVisible(
                viewHolder.appItemForegroundTrimMemText,
                enablePolicyForegroundTrim
            )
        }
        setAppOptimizePolicyTextVisible(
            viewHolder.appItemBackgroundTrimMemText,
            enablePolicyBackgroundTrim
        )
        setAppOptimizePolicyTextVisible(
            viewHolder.appItemBackgroundGcText,
            enablePolicyBackgroundGc
        )
    }

    private fun setAppOptimizePolicyTextVisible(textView: TextView, isVisible: Boolean) {
        if (isVisible) {
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    class ShowBackgroundTasksViewHolder(itemView: View) : ShowInfoFromAppItemViewHolder(itemView) {
        val appItemForegroundTrimMemText: TextView
        val appItemBackgroundTrimMemText: TextView
        val appItemBackgroundGcText: TextView

        init {
            appItemForegroundTrimMemText = itemView.findViewById(R.id.appItemForegroundTrimMemText)
            appItemBackgroundTrimMemText = itemView.findViewById(R.id.appItemBackgroundTrimMemText)
            appItemBackgroundGcText = itemView.findViewById(R.id.appItemBackgroundGcText)
        }
    }
}