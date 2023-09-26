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
    val uid:Int,
    val appIcon: Drawable,
    val packageInfo: PackageInfo?
) {
    var pid: Int = Int.MIN_VALUE
    var processName: String? = null
    var oomAdjScore:Int = Int.MIN_VALUE
}