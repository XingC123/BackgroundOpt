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

package com.venus.backgroundopt.xposed.entity.self

import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy
import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy.MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_ALWAYS
import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy.MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_HAS_ACTIVITY
import com.venus.backgroundopt.common.entity.message.AppOptimizePolicy.MainProcessAdjManagePolicy.MAIN_PROC_ADJ_MANAGE_NEVER
import com.venus.backgroundopt.common.util.concurrent.lock.LockFlag
import com.venus.backgroundopt.common.util.ifTrue
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.environment.HookCommonProperties.appOptimizePolicyMap
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.manager.application.DefaultApplicationManager.Companion.isDefaultAppPkgName
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.Volatile

/**
 * app信息
 *
 * @author XingC
 * @date 2023/2/8
 */
class AppInfo(
    /**
     * 多开uid:
     * miui: 99910435 = 用户id+真正uid
     * 安卓13原生: 1010207 = 用户id+真正uid
     */
    var uid: Int,
    var userId: Int,
    var packageName: String?,
    var findAppResult: FindAppResult,
) : ILogger, LockFlag {
    fun init(): AppInfo {
        _activitySwitchEventHandlingCount.set(0)
        _activityRecord.set(null)
        appGroupEnum = AppGroupEnum.NONE

        setAdjHandleFunction()

        return this
    }

    /* *************************************************************************
     *                                                                         *
     * app状态信息                                                               *
     *                                                                         *
     **************************************************************************/
    private lateinit var runningInfo: RunningInfo

    // 主进程记录
    @Volatile
    @CleanUpField
    var mProcessRecord: ProcessRecord? = null
        set(value) {
            field = value

            if (BuildConfig.DEBUG) {
                logger.debug("设置ProcessRecord(userId: ${value!!.userId}, packageName: ${value.packageName}, pid: ${value.pid})")
            }
        }

    val mPid: Int get() = mProcessRecord!!.pid

    // 当前app在本模块内的内存分组
    @Volatile
    var appGroupEnum = AppGroupEnum.NONE

    // 当前app被模块检测到的activity的ComponentName
    @CleanUpField(reset = true)
    private val activities: MutableSet<Any> = Collections.newSetFromMap(ConcurrentHashMap(4))
    val appActivityCount: Int get() = activities.size

    // 正在处理的app切换事件的数量
    @CleanUpField(reset = true)
    private val _activitySwitchEventHandlingCount = AtomicInteger(0)
    val activitySwitchEventHandlingCount: Int get() = _activitySwitchEventHandlingCount.get()

    @CleanUpField(reset = true)
    private val _activityRecord = AtomicReference<Any?>(null)
    var activityRecord: Any?
        @OriginalObject(classPath = ClassConstants.ActivityRecord)
        get() = _activityRecord.get()
        set(@OriginalObject(classPath = ClassConstants.ActivityRecord) value) {
            this._activityRecord.set(value)
        }

    val importantSystemApp: Boolean = findAppResult.importantSystemApp

    fun isHandlingActivitySwitch(): Boolean {
        return _activitySwitchEventHandlingCount.get() > 0
    }

    fun increaseActivitySwitchEventHandlingCount(): Int {
        return _activitySwitchEventHandlingCount.incrementAndGet()
    }

    fun decreaseActivitySwitchEventHandlingCount(): Int {
        return _activitySwitchEventHandlingCount.decrementAndGet()
    }

    fun activityActive(@OriginalObject activityRecord: Any) {
        activities.add(activityRecord)
    }

    fun activityDie(@OriginalObject activityRecord: Any) {
        activities.remove(activityRecord)
    }

    fun resetActivityCount() = activities.clear()

    fun hasActivity(): Boolean = appActivityCount >= 1

    /* *************************************************************************
     *                                                                         *
     * adj处理                                                                  *
     *                                                                         *
     **************************************************************************/
    @Volatile
    var adjHandleFunction: (AppInfo) -> Boolean = handleAdjIfHasActivity

    /**
     * 应用是否需要管理adj
     *
     * @return 需要 -> true
     */
    fun shouldHandleAdj(): Boolean = adjHandleFunction(this)

    fun setAdjHandleFunction() {
        setAdjHandleFunction(appOptimizePolicyMap[packageName])
    }

    fun setAdjHandleFunction(appOptimizePolicy: AppOptimizePolicy?) {
        this.adjHandleFunction = when (appOptimizePolicy?.mainProcessAdjManagePolicy) {
            MAIN_PROC_ADJ_MANAGE_NEVER -> handleAdjNever
            MAIN_PROC_ADJ_MANAGE_ALWAYS -> handleAdjAlways
            MAIN_PROC_ADJ_MANAGE_HAS_ACTIVITY -> handleAdjIfHasActivity
            else -> handleAdjIfHasActivity
        }

        // 默认应用, 始终处理adj
        // 即便已配置过app优化策略
        isDefaultAppPkgName(packageName).ifTrue {
            this.adjHandleFunction = handleAdjAlways
        }

        // 如果现在是永不处理
        if (this.adjHandleFunction === handleAdjNever) {
            runningInfo.runningProcessList.asSequence()
                // 按包名匹配(处理应用分身情况)
                .filter { processRecord: ProcessRecord -> processRecord.packageName == packageName }
                .forEach(ProcessRecord::resetMaxAdj)
        }
    }

    /* *************************************************************************
     *                                                                         *
     * app状态信息                                                               *
     *                                                                         *
     **************************************************************************/
    val applicationIdentity: ApplicationIdentity get() = ApplicationIdentity(userId, packageName)

    /**
     * 主进程当前记录的adj
     */
    val mainProcFixedAdj: Int get() = mProcessRecord!!.fixedOomAdjScore

    /* *************************************************************************
    *                                                                         *
    * 锁                                                                       *
    *                                                                         *
    **************************************************************************/
    override val lock: ReentrantLock = ReentrantLock()

    /**
     * 该注解用以标识那些不需要被清理的字段
     */
    @Target(AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.RUNTIME)
    private annotation class CleanUpField(
        // 控制字段是否需要被重置
        val reset: Boolean = false,
    )

    fun clearAppInfo() {
        runCatchThrowable(catchBlock = { throwable: Throwable ->
            logger.error("AppInfo清理失败", throwable)
        }) {
            fields.forEach { field ->
                val cleanupField = field.getAnnotation(CleanUpField::class.java) ?: return@forEach
                if (cleanupField.reset) {
                    when (val any = field.get(this)) {
                        is MutableMap<*, *> -> any.clear()
                        is MutableCollection<*> -> any.clear()
                        is AtomicInteger -> any.set(0)
                        is AtomicBoolean -> any.set(false)
                        is AtomicReference<*> -> any.set(null)
                        else -> {}
                    }
                } else {
                    field.set(this, null)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AppInfo) return false

        if (userId != other.userId) return false
        if (packageName != other.packageName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId
        result = 31 * result + (packageName?.hashCode() ?: 0)
        return result
    }

    companion object {
        val handleAdjAlways: (AppInfo) -> Boolean = { true }
        val handleAdjNever: (AppInfo) -> Boolean = { false }
        val handleAdjIfHasActivity: (AppInfo) -> Boolean = { it.hasActivity() }

        @JvmStatic
        fun newInstance(
            uid: Int,
            userId: Int,
            packageName: String?,
            findAppResult: FindAppResult,
            runningInfo: RunningInfo,
        ): AppInfo {
            return AppInfo(
                uid = uid,
                userId = userId,
                packageName = packageName,
                findAppResult = findAppResult
            ).apply {
                this.runningInfo = runningInfo
            }
        }

        /* *************************************************************************
         *                                                                         *
         * 当前appInfo清理状态                                                       *
         *                                                                         *
         **************************************************************************/
        private val fields = AppInfo::class.java.declaredFields.asSequence()
            .filter { field ->
                field.getAnnotation(CleanUpField::class.java) != null
            }
            .onEach { it.isAccessible = true }
            .toList()
            .toTypedArray()
    }
}
