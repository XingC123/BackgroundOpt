package com.venus.backgroundopt.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants.SUB_PROCESS_OOM_POLICY
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.StringUtils
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.getTmpData
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy
import com.venus.backgroundopt.utils.message.sendMessage
import com.venus.backgroundopt.utils.preference.prefPut
import com.venus.backgroundopt.utils.preference.prefValue
import com.venus.backgroundopt.utils.showProgressBarViewForAction
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class ConfigureAppProcessActivity : BaseActivity() {
    private lateinit var appItem: AppItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction("正在加载...") {
            init()
        }
    }

    override fun initToolBar(): Toolbar? {
        return UiUtils.getToolbar(this, R.id.toolbarLeftTitleToolbar, titleStr = "应用进程")
    }

    override fun getToolBarMenusResId(): Int {
        return R.menu.menu_configure_app_proc_toolbar
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.configureAppProcAcitivityToolBarHelpMenuItem -> {
                UiUtils.createDialog(
                    this, R.layout.content_configure_app_proc_toolbar_help
                ).show()
            }
        }
    }

    override fun getContentView(): Int {
        return R.layout.activity_configure_app_process
    }


    private fun init() {
        appItem = getTmpData() as AppItem

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

        runOnUiThread {
            /*
                app优化策略
             */
            val curPackageName = appItem.packageName
            // 获取本地配置
            val appOptimizePolicy = prefValue<AppOptimizePolicy>(
                PreferenceNameConstants.APP_OPTIMIZE_POLICY,
                curPackageName
            ) ?: AppOptimizePolicy().apply {
                this.packageName = curPackageName
                this.versionCode = BuildConfig.VERSION_CODE
                this.versionName = BuildConfig.VERSION_NAME
            }

            // 白名单
            /*findViewById<SwitchCompat>(R.id.configureAppProcessAppDoNothingSwitch)?.let { switch ->
                // 获取本地配置
                switch.isChecked =
                    (appOptimizePolicy.disableBackgroundTrimMem or
                            appOptimizePolicy.disableBackgroundGc or
                            appOptimizePolicy.disableForegroundTrimMem)

                // 监听
                switch.setOnCheckedChangeListener { _, isChecked ->
                    appOptimizePolicy.disableForegroundTrimMem = isChecked
                    appOptimizePolicy.disableBackgroundTrimMem = isChecked
                    appOptimizePolicy.disableBackgroundGc = isChecked

                    appOptimizePolicySaveAction(appOptimizePolicy)
                }
            }*/
            fun initAppMemoryOptimizeSwitch(
                switch: SwitchCompat?,
                policy: KMutableProperty0<Boolean>
            ) {
                switch?.let { sw ->
                    // 启用状态
                    sw.isChecked = !policy.get()
                    // 监听
                    sw.setOnCheckedChangeListener { _, isChecked ->
                        policy.set(isChecked)
                        appOptimizePolicySaveAction(appOptimizePolicy)
                    }
                }
            }

            initAppMemoryOptimizeSwitch(
                findViewById(R.id.configureAppProcessForegroundMemoryTrim),
                appOptimizePolicy::disableForegroundTrimMem
            )
            initAppMemoryOptimizeSwitch(
                findViewById(R.id.configureAppProcessBackgroundMemoryTrim),
                appOptimizePolicy::disableBackgroundTrimMem
            )
            initAppMemoryOptimizeSwitch(
                findViewById(R.id.configureAppProcessBackgroundGc),
                appOptimizePolicy::disableBackgroundGc
            )

            /*
                自定义主进程oom分数
             */
            // 输入框
            val customMainProcessOomScoreEditText =
                findViewById<EditText>(R.id.configureAppProcessCustomMainProcessOomScoreEditText)
            // 应用按钮
            val customMainProcessOomScoreApplyBtn =
                findViewById<Button>(R.id.configureAppProcessCustomMainProcessOomScoreApplyBtn)?.apply {
                    setOnClickListener { _ ->
                        val inputOomScore =
                            customMainProcessOomScoreEditText?.text.toString().trim()
                        if (StringUtils.isEmpty(inputOomScore)) {
                            return@setOnClickListener
                        }

                        val oomScore = try {
                            inputOomScore.toInt()
                        } catch (e: Exception) {
                            Int.MIN_VALUE
                        }
                        // 不合法的取值
                        if (oomScore < ProcessList.NATIVE_ADJ || oomScore >= ProcessList.UNKNOWN_ADJ) {
                            UiUtils.createDialog(
                                context = context,
                                text = "不合法的输入值",
                                cancelable = true,
                                enableNegativeBtn = true
                            ).show()
                            return@setOnClickListener
                        }

                        appOptimizePolicy.customMainProcessOomScore = oomScore

                        appOptimizePolicySaveAction(appOptimizePolicy)
                    }
                }
            // 启用/禁用此功能
            findViewById<SwitchCompat>(R.id.configureAppProcessCustomMainProcessOomScoreSwitch)?.let { switch ->
                // 开关的ui状态
                switch.isChecked =
                    appOptimizePolicy.enableCustomMainProcessOomScore.also { isEnabled ->
                        if (isEnabled) {
                            customMainProcessOomScoreEditText?.let {
                                it.isEnabled = true
                                // 从配置中读取值
                                it.setText(
                                    appOptimizePolicy.customMainProcessOomScore.toString(),
                                    TextView.BufferType.EDITABLE
                                )
                            }
                        }
                    }

                fun enableInputArea(enabled: Boolean) {
                    customMainProcessOomScoreEditText?.isEnabled = enabled
                    customMainProcessOomScoreApplyBtn?.isEnabled = enabled
                }
                // 默认是否启用输入框
                enableInputArea(switch.isChecked)
                // 监听
                switch.setOnCheckedChangeListener { _, isChecked ->
                    // 写入数据到对象
                    appOptimizePolicy.enableCustomMainProcessOomScore = isChecked

                    appOptimizePolicySaveAction(appOptimizePolicy)

                    // 输入框禁用与启用
                    enableInputArea(isChecked)
                }
            }
        }

        // 获取进程列表
        PackageUtils.getAppProcesses(this, appItem)

        // 获取本地配置
        val subProcessOomPolicyList = arrayListOf<SubProcessOomPolicy>()
        val iterator = appItem.processes.iterator()
        var processName: String
        while (iterator.hasNext()) {
            processName = iterator.next()
            // 剔除主进程
            if (!processName.contains(PackageUtils.processNameSeparator)) {
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

    private fun appOptimizePolicySaveAction(appOptimizePolicy: AppOptimizePolicy) {
        // 保存到本地
        prefPut(
            PreferenceNameConstants.APP_OPTIMIZE_POLICY,
            commit = true,
            appOptimizePolicy.packageName,
            appOptimizePolicy
        )
        // 通知模块进程
        showProgressBarViewForAction("正在设置") {
            sendMessage(this, MessageKeyConstants.appOptimizePolicy, appOptimizePolicy)
        }
    }
}