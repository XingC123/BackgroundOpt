package com.venus.backgroundopt.entity

import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable

/**
 * 用于ui展示应用列表时使用
 *
 * @author XingC
 * @date 2023/9/25
 */
data class AppItem(
    val appName: String,
    val packageName: String,
    val uid: Int,
    val appIcon: Drawable,
    val packageInfo: PackageInfo?
) {
    var pid: Int = Int.MIN_VALUE
    var processName: String? = null
    var oomAdjScore: Int = Int.MIN_VALUE
    var curAdj: Int = Int.MIN_VALUE

    // 清单文件中注册的所有进程
    lateinit var processes: Set<String>

    var versionName: String? = null
    var versionCode: Int = Int.MIN_VALUE
    var longVersionCode: Long = Long.MIN_VALUE
}