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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am

import android.content.pm.ApplicationInfo
import android.os.Build
import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.common.entity.preference.OomWorkModePref
import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.common.util.PackageUtils
import com.venus.backgroundopt.common.util.ifTrue
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.log.logDebug
import com.venus.backgroundopt.common.util.log.logInfo
import com.venus.backgroundopt.common.util.nullableFilter
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.xposed.entity.android.android.content.ComponentCallbacks2
import com.venus.backgroundopt.xposed.entity.android.android.os.Process
import com.venus.backgroundopt.xposed.entity.android.android.os.Process.PROC_NEWLINE_TERM
import com.venus.backgroundopt.xposed.entity.android.android.os.Process.PROC_OUT_LONG
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessServiceRecord.ProcessServiceRecordHelper
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessStateRecord.ProcessStateRecordHelper
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.ProcessRecordCompatSinceA12
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.ProcessRecordCompatUntilA11
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.entity.base.callStaticMethod
import com.venus.backgroundopt.xposed.entity.self.AppInfo
import com.venus.backgroundopt.xposed.entity.self.ProcessAdjConstants
import com.venus.backgroundopt.xposed.entity.self.ProcessRecordBaseInfo
import com.venus.backgroundopt.xposed.environment.HookCommonProperties
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.manager.message.handle.isCustomMainProcessAdjValid
import com.venus.backgroundopt.xposed.manager.process.oom.isHighPriorityProcessByBasicProperty
import com.venus.backgroundopt.xposed.util.callMethod
import com.venus.backgroundopt.xposed.util.getBooleanFieldValue
import com.venus.backgroundopt.xposed.util.getIntFieldValue
import com.venus.backgroundopt.xposed.util.getObjectFieldValue
import com.venus.backgroundopt.xposed.util.getStringFieldValue
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 封装了[ClassConstants.ProcessRecord]
 *
 * @author XingC
 * @date 2023/9/26
 */
abstract class ProcessRecord(
    @field:JSONField(serialize = false)
    final override val originalInstance: Any,
) : ProcessRecordBaseInfo(), IEntityWrapper, IEntityCompatFlag, ILogger {
    @JSONField(serialize = false)
    lateinit var appInfo: AppInfo

    @JSONField(serialize = false)
    lateinit var processStateRecord: ProcessStateRecord

    @get:JSONField(serialize = false)
    val mKilledByAm: Boolean get() = originalInstance.callMethod<Boolean>(MethodConstants.isKilledByAm)

    // 当前ProcessRecord已记录的最大adj
    @JSONField(serialize = false)
    var recordMaxAdj = 0

    @get:JSONField(serialize = false)
    val thread: Any? get() = getThread(originalInstance)

    /* *************************************************************************
     *                                                                         *
     * 最大adj                                                                  *
     *                                                                         *
     **************************************************************************/
    /**
     * 设置默认的最大adj
     */
    @JSONField(serialize = false)
    fun setDefaultMaxAdj() {
        originalMaxAdj = getMaxAdj()

        val maxAdj = if (mainProcess) {
            defaultMaxAdj
        } else {
            highPrioritySubprocessDefaultMaxAdj
        }
        setMaxAdj(maxAdj)
    }

    /**
     * 设置指定的最大adj
     * 注意:
     * <pre>
     *      在Redmi k30p MIUI13 22.7.11 (Android 12)中, 设置小于0的值(未充分测试, 只设置过-800) 且 打开的app是单进程,
     *      会导致在最近任务上划无法杀死app。
     *      在另一台机器Redmi Note5p Nusantara v5.2 official (Android安全更新2022.11.5, Android 13)中无此问题
     * </pre>
     *
     * @param maxAdj 最大adj的值
     */
    @JSONField(serialize = false)
    fun setMaxAdj(maxAdj: Int) {
        processStateRecord.maxAdj = maxAdj
        recordMaxAdj = maxAdj
    }

    /**
     * 是否已经对该进程设置过maxAdj
     *
     * @return
     */
    @JSONField(serialize = false)
    fun isSetMaxAdj(): Boolean = recordMaxAdj != 0

    /**
     * 重置进程的maxAdj
     *
     */
    fun resetMaxAdj() {
        if (isSetMaxAdj()) {
            runCatchThrowable(catchBlock = { throwable: Throwable ->
                logger.error("pid: [${pid}] >>> maxAdj重置失败", throwable)
            }) {
                processStateRecord.maxAdj = ProcessList.UNKNOWN_ADJ
                recordMaxAdj = 0
                if (BuildConfig.DEBUG) {
                    logger.debug("pid: [${pid}] >>> maxAdj重置成功")
                }
            }
        }
    }

    /**
     * 获取进程的最大adj
     *
     * @return 进程的最大adj
     */
    @JSONField(serialize = false)
    fun getMaxAdj(): Int {
        return processStateRecord.maxAdj
    }

    /**
     * 是否需要调整最大adj
     *
     * @return 若已设置的最大adj!=当前所使用的最大adj => true
     */
    @JSONField(serialize = false)
    fun isNeedAdjustMaxAdj(): Boolean {
        return getMaxAdj() != recordMaxAdj
    }

    /**
     * 如果当前最大adj不等于已记录的最大adj, 则进行调整
     */
    fun adjustMaxAdjIfNeed() {
        if (isNeedAdjustMaxAdj()) {
            setMaxAdj(recordMaxAdj)
        }
    }

    /**
     * 设置trim
     *
     * @param level trim级别([ComponentCallbacks2])
     * @return 成功设置 -> true
     */
    fun scheduleTrimMemory(level: Int): Boolean {
        return thread?.let {
            it.callMethod(MethodConstants.scheduleTrimMemory, level)
            true
        } ?: false
    }

    @JSONField(serialize = false)
    fun getCurAdjNative(): Int = getCurAdjNative(pid)

    @JSONField(serialize = false)
    fun getFullProcessName(): String = PackageUtils.absoluteProcessName(packageName, processName)

    /* *************************************************************************
     *                                                                         *
     * 进程内存调节                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * 获取当前的资源占用
     * @return Long 正常获取的值 >= 0, 获取异常 = Long.MIN_VALUE
     */
    @JSONField(serialize = false)
    fun getCurRssInBytes(): Long {
        return MemoryStatUtil.readMemoryStatFromFilesystem(uid, pid)?.rssInBytes ?: Long.MIN_VALUE
    }

    /**
     * 是否需要应用内存调整
     *
     * @return true if necessary
     */
    @JSONField(serialize = false)
    fun isNecessaryToOptimize(): Boolean {
        return getCurRssInBytes() > minOptimizeRssInBytes
    }

    /* *************************************************************************
     *                                                                         *
     * 独立于安卓原本ProcessRecord的字段                                           *
     *                                                                         *
     **************************************************************************/
    // 此oomAdjScore为模块设置的值。若当前非app主进程, 则此值和真实值保持一致
    @JSONField(serialize = false)
    val oomAdjScoreAtomicInteger = AtomicInteger(Int.MIN_VALUE)

    // 上次压缩时间
    @JSONField(serialize = false)
    private val lastCompactTimeAtomicLong = AtomicLong(0L)

    // 压缩间隔
    @JSONField(serialize = false)
    private val compactInterval = TimeUnit.MINUTES.toMillis(7)

    fun addCompactProcess(runningInfo: RunningInfo) {
        runningInfo.processManager.addCompactProcess(this)
    }

    @JSONField(serialize = false)
    fun isAllowedCompact(time: Long): Boolean = time - getLastCompactTime() > compactInterval

    @JvmName("getOomAdjScoreFromAtomicInteger")
    @JSONField(serialize = false)
    fun getOomAdjScore(): Int = oomAdjScoreAtomicInteger.get()

    @JvmName("setOomAdjScoreToAtomicInteger")
    @JSONField(serialize = false)
    fun setOomAdjScore(oomAdjScore: Int) {
        this.oomAdjScoreAtomicInteger.set(oomAdjScore)
        this.oomAdjScore = oomAdjScore
    }

    @JSONField(serialize = false)
    private fun getLastCompactTime(): Long = lastCompactTimeAtomicLong.get()

    @JSONField(serialize = false)
    fun setLastCompactTime(time: Long) = lastCompactTimeAtomicLong.set(time)

    /* *************************************************************************
     *                                                                         *
     * 进程拥有的唤醒锁的数量                                                       *
     *                                                                         *
     **************************************************************************/
    @JSONField(serialize = false)
    private var _wakeLockCount = AtomicInteger(0)

    @get:JSONField(serialize = false)
    val wakeLockCount: Int get() = _wakeLockCount.get()

    fun incrementWakeLockCount() {
        _wakeLockCount.incrementAndGet()
    }

    fun decrementWakeLockCount() {
        _wakeLockCount.decrementAndGet()
    }

    fun incrementWakeLockCountAndChangeAdjHandleActionType() {
        incrementWakeLockCount()
        /*if (!(mainProcess || isHighPrioritySubProcessBasic())) {
            appInfo.getmProcessRecord()?.adjHandleActionType?.let { adjHandleActionType = it }
        }*/
        // adjHandleActionType = (adjHandleActionType shl 4) or AdjHandleActionType.WAKE_LOCK
        adjHandleActionType = adjHandleActionType xor AdjHandleActionType.WAKE_LOCK
    }

    fun decrementWakeLockCountAndChangeAdjHandleActionType() {
        decrementWakeLockCount()
        /*if (!(mainProcess || isHighPrioritySubProcessBasic())) {
            adjHandleActionType = AdjHandleActionType.OTHER
        }*/
        // adjHandleActionType = adjHandleActionType shr 4
        adjHandleActionType = adjHandleActionType xor AdjHandleActionType.WAKE_LOCK
    }

    @JSONField(serialize = false)
    fun hasWakeLock(): Boolean = wakeLockCount > 0

    /* *************************************************************************
   *                                                                         *
   * adj处理方式                                                               *
   *                                                                         *
   **************************************************************************/
    object AdjHandleActionType {
        const val DO_NOTHING = 0
        const val CUSTOM_MAIN_PROCESS = 1
        const val GLOBAL_OOM_ADJ = 2
        const val OTHER = 3
        const val WAKE_LOCK = 4

        /*val WAKE_LOCK_ARRAY = run {
            AdjHandleActionType::class.declaredMemberProperties
                .filter { it.isConst }
                // .filter { property -> property != AdjHandleActionType::WAKE_LOCK }
                .map { property ->
                    if (property == AdjHandleActionType::WAKE_LOCK) {
                        WAKE_LOCK
                    } else {
                        (property.call() as Int) xor WAKE_LOCK
                    }
                }
                .distinct()
                .sorted()
                .toIntArray()
        }*/
    }

    var adjHandleActionType: Int = AdjHandleActionType.OTHER

    private fun initAdjHandleType() {
        // 高优先级进程
        if (isHighPriorityProcessByBasicProperty()) {
            // 是否配置自定义主进程
            val appOptimizePolicy = HookCommonProperties.appOptimizePolicyMap[packageName]
            appOptimizePolicy.isCustomMainProcessAdjValid().ifTrue {
                adjHandleActionType = AdjHandleActionType.CUSTOM_MAIN_PROCESS
                return
            }
            // 是否开启了全局oom
            if (HookCommonProperties.globalOomScorePolicy.value.enabled) {
                adjHandleActionType = AdjHandleActionType.GLOBAL_OOM_ADJ
                return
            }
        }

        adjHandleActionType = AdjHandleActionType.OTHER
    }

    fun resetAdjHandleType() {
        initAdjHandleType()
    }

    object ProcessRecordHelper : IEntityCompatHelper<ProcessRecord> {
        override val instanceClazz: Class<out ProcessRecord>
        override val instanceCreator: (Any) -> ProcessRecord

        init {
            if (OsUtils.isSOrHigher) {
                instanceClazz = ProcessRecordCompatSinceA12::class.java
                instanceCreator = ::createProcessRecordSinceA12
            } else {
                instanceClazz = ProcessRecordCompatUntilA11::class.java
                instanceCreator = ::createProcessRecordUntilA11
            }
        }

        @JvmStatic
        fun createProcessRecordSinceA12(@OriginalObject instance: Any): ProcessRecord =
            ProcessRecordCompatSinceA12(instance)

        @JvmStatic
        fun createProcessRecordUntilA11(@OriginalObject instance: Any): ProcessRecord =
            ProcessRecordCompatUntilA11(instance)
    }

    companion object : IProcessRecord {
        val LONG_FORMAT by lazy {
            intArrayOf(PROC_NEWLINE_TERM or PROC_OUT_LONG)
        }

        // 默认的主进程要设置的adj
        const val DEFAULT_MAIN_ADJ = ProcessAdjConstants.DEFAULT_MAIN_ADJ

        // 默认的子进程要设置的adj
        const val SUB_PROC_ADJ = ProcessAdjConstants.SUB_PROC_ADJ

        // 默认的最大adj
        var defaultMaxAdj = ProcessList.UNKNOWN_ADJ

        // 高优先级子进程的最大adj
        var highPrioritySubprocessDefaultMaxAdj = ProcessList.UNKNOWN_ADJ

        // 是否设置默认最大adj
        var isNeedSetDefaultMaxAdj = false

        @JvmField
        var defaultMaxAdjStr = "null"

        // 当前资源占用大于此值则进行优化
        // 268435456 = 256MB *1024 * 1024
        @JvmField
        var minOptimizeRssInBytes = 268435456.0

        @JvmField
        var minOptimizeRssInMBytesStr = "null"

        // 触发内存优化的资源占用大小的封顶阈值
        // 由于当前算法是根据最大内存计算。因此需要一个限制, 防止因为内存过大而导致阈值过高
        // 419430400 = 400 * 1024 * 1024
        const val maxOptimizeRssInBytes = 419430400.0

        // 资源占用因子
        const val minOptimizeRssFactor = 0.025

        init {
            // 根据配置文件决定defaultMaxAdj
            when (HookCommonProperties.oomWorkModePref.oomMode) {
                OomWorkModePref.MODE_STRICT,
                OomWorkModePref.MODE_STRICT_SECONDARY,
                OomWorkModePref.MODE_BALANCE_PLUS,
                -> {
                    isNeedSetDefaultMaxAdj = true

                    defaultMaxAdj = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ
                    // 高优先级子进程
                    highPrioritySubprocessDefaultMaxAdj = ProcessList.VISIBLE_APP_ADJ
                }
                // OomWorkModePref.MODE_NEGATIVE -> ProcessList.HEAVY_WEIGHT_APP_ADJ
                else -> ProcessList.UNKNOWN_ADJ
            }
            defaultMaxAdjStr = "${
                if (defaultMaxAdj == ProcessList.UNKNOWN_ADJ) "系统默认" else defaultMaxAdj
            }"
            logInfo(logStr = "最大oom_score_adj: $defaultMaxAdjStr")

            // 计算最小的、要进行优化的资源占用的值
            RunningInfo.getInstance().memInfoReader?.let { memInfoReader ->
                minOptimizeRssInBytes =
                    (memInfoReader.getTotalSize() * minOptimizeRssFactor).coerceAtMost(
                        maxOptimizeRssInBytes
                    )
                minOptimizeRssInMBytesStr = "${
                    DecimalFormat(".00").apply {
                        roundingMode = RoundingMode.DOWN
                    }.format(minOptimizeRssInBytes / (1024 * 1024))
                }MB"
                logInfo(logStr = "触发优化的内存阈值 = $minOptimizeRssInMBytesStr")
            }
        }

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            appInfo: AppInfo,
            @OriginalObject processRecord: Any,
            pid: Int = getPid(processRecord),
            uid: Int = getUID(processRecord),
            userId: Int = getUserId(processRecord),
            packageName: String = getPkgName(processRecord),
        ): ProcessRecord {
            return ProcessRecordHelper.instanceCreator(processRecord).apply {
                this.uid = uid
                this.userId = userId
                this.pid = pid
                this.packageName = packageName
                this.processName = getProcessName(processRecord)

                this.processStateRecord = ProcessStateRecordHelper.instanceCreator(processRecord)

                this.mainProcess = isMainProcess(packageName, processName).ifTrue {
                    appInfo.mProcessRecord = this
                }
                this.webviewProcessProbable = isWebviewProcProbable(processRecord)

                this.appInfo = appInfo

                initAdjHandleType()
            }
        }

        /**
         * 获取进程的用户id
         *
         * @param processRecord 安卓的进程记录
         */
        @JvmStatic
        fun getUserId(@OriginalObject processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.userId)
        }

        /**
         * 获取包名
         *
         * @param processRecord 安卓源码中的进程记录器
         * @return 包名
         */
        @JvmStatic
        fun getPkgName(@OriginalObject processRecord: Any): String {
            return getApplicationInfo(processRecord).packageName
        }

        /**
         * 获取进程名
         *
         * @param processRecord 安卓的ProcessRecord 对象
         * @return 进程名
         */
        @JvmStatic
        fun getProcessName(@OriginalObject processRecord: Any): String {
            return processRecord.getStringFieldValue(FieldConstants.processName, "")!!
        }

        @JvmStatic
        fun isProcessNameSame(expectProcName: String, @OriginalObject processRecord: Any): Boolean {
            return expectProcName == getProcessName(processRecord)
        }

        @JvmStatic
        fun getUID(@OriginalObject processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.uid)
        }

        /**
         * 获取pid
         *
         * @param instance 安卓ProcessRecord
         */
        @JvmStatic
        override fun getPid(@OriginalObject instance: Any): Int {
            return ProcessRecordHelper.callStaticMethod(
                method = IProcessRecord::getPid,
                instance
            )
        }

        /**
         * 获取[ApplicationInfo]
         *
         * @param processRecord Any
         */
        @JvmStatic
        fun getApplicationInfo(@OriginalObject processRecord: Any): ApplicationInfo {
            return processRecord.getObjectFieldValue(FieldConstants.info) as ApplicationInfo
        }

        /**
         * The process ID which will be set when we're killing this process.
         *
         * @param processRecord Any 安卓ProcessRecord
         * @return Int mDyingPid
         */
        @JvmStatic
        fun getMDyingPid(@OriginalObject processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.mDyingPid)
        }

        /**
         * 设置实际的oom_adj_score
         *
         * @param collection 待设置的集合
         */
        @JvmStatic
        fun setActualAdj(collection: Collection<ProcessRecord>) {
            collection.forEach {
                it.curAdj = it.getCurAdjNative()
            }
        }

        /**
         * 获取当前oom_adj_score
         *
         * @param pid 要查询的进程的pid
         * @return 进程的oom_adj_score
         */
        @JvmStatic
        fun getCurAdjNative(pid: Int): Int {
            val longOut = LongArray(1)
            Process.readProcFile(
                "/proc/${pid}/oom_score_adj",
                LONG_FORMAT,
                null,
                longOut,
                null
            )
            return longOut[0].toInt()
        }

        /**
         * 获取完整进程名
         *
         * @param processRecord 安卓的ProcessRecord
         * @return 包名:进程名
         */
        @JvmStatic
        fun getFullProcessName(@OriginalObject processRecord: Any): String {
            return PackageUtils.absoluteProcessName(
                getPkgName(processRecord),
                getProcessName(processRecord)
            )
        }

        /* *************************************************************************
         *                                                                         *
         * webview进程                                                              *
         *                                                                         *
         **************************************************************************/
        /**
         * 是否是webview进程的判断结果<进程名, 结果(是->true)>
         */
        private val webviewProcessNameMap = ConcurrentHashMap<String, Boolean>(8)

        /**
         * 是否是webview进程
         * @param processRecord Any 安卓的ProcessRecord对象
         * @return Boolean 是 -> true
         */
        @JvmStatic
        fun isWebviewProc(@OriginalObject processRecord: Any): Boolean {
            return getProcessName(processRecord).contains("SandboxedProcessService")
        }

        @JvmStatic
        fun isWebviewProcProbable(@OriginalObject processRecord: Any): Boolean {
            return webviewProcessNameMap.computeIfAbsent(getProcessName(processRecord)) { processName ->
                // 该方案会匹配到: sandboxed_privilege_process
                /*val index = processName.lastIndexOf("sandbox", ignoreCase = true)
                val index2 = processName.lastIndexOf("process", ignoreCase = true)
                index in 0..<index2*/
                // 可以使用uid匹配。但总有写家伙喜欢自己带个webview
                processName.contains(
                    "SandboxedProcessService"/* 标准名字 */,
                    ignoreCase = true
                ) || processName.contains(
                    "SandboxProcess"/* 百度地图(无语, 又不太无语) */,
                    ignoreCase = true
                ) || processName.contains(
                    "SandboxedProcess"/* 万一真有这么起名的呢 */,
                    ignoreCase = true
                ) /* 其他情况去死吧, 不兼容了。搞得代码又臭又长, 写一堆匹配 */
            }
        }

        @JvmStatic
        fun getProcessCachedOptimizerRecord(
            processRecord: Any,
        ): Any? = processRecord.getObjectFieldValue(FieldConstants.mOptRecord)

        @JvmStatic
        fun getProcessStateRecord(
            processRecord: Any,
        ): Any = ProcessStateRecordHelper.getProcessStateRecordFromProcessRecord(processRecord)

        @JvmStatic
        override fun isKilledByAm(instance: Any): Boolean {
            return ProcessRecordHelper.callStaticMethod(
                method = IProcessRecord::isKilledByAm,
                instance
            )
        }

        @JvmStatic
        override fun getThread(instance: Any): Any? {
            return ProcessRecordHelper.callStaticMethod(
                method = IProcessRecord::getThread,
                instance
            )
        }

        @JvmStatic
        fun isPendingFinishAttach(processRecord: Any): Boolean = processRecord.callMethod<Boolean>(
            methodName = MethodConstants.isPendingFinishAttach
        )

        @JvmStatic
        fun getProcessServiceRecord(processRecord: Any): Any =
            ProcessServiceRecordHelper.getProcessServiceRecordFromProcessRecord(processRecord)

        @JvmStatic
        fun isIsolated(processRecord: Any): Boolean = processRecord.getBooleanFieldValue(
            fieldName = FieldConstants.isolated
        )

        @JvmStatic
        override fun getIsolatedEntryPoint(instance: Any): String? {
            return ProcessRecordHelper.callStaticMethod(
                method = IProcessRecord::getIsolatedEntryPoint,
                instance
            )
        }

        @JvmStatic
        override fun killLocked(
            instance: Any,
            reason: String,
            reasonCode: Int,
            subReason: Int,
            noisy: Boolean,
        ) {
            ProcessRecordHelper.callStaticMethod(
                method = IProcessRecord::killLocked,
                instance,
                reason,
                reasonCode,
                subReason,
                noisy
            )
        }

        @JvmStatic
        @OriginalObject(since = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun isSdkSandbox(processRecord: Any): Boolean = processRecord.getBooleanFieldValue(
            fieldName = FieldConstants.isSdkSandbox
        )

        @JvmStatic
        fun getActiveInstrumentation(processRecord: Any): Any? = processRecord.callMethod(
            methodName = MethodConstants.getActiveInstrumentation
        )

        @JvmStatic
        fun resetAdjHandleType(
            packageNameFilter: ((ProcessRecord) -> Boolean)? = null,
            processNameFilter: ((ProcessRecord) -> Boolean)? = null,
        ) {
            val runningInfo = RunningInfo.getInstance()
            runningInfo.runningProcessList.asSequence()
                .nullableFilter(packageNameFilter)
                .nullableFilter(processNameFilter)
                .forEach { processRecord ->
                    processRecord.resetAdjHandleType()
                }
        }

        @JvmStatic
        fun resetAdjHandleType(packageName: String, processName: String? = null) {
            if (BuildConfig.DEBUG) {
                logDebug("重新计算adj处理策略: packageName: ${packageName}, processName: ${processName}")
            }

            resetAdjHandleType(
                packageNameFilter = { processRecord ->
                    processRecord.packageName == packageName
                },
                processNameFilter = processName?.let {
                    { processRecord ->
                        processRecord.getFullProcessName() == processName
                    }
                }
            )
        }
    }
}

interface IProcessRecord : IEntityCompatRule {
    fun getPid(@OriginalObject instance: Any): Int

    fun getThread(@OriginalObject instance: Any): Any?

    fun getIsolatedEntryPoint(@OriginalObject instance: Any): String?

    fun isKilledByAm(@OriginalObject instance: Any): Boolean

    fun killLocked(
        @OriginalObject instance: Any,
        reason: String,
        reasonCode: Int,
        subReason: Int,
        noisy: Boolean,
    )
}

fun ProcessRecord.isMainProcess(packageName: String, processName: String): Boolean =
    packageName == processName

fun ProcessRecord.isMainProcess(): Boolean = isMainProcess(this.packageName, this.processName)

fun ProcessRecord.setMainProcess() {
    runCatchThrowable {
        this.mainProcess = isMainProcess()
    }
}

/**
 * 若该进程创建时, app处于IDLE, 则将此进程添加到待压缩列表
 * @receiver ProcessRecordKt
 * @param runningInfo RunningInfo
 * @param appInfo AppInfo
 */
fun ProcessRecord.addCompactProcess(
    runningInfo: RunningInfo,
    appInfo: AppInfo,
) {
    // 若该进程创建时, app处于IDLE且当前app不是桌面, 则将此进程添加到待压缩列表
    if (AppGroupEnum.IDLE == appInfo.appGroupEnum && runningInfo.activeLaunchPackageName != appInfo.packageName) {
        this.appInfo = appInfo
        addCompactProcess(runningInfo)
    }
}

/**
 * 检查当前进程记录是否合法
 *
 * @return 合法 -> true
 */
fun ProcessRecord.isValid(runningInfo: RunningInfo): Boolean {
    // pid是否合法
    if (this.pid <= 0) {
        return false
    }

    // 当前应用内存分组
    if (this.appInfo.appGroupEnum == AppGroupEnum.DEAD) {
        return false
    }

    // 子进程在当前运行的app列表中来判断
    runningInfo.getRunningProcess(this.pid) ?: return false

    return !this.mKilledByAm
}

/**
 * processRecord.pid纠正
 * @receiver ProcessRecordKt? 要纠正的ProcessRecord
 */
fun ProcessRecord?.correctProcessPid() {
    if (this == null || this.pid > 0) {
        return
    }

    var curCorrectTimes = 1
    while (curCorrectTimes <= 10) {
        this.pid = ProcessRecord.getPid(this.originalInstance)
        curCorrectTimes++
        if (this.pid <= 0) {
            TimeUnit.MILLISECONDS.sleep(20)
        }
    }
}
