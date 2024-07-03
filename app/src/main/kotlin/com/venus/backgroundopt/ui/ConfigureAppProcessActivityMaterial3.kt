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

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AppItem
import com.venus.backgroundopt.entity.AppItem.AppConfiguredEnum
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.environment.PreferenceDefaultValue
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants.SUB_PROCESS_OOM_POLICY
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.ui.base.BaseActivityMaterial3
import com.venus.backgroundopt.ui.component.VenusListCheckMaterial3
import com.venus.backgroundopt.ui.component.VenusSwitchMaterial3
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.UiUtils
import com.venus.backgroundopt.utils.getTmpData
import com.venus.backgroundopt.utils.ifFalse
import com.venus.backgroundopt.utils.ifTrue
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicy.MainProcessAdjManagePolicy
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler.AppOptimizePolicyMessage
import com.venus.backgroundopt.utils.message.sendMessage
import com.venus.backgroundopt.utils.preference.prefPut
import com.venus.backgroundopt.utils.preference.prefValue
import com.venus.backgroundopt.utils.runCatchThrowable
import com.venus.backgroundopt.utils.showProgressBarViewForAction
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KMutableProperty1

class ConfigureAppProcessActivityMaterial3 : BaseActivityMaterial3() {
    private lateinit var appItem: AppItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showProgressBarViewForAction("正在加载...") {
            init()
        }
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.configureAppProcAcitivityToolBarHelpMenuItem -> {
                UiUtils.createDialog(
                    context = this,
                    viewResId = R.layout.content_configure_app_proc_toolbar_help
                ).show()
            }
        }
    }

    override fun getContentView(): Int {
        return R.layout.activity_configure_app_process_material3
    }


    private fun init() {
        appItem = getTmpData() as AppItem

        /*
            app优化策略
         */
        val curPackageName = appItem.packageName
        // 获取本地配置
        val appOptimizePolicy = getAppOptimizePolicy(curPackageName)

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

            fun initAppMemoryOptimizeRadioGroup(
                radioGroupId: Int,
                policy: KMutableProperty0<Boolean?>,
                buttonIdArr: Array<Int>
            ) {
                findViewById<RadioGroup>(radioGroupId)?.let { group ->
                    // 初始选中
                    val initialCheckedBtnId = when (policy.get()) {
                        null -> buttonIdArr[0]
                        true -> buttonIdArr[1]
                        false -> buttonIdArr[2]
                    }
                    group.check(initialCheckedBtnId)

                    // 事件监听
                    group.setOnCheckedChangeListener { _, checkedId ->
                        var showConfigTag = false
                        when (findViewById<RadioButton>(checkedId).id) {
                            buttonIdArr[0] -> {
                                policy.set(null)
                                showConfigTag = false
                            }

                            buttonIdArr[1] -> {
                                policy.set(true)
                                showConfigTag = true
                            }

                            buttonIdArr[2] -> {
                                policy.set(false)
                                showConfigTag = true
                            }

                            else -> {
                                policy.set(null)
                                showConfigTag = false
                            }
                        }

                        // 更新tag的显示
                        appItem.appConfiguredEnumSet.apply {
                            if (showConfigTag) {
                                appItem.appConfiguredEnumSet.add(AppConfiguredEnum.AppOptimizePolicy)
                            } else {
                                appItem.appConfiguredEnumSet.remove(AppConfiguredEnum.AppOptimizePolicy)
                            }
                        }

                        appOptimizePolicySaveAction(appOptimizePolicy)
                    }
                }
            }

            initAppMemoryOptimizeRadioGroup(
                radioGroupId = R.id.configAppProcessFgMemTrimGroup,
                policy = appOptimizePolicy::enableForegroundTrimMem,
                buttonIdArr = arrayOf(
                    R.id.configAppProcessFgMemTrimDefaultRadio,
                    R.id.configAppProcessFgMemTrimEnableRadio,
                    R.id.configAppProcessFgMemTrimDisableRadio,
                )
            )
            initAppMemoryOptimizeRadioGroup(
                radioGroupId = R.id.configAppProcessBgMemTrimGroup,
                policy = appOptimizePolicy::enableBackgroundTrimMem,
                buttonIdArr = arrayOf(
                    R.id.configAppProcessBgMemTrimDefaultRadio,
                    R.id.configAppProcessBgMemTrimEnableRadio,
                    R.id.configAppProcessBgMemTrimDisableRadio,
                )
            )
            initAppMemoryOptimizeRadioGroup(
                radioGroupId = R.id.configAppProcessBgGcGroup,
                policy = appOptimizePolicy::enableBackgroundGc,
                buttonIdArr = arrayOf(
                    R.id.configAppProcessBgGcDefaultRadio,
                    R.id.configAppProcessBgGcEnableRadio,
                    R.id.configAppProcessBgGcDisableRadio,
                )
            )

            initCustomMainProcAdj(appOptimizePolicy)

            /*
                是否被模块纳入管理
             */
            findViewById<VenusSwitchMaterial3>(R.id.configureAppProcessShouldHandleMainProcAdjSwitch)?.let { switch ->
                // 初始状态
                switch.isChecked = appOptimizePolicy.shouldHandleAdj
                    ?: appOptimizePolicy.shouldHandleAdjUiState

                switch.setOnCheckedChangeListener { _, isChecked ->
                    appOptimizePolicy.shouldHandleAdj = isChecked
                    appOptimizePolicySaveAction(appOptimizePolicy)
                }

                switch.setButtonOnClickedListener {
                    appOptimizePolicy.shouldHandleAdj = null
                    appOptimizePolicySaveAction(appOptimizePolicy) {
                        runOnUiThread { switch.isChecked = it.shouldHandleAdjUiState }
                    }
                }
            }

            // 拥有界面时临时保活主进程
            findViewById<VenusSwitchMaterial3>(R.id.configureAppProcessKeepMainProcessAliveHasActivitySwitch)?.let { switch ->
                fun setSwitchChecked(appOptimizePolicy: AppOptimizePolicy) {
                    switch.isChecked = appOptimizePolicy.keepMainProcessAliveHasActivity
                        ?: PreferenceDefaultValue.keepMainProcessAliveHasActivity
                }
                // 初始状态
                setSwitchChecked(appOptimizePolicy)

                switch.setOnCheckedChangeListener { _, isChecked ->
                    appOptimizePolicy.keepMainProcessAliveHasActivity = isChecked
                    appOptimizePolicySaveAction(appOptimizePolicy)
                }

                switch.setButtonOnClickedListener {
                    appOptimizePolicy.keepMainProcessAliveHasActivity = null
                    setSwitchChecked(appOptimizePolicy)
                    appOptimizePolicySaveAction(appOptimizePolicy)
                }
            }

            /*
             * 管理主进程adj的条件
             */
            findViewById<VenusListCheckMaterial3>(R.id.manage_main_process_adj_venus_list_check)?.let { listCheck ->
                fun getMapFromMainProcessAdjManagePolicy(): Map<String, String> {
                    return HashMap<String, String>(MainProcessAdjManagePolicy.entries.size).apply {
                        MainProcessAdjManagePolicy.entries.forEach { policy ->
                            put(policy.uiText, policy.name)
                        }
                    }
                }
                // 界面显示与值的映射
                val map = getMapFromMainProcessAdjManagePolicy()
                listCheck.setEntriesAndValue(map)

                // 默认值
                listCheck.defaultValue = appOptimizePolicy.defaultMainProcessAdjManagePolicyUiText

                listCheck.setDialogConfirmClickListener { _: Int, _: Int, _: CharSequence?, entryValue: CharSequence? ->
                    val policyName = entryValue?.toString() ?: return@setDialogConfirmClickListener
                    appOptimizePolicy.mainProcessAdjManagePolicy =
                        MainProcessAdjManagePolicy.valueOf(policyName)

                    // 更新tag的显示
                    appItem.appConfiguredEnumSet.apply {
                        if (appOptimizePolicy.mainProcessAdjManagePolicy == MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_DEFAULT) {
                            remove(AppConfiguredEnum.MainProcessAdjManagePolicy)
                        } else {
                            add(AppConfiguredEnum.MainProcessAdjManagePolicy)
                        }
                    }

                    appOptimizePolicySaveAction(appOptimizePolicy) {
                        runOnUiThread {
                            listCheck.defaultValue = it.defaultMainProcessAdjManagePolicyUiText
                        }
                    }
                }
            }
        }

        // 获取进程列表
        PackageUtils.getAppProcesses(this, appItem)

        // 获取本地配置
        val subProcessOomPolicyMap = HashMap<String, SubProcessOomPolicy>()
        val iterator = appItem.processes.iterator()
        var processName: String
        while (iterator.hasNext()) {
            processName = iterator.next()
            // 剔除主进程
            if (!processName.contains(PackageUtils.processNameSeparator)) {
                iterator.remove()
                continue
            }

            val subProcessOomPolicy = prefValue<SubProcessOomPolicy>(
                SUB_PROCESS_OOM_POLICY,
                processName
            ) ?: run {
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

            subProcessOomPolicyMap[processName] = subProcessOomPolicy
        }

        // 设置view
        runOnUiThread {
            findViewById<RecyclerView>(R.id.configureAppProcessRecycleView).apply {
                layoutManager =
                    LinearLayoutManager(this@ConfigureAppProcessActivityMaterial3).apply {
                        orientation = LinearLayoutManager.VERTICAL
                    }
                adapter = ConfigureAppProcessAdapter(
                    appItem = appItem,
                    processes = appItem.processes.toList(),
                    subProcessOomPolicyMap = subProcessOomPolicyMap
                )
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 自定义主进程oom分数                                                        *
     *                                                                         *
     **************************************************************************/
    private fun initCustomMainProcAdj(appOptimizePolicy: AppOptimizePolicy) {
        // 输入框
        val fgAdjEditText = findViewById<EditText>(
            R.id.configureAppProcessCustomMainProcessFgAdjEditText
        )
        val bgAdjEditText = findViewById<EditText>(
            R.id.configureAppProcessCustomMainProcessBgAdjEditText
        )

        val customAdjEditTextAndPropertyMap = mapOf(
            fgAdjEditText to AppOptimizePolicy::customMainProcessFgAdj,
            bgAdjEditText to AppOptimizePolicy::customMainProcessOomScore
        )
        // 应用按钮
        val customMainProcessOomScoreApplyBtn = findViewById<Button>(
            R.id.configureAppProcessCustomMainProcessOomScoreApplyBtn
        ).apply {
            setOnClickListener { _ ->
                var isAllAdjValid = true
                val adjEditTextArray = arrayOf<EditText>(fgAdjEditText, bgAdjEditText)
                val adjArray = IntArray(2)
                customAdjEditTextAndPropertyMap.keys.forEachIndexed { index, editText ->
                    val inputAdjStr = editText.text.toString().trim()
                    if (inputAdjStr.isBlank()) {
                        adjArray[index] = Int.MIN_VALUE
                        return@forEachIndexed
                    }

                    val finalAdj = runCatchThrowable(
                        catchBlock = {
                            // 非数字
                            isAllAdjValid = false
                            Int.MIN_VALUE
                        }
                    ) {
                        val adj = inputAdjStr.toInt()
                        return@runCatchThrowable if (ProcessList.isValidAdj(adj)) {
                            adj
                        } else {
                            // 取值不正确
                            isAllAdjValid = false
                            Int.MIN_VALUE

                        }
                    }!!
                    adjArray[index] = finalAdj
                }
                isAllAdjValid.ifFalse {
                    UiUtils.createDialog(
                        context = context,
                        text = "不合法的输入值",
                        cancelable = true,
                        enableNegativeBtn = true
                    ).show()
                    return@setOnClickListener
                }

                appOptimizePolicy.apply {
                    enableCustomMainProcessOomScore = true
                    // 设置自定义的adj
                    adjEditTextArray.forEachIndexed { index, editText ->
                        customAdjEditTextAndPropertyMap[editText]!!.set(
                            this,
                            adjArray[index]
                        )
                    }
                }

                // 更新tag显示
                appItem.appConfiguredEnumSet.add(AppConfiguredEnum.CustomMainProcessOomScore)

                appOptimizePolicySaveAction(appOptimizePolicy)
            }
        }

        // 启用/禁用此功能
        findViewById<SwitchCompat>(R.id.configureAppProcessCustomMainProcessOomScoreSwitch).apply {
            val switch = this
            // 开关的ui状态
            switch.isChecked = appOptimizePolicy.enableCustomMainProcessOomScore.also { isEnabled ->
                customAdjEditTextAndPropertyMap.forEach { (editText, property) ->
                    initCustomMainProcAdjEditText(
                        isEnabled = isEnabled,
                        editText = editText,
                        appOptimizePolicy = appOptimizePolicy,
                        property = property
                    )
                }
            }

            fun enableCustomMainProcAdjComponents() {
                enableCustomMainProcAdjComponents(
                    isEnabled = switch.isChecked,
                    editTexts = customAdjEditTextAndPropertyMap.keys,
                    applyBtn = customMainProcessOomScoreApplyBtn
                )
            }

            // 默认是否启用输入框
            enableCustomMainProcAdjComponents()
            // 监听
            switch.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked && appOptimizePolicy.enableCustomMainProcessOomScore != isChecked) {
                    customAdjEditTextAndPropertyMap.forEach { (editText, property) ->
                        // 重置输入框
                        editText.text.clear()
                        // 重置app配置中的值
                        property.set(appOptimizePolicy, Int.MIN_VALUE)
                    }
                    appOptimizePolicy.enableCustomMainProcessOomScore = false

                    // 更新tag显示
                    appItem.appConfiguredEnumSet.remove(AppConfiguredEnum.CustomMainProcessOomScore)

                    appOptimizePolicySaveAction(appOptimizePolicy)
                }

                // 输入框禁用与启用
                enableCustomMainProcAdjComponents()
            }
        }
    }

    private fun initCustomMainProcAdjEditText(
        isEnabled: Boolean,
        editText: EditText,
        appOptimizePolicy: AppOptimizePolicy,
        property: KMutableProperty1<AppOptimizePolicy, Int>,
    ) {
        isEnabled.ifTrue {
            editText.isEnabled = true
            // 从配置中读取值
            val adj = property.get(appOptimizePolicy)
            ProcessList.isValidAdj(adj).ifTrue {
                editText.setText(
                    property.get(appOptimizePolicy).toString(),
                    TextView.BufferType.EDITABLE
                )
            }
        }
    }

    private fun enableCustomMainProcAdjComponents(
        isEnabled: Boolean,
        editTexts: Collection<EditText>,
        applyBtn: Button
    ) {
        editTexts.forEach { editText ->
            editText.isEnabled = isEnabled
        }
        applyBtn.isEnabled = isEnabled
    }

    private fun appOptimizePolicySaveAction(
        appOptimizePolicy: AppOptimizePolicy,
        block: ((AppOptimizePolicy) -> Unit)? = null
    ) {
        // 保存到本地
        saveAppMemoryOptimize(appOptimizePolicy, this)
        // 通知模块进程
        showProgressBarViewForAction("正在设置") {
            val returnAppOptimizePolicy = sendMessage<AppOptimizePolicy>(
                context = this,
                key = MessageKeyConstants.appOptimizePolicy,
                value = AppOptimizePolicyMessage().apply {
                    value = appOptimizePolicy
                    uid = appItem.uid
                    packageName = appItem.packageName
                    messageType = AppOptimizePolicyMessage.MSG_SAVE
                }
            )
            returnAppOptimizePolicy?.let { block?.invoke(it) }
        }
    }

    private fun getAppOptimizePolicy(packageName: String): AppOptimizePolicy {
        return sendMessage<AppOptimizePolicy>(
            context = this,
            key = MessageKeyConstants.appOptimizePolicy,
            AppOptimizePolicyMessage().apply {
                this.uid = appItem.uid
                this.packageName = packageName

                messageType = AppOptimizePolicyMessage.MSG_CREATE_OR_GET
            }
        )!!
    }

    companion object {
        @JvmStatic
        fun saveAppMemoryOptimize(appOptimizePolicy: AppOptimizePolicy, context: Context) {
            context.prefPut(
                PreferenceNameConstants.APP_OPTIMIZE_POLICY,
                commit = true,
                appOptimizePolicy.packageName,
                appOptimizePolicy
            )
        }
    }
}