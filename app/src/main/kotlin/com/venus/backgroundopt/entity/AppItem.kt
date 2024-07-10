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
import java.text.Collator
import java.util.Locale

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
    var fullQualifiedProcessName: String? = null

    @JSONField(serialize = false)
    var oomAdjScore: Int = Int.MIN_VALUE

    @JSONField(serialize = false)
    var curAdj: Int = Int.MIN_VALUE

    @JSONField(serialize = false)
    var rssInBytes: Long = Long.MIN_VALUE

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
    enum class AppConfiguredEnum(val displayName: String) {
        AppOptimizePolicy("App内存优化策略"),
        SubProcessOomPolicy("OOM策略"),
        CustomMainProcessOomScore("自定义主进程OOM"),
        ShouldHandleMainProcAdj("管理主进程Adj"),
        MainProcessAdjManagePolicy("主进程ADJ管理策略"),
    }

    @get:JSONField(serialize = false)
    val appConfiguredEnumSet by lazy { HashSet<AppConfiguredEnum>() }

    companion object {
        @JvmStatic
        val appNameComparator: Comparator<AppItem>
            get() = object : Comparator<AppItem> {
                val chinaCollator = Collator.getInstance(Locale.CHINA)
                fun isEnglish(char: Char): Boolean {
                    return char in 'a'..'z' || char in 'A'..'z'
                }

                /**
                 * 最终排序为: 前: 所有英文的app名字。后: 所有中文的app名字
                 */
                override fun compare(o1: AppItem, o2: AppItem): Int {
                    val firstAppName = o1.appName
                    val secondAppName = o2.appName
                    // 只判断app名字的第一个字符(英文: 首字母。中文: 第一个字)
                    val isFirstNameEnglish = isEnglish(firstAppName[0])
                    val isSecondNameEnglish = isEnglish(secondAppName[0])
                    if (isFirstNameEnglish) {
                        if (isSecondNameEnglish) {
                            // 都是英文
                            return o1.appName.lowercase().compareTo(o2.appName.lowercase())
                        }
                        // 第一个是英文, 第二个是中文。
                        return -1
                    } else if (!isSecondNameEnglish) {
                        // 两个都是中文
                        return chinaCollator.compare(firstAppName, secondAppName)
                    }

                    return 1
                }
            }

        @JvmStatic
        val pidComparator: Comparator<AppItem>
            get() = Comparator { o1, o2 -> o2.pid - o1.pid }

        @JvmStatic
        val pidAscComparator: Comparator<AppItem>
            get() = Comparator { o1, o2 -> o1.pid - o2.pid }

        @JvmStatic
        val uidComparator: Comparator<AppItem>
            get() = Comparator { o1, o2 -> o2.uid - o1.uid }

        @JvmStatic
        val uidAscComparator: Comparator<AppItem>
            get() = Comparator { o1, o2 -> o1.uid - o2.uid }
    }
}