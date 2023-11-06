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
    lateinit var lastProcessingResultMap:MutableMap<AppOptimizeEnum, ProcessingResult>

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
}