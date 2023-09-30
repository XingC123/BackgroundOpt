package com.venus.backgroundopt.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants.SUB_PROCESS_OOM_POLICY
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.ui.widget.showProgressBarViewForAction
import com.venus.backgroundopt.utils.TMP_DATA
import com.venus.backgroundopt.utils.getAppProcesses
import com.venus.backgroundopt.utils.preference.prefPut
import com.venus.backgroundopt.utils.preference.prefValue
import com.venus.backgroundopt.utils.processNameSeparator

class ConfigureAppProcessActivity : BaseActivity() {
    private lateinit var appItem: AppItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction(this, "正在加载...") {
            init()
        }
    }

    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.configureAppProcessToolBar)
    }

    override fun getContentView(): Int {
        return R.layout.activity_configure_app_process
    }


    private fun init() {
        appItem = TMP_DATA as AppItem

        // 设置基本数据
        runOnUiThread {
            findViewById<ImageView>(R.id.configureAppProcessAppIcon)?.setImageDrawable(appItem.appIcon)
            findViewById<TextView>(R.id.configureAppProcessAppNameText)?.let {
                it.text = appItem.appName
            }
            findViewById<TextView>(R.id.configureAppProcessUidText)?.let {
                it.text = appItem.uid.toString()
            }
            findViewById<TextView>(R.id.configureAppProcessVersionNameText)?.let {
                it.text = appItem.versionName
            }
            findViewById<TextView>(R.id.configureAppProcessVersionCodeText)?.let {
                it.text = appItem.longVersionCode.toString()
            }
        }

        // 获取进程列表
        getAppProcesses(this, appItem)

        // 获取本地配置
        val subProcessOomPolicyList = arrayListOf<SubProcessOomPolicy>()
        val iterator = appItem.processes.iterator()
        var processName: String
        while (iterator.hasNext()) {
            processName = iterator.next()
            // 剔除主进程
            if (!processName.contains(processNameSeparator)) {
                iterator.remove()
                continue
            }
            subProcessOomPolicyList.add(
                prefValue<SubProcessOomPolicy>(SUB_PROCESS_OOM_POLICY, processName) ?: run {
                    // 不存在
                    SubProcessOomPolicy().apply {
                        // 当前进程是否在默认白名单
                        if (CommonProperties.subProcessDefaultUpgradeSet.contains(processName)) {
                            this.policyEnum =
                                SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS

                            // 保存到本地
                            prefPut(SUB_PROCESS_OOM_POLICY, commit = true, processName, this)
                        }
                    }
                }
            )
        }

        // 设置view
        runOnUiThread {
            findViewById<RecyclerView>(R.id.configureAppProcessRecycleView).apply {
                layoutManager = LinearLayoutManager(this@ConfigureAppProcessActivity).apply {
                    orientation = LinearLayoutManager.VERTICAL
                }
                adapter =
                    ConfigureAppProcessAdapter(appItem.processes.toList(), subProcessOomPolicyList)
            }
        }
    }
}