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
                    
 package com.venus.backgroundopt.entity

import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import com.alibaba.fastjson2.annotation.JSONCreator
import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.manager.process.AbstractAppOptimizeManager.AppOptimizeEnum
import com.venus.backgroundopt.manager.process.ProcessingResult
import com.venus.backgroundopt.utils.message.MessageFlag

/**
 * 用于ui展示应用列表时使用
 *
 * @author XingC
 * @date 2023/9/25
 */
class AppItem @JSONCreator constructor() : MessageFlag {
    @JSONField(serialize = false)
    lateinit var appName: String

    @JSONField(serialize = false)
    lateinit var appIcon: Drawable
    lateinit var packageName: String

    @JSONField(serialize = false)
    lateinit var packageInfo: PackageInfo

    @JSONField(serialize = false)
    var uid = Int.MIN_VALUE

    @JSONField(serialize = false)
    var pid: Int = Int.MIN_VALUE

    @JSONField(serialize = false)
    var processName: String? = null

    @JSONField(serialize = false)
    var oomAdjScore: Int = Int.MIN_VALUE

    @JSONField(serialize = false)
    var curAdj: Int = Int.MIN_VALUE

    // 清单文件中注册的所有进程
    @JSONField(serialize = false)
    lateinit var processes: MutableSet<String>

    @JSONField(serialize = false)
    var versionName: String? = null

    @JSONField(serialize = false)
    var longVersionCode: Long = Long.MIN_VALUE

    @JSONField(serialize = false)
    lateinit var lastProcessingResultMap: MutableMap<AppOptimizeEnum, ProcessingResult>

    // 是否是系统app
    @JSONField(serialize = false)
    var systemApp = false

    constructor(packageName: String) : this() {
        this.packageName = packageName
    }

    constructor(
        appName: String,
        packageName: String,
        uid: Int,
        appIcon: Drawable,
        packageInfo: PackageInfo
    ) : this(packageName) {
        this.appName = appName
        this.uid = uid
        this.appIcon = appIcon
        this.packageInfo = packageInfo
    }

    /* *************************************************************************
     *                                                                         *
     * 用于已安装app列表                                                          *
     *                                                                         *
     **************************************************************************/
    enum class AppConfiguredEnum(val displayName:String) {
        AppOptimizePolicy("App内存优化策略"),
        SubProcessOomPolicy("OOM策略"),
        CustomMainProcessOomScore("自定义主进程OOM"),
        ShouldHandleMainProcAdj("管理主进程Adj"),
        KeepMainProcessAliveHasActivity("拥有界面时临时保活主进程"),
    }

    @get:JSONField(serialize = false)
    val appConfiguredEnumSet by lazy { HashSet<AppConfiguredEnum>() }
}