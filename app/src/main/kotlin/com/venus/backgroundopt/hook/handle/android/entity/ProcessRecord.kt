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

package com.venus.backgroundopt.hook.handle.android.entity

import android.content.pm.ApplicationInfo
import android.os.Build
import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.annotation.AndroidMethod
import com.venus.backgroundopt.annotation.AndroidObject
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.entity.preference.OomWorkModePref
import com.venus.backgroundopt.environment.hook.HookCommonProperties
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.Process.PROC_NEWLINE_TERM
import com.venus.backgroundopt.hook.handle.android.entity.Process.PROC_OUT_LONG
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getBooleanFieldValue
import com.venus.backgroundopt.utils.getIntFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.getStringFieldValue
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.log.logInfo
import com.venus.backgroundopt.utils.runCatchThrowable
import com.venus.backgroundopt.utils.setIntFieldValue
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Objects
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * @author XingC
 * @date 2023/9/26
 */
class ProcessRecord(
    @AndroidObject(classPath = ClassConstants.ProcessRecord)
    @get:JSONField(serialize = false)
    override val originalInstance: Any,
) : BaseProcessInfoKt(), ILogger, IAndroidEntity {
    companion object {
        val LONG_FORMAT by lazy {
            intArrayOf(PROC_NEWLINE_TERM or PROC_OUT_LONG)
        }

        // 默认的主进程要设置的adj
        const val DEFAULT_MAIN_ADJ = 0

        // 默认的子进程要设置的adj
        const val SUB_PROC_ADJ = ProcessList.VISIBLE_APP_ADJ + 1

        // 默认的最大adj
        var defaultMaxAdj = ProcessList.UNKNOWN_ADJ

        // 高优先级子进程的最大adj
        var HIGH_PRIORITY_SUB_PROC_DEFAULT_MAX_ADJ = ProcessList.UNKNOWN_ADJ

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
                OomWorkModePref.MODE_BALANCE_PLUS -> {
                    isNeedSetDefaultMaxAdj = true

                    defaultMaxAdj = ProcessList.PERCEPTIBLE_RECENT_FOREGROUND_APP_ADJ
                    // 高优先级子进程
                    HIGH_PRIORITY_SUB_PROC_DEFAULT_MAX_ADJ = ProcessList.VISIBLE_APP_ADJ
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
        fun newInstance(
            runningInfo: RunningInfo,
            appInfo: AppInfo,
            @AndroidObject processRecord: Any
        ): ProcessRecord {
            val record = ProcessRecord(runningInfo.activityManagerService, processRecord)
            // addCompactProcess(runningInfo, appInfo, record)
            return record
        }

        @JvmStatic
        @JvmOverloads
        fun newInstance(
            activityManagerService: ActivityManagerService,
            appInfo: AppInfo,
            @AndroidObject processRecord: Any,
            pid: Int = getPid(processRecord),
            uid: Int = getUID(processRecord),
            userId: Int = getUserId(processRecord),
            packageName: String = getPkgName(processRecord)
        ): ProcessRecord = ProcessRecord(
            activityManagerService = activityManagerService,
            processRecord = processRecord,
            pid = pid,
            uid = uid,
            userId = userId,
            packageName = packageName,
        ).apply {
            this.appInfo = appInfo

            if (mainProcess) {
                this.appInfo.setmProcessRecord(this)
            }
        }

        /**
         * 获取进程的用户id
         *
         * @param processRecord 安卓的进程记录
         */
        @JvmStatic
        fun getUserId(@AndroidObject processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.userId)
        }

        /**
         * 获取包名
         *
         * @param processRecord 安卓源码中的进程记录器
         * @return 包名
         */
        @JvmStatic
        fun getPkgName(@AndroidObject processRecord: Any): String {
            return (processRecord.getObjectFieldValue(FieldConstants.info) as ApplicationInfo).packageName
        }

        /**
         * 获取进程名
         *
         * @param processRecord 安卓的ProcessRecord 对象
         * @return 进程名
         */
        @JvmStatic
        fun getProcessName(@AndroidObject processRecord: Any): String {
            return processRecord.getStringFieldValue(FieldConstants.processName, "")!!
        }

        @JvmStatic
        fun isProcessNameSame(expectProcName: String, @AndroidObject processRecord: Any): Boolean {
            return expectProcName == getProcessName(processRecord)
        }

        @JvmStatic
        fun getUID(@AndroidObject processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.uid)
        }

        /**
         * 获取pid
         *
         * @param processRecord 安卓ProcessRecord
         */
        @JvmStatic
        fun getPid(@AndroidObject processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.mPid)
        }

        /**
         * 获取[ApplicationInfo]
         *
         * @param processRecord Any
         */
        @JvmStatic
        fun getApplicationInfo(@AndroidObject processRecord: Any): ApplicationInfo {
            return processRecord.getObjectFieldValue(FieldConstants.info) as ApplicationInfo
        }

        /**
         * The process ID which will be set when we're killing this process.
         *
         * @param processRecord Any 安卓ProcessRecord
         * @return Int mDyingPid
         */
        @JvmStatic
        fun getMDyingPid(@AndroidObject processRecord: Any): Int {
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
        fun getFullProcessName(@AndroidObject processRecord: Any): String {
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
        fun isWebviewProc(@AndroidObject processRecord: Any): Boolean {
            return getProcessName(processRecord).contains("SandboxedProcessService")
        }

        @JvmStatic
        fun isWebviewProcProbable(@AndroidObject processRecord: Any): Boolean {
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
            processRecord: Any
        ): Any? = processRecord.getObjectFieldValue(FieldConstants.mOptRecord)

        @JvmStatic
        fun getProcessStateRecord(
            processRecord: Any
        ): Any = processRecord.getObjectFieldValue(FieldConstants.mState)!!

        @JvmStatic
        @AndroidMethod
        fun isKilledByAm(processRecord: Any): Boolean = processRecord.callMethod<Boolean>(
            methodName = MethodConstants.isKilledByAm
        )

        @JvmStatic
        @AndroidMethod
        fun getThread(processRecord: Any): Any? = processRecord.callMethod(
            methodName = MethodConstants.getThread
        )

        @JvmStatic
        @AndroidMethod
        fun isPendingFinishAttach(processRecord: Any): Boolean = processRecord.callMethod<Boolean>(
            methodName = MethodConstants.isPendingFinishAttach
        )

        @JvmStatic
        fun getProcessServiceRecord(processRecord: Any): Any = processRecord.getObjectFieldValue(
            fieldName = FieldConstants.mServices
        )!!

        @JvmStatic
        fun isIsolated(processRecord: Any): Boolean = processRecord.getBooleanFieldValue(
            fieldName = FieldConstants.isolated
        )

        @JvmStatic
        @AndroidMethod
        fun getIsolatedEntryPoint(processRecord: Any): Any? = processRecord.callMethod(
            methodName = MethodConstants.getIsolatedEntryPoint
        )

        @JvmStatic
        @AndroidMethod
        fun killLocked(
            processRecord: Any,
            reason: String,
            reasonCode: Int,
            subReason: Int,
            noisy: Boolean
        ) {
            processRecord.callMethod(
                methodName = MethodConstants.killLocked,
                reason,
                reasonCode,
                subReason,
                noisy
            )
        }

        @JvmStatic
        @AndroidObject(since = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun isSdkSandbox(processRecord: Any): Boolean = processRecord.getBooleanFieldValue(
            fieldName = FieldConstants.isSdkSandbox
        )

        @JvmStatic
        @AndroidMethod
        fun getActiveInstrumentation(processRecord: Any): Any? = processRecord.callMethod(
            methodName = MethodConstants.getActiveInstrumentation
        )

        /**
         * 进程的int值消费者
         */
        /*private val processIntValueConsumer =
            { process: ProcessRecordKt, int: Int, boolwan: Boolean ->

            }*/

        /**
         * 默认的用来设置[ProcessRecord.fixedOomAdjScore]的设置器
         */
        /*private val defaultFixedOomScoreAdjSetter =
            { process: ProcessRecordKt, oomScoreAdj: Int, isAdjustMaxAdj: Boolean ->
                process.fixedOomAdjScore = oomScoreAdj
                if (isAdjustMaxAdj) {
                    if (CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_STRICT ||
                        CommonProperties.oomWorkModePref.oomMode == OomWorkModePref.MODE_NEGATIVE
                    ) {
                        process.setDefaultMaxAdj()
                    }
                }
                // 执行完本次之后就清除掉设置器
                process.clearFixedOomScoreAdjSetter()
            }*/
    }

    // 反射拿到的安卓的processStateRecord对象
    @AndroidObject(classPath = ClassConstants.ProcessStateRecord)
    @JSONField(serialize = false)
    lateinit var processStateRecord: ProcessStateRecord

    // 当前ProcessRecord已记录的最大adj
    @JSONField(serialize = false)
    var recordMaxAdj = 0

    /**
     * All about the state info of the optimizer when the process is cached.
     */
    @JSONField(serialize = false)
    var processCachedOptimizerRecord: ProcessCachedOptimizerRecord? = null
        get() {
            if (field == null) {
                processCachedOptimizerRecord = ProcessCachedOptimizerRecord(
                    originalInstance.getObjectFieldValue(FieldConstants.mOptRecord)
                )
            }
            return field
        }

    @JSONField(serialize = false)
    private lateinit var activityManagerService: ActivityManagerService

    @JSONField(serialize = false)
    lateinit var appInfo: AppInfo

    @get:JSONField(serialize = false)
    val mKilledByAm: Boolean
        get() {
            return originalInstance.callMethod<Boolean>(MethodConstants.isKilledByAm)
        }

    constructor(
        activityManagerService: ActivityManagerService,
        @AndroidObject processRecord: Any,
        pid: Int,
        uid: Int,
        userId: Int,
        packageName: String
    ) : this(processRecord) {
        this.activityManagerService = activityManagerService
        this.pid = pid
        this.uid = uid
        this.userId = userId
        this.packageName = packageName

        processName = getProcessName(processRecord)
        mainProcess = isMainProcess(packageName, processName)
        webviewProcess = isWebviewProc(processRecord)
        webviewProcessProbable = isWebviewProcProbable(processRecord)
        processStateRecord =
            ProcessStateRecord(getProcessStateRecord(processRecord = processRecord))
    }

    constructor(
        activityManagerService: ActivityManagerService,
        @AndroidObject processRecord: Any
    ) : this(
        activityManagerService,
        processRecord,
        getPid(processRecord),
        getUID(processRecord),
        getUserId(processRecord),
        getApplicationInfo(processRecord).packageName
    )

    /**
     * 设置默认的最大adj
     */
    @JSONField(serialize = false)
    fun setDefaultMaxAdj() {
        if (mainProcess) {
            setMaxAdj(defaultMaxAdj)
        } else {
            setMaxAdj(HIGH_PRIORITY_SUB_PROC_DEFAULT_MAX_ADJ)
        }
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
        var setSucceed = false
        try {
            processStateRecord.maxAdj = maxAdj
            setSucceed = true
        } catch (t: Throwable) {
            try {
                processStateRecord.processStateRecord.setIntFieldValue(
                    FieldConstants.mMaxAdj,
                    maxAdj
                )
                setSucceed = true
            } catch (ignore: Throwable) {
            }
        }
        recordMaxAdj = if (setSucceed) maxAdj else ProcessList.UNKNOWN_ADJ
    }

    /**
     * 是否已经对该进程设置过maxAdj
     *
     * @return
     */
    fun hasSetMaxAdj(): Boolean = recordMaxAdj != 0

    /**
     * 重置进程的maxAdj
     *
     */
    fun resetMaxAdj() {
        if (hasSetMaxAdj()) {
            try {
                processStateRecord.maxAdj = ProcessList.UNKNOWN_ADJ
                recordMaxAdj = 0
                if (BuildConfig.DEBUG) {
                    logger.debug("pid: [${pid}] >>> maxAdj重置成功")
                }
            } catch (t: Throwable) {
                logger.error("pid: [${pid}] >>> maxAdj重置失败", t)
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
        return try {
            processStateRecord.maxAdj
        } catch (t: Throwable) {
            try {
                processStateRecord.processStateRecord.getIntFieldValue(FieldConstants.mMaxAdj)
            } catch (th: Throwable) {
                ProcessList.UNKNOWN_ADJ
            }
        }
    }

    /**
     * 设置trim
     *
     * @param level trim级别([ComponentCallbacks2])
     * @return 成功设置 -> true
     */
    fun scheduleTrimMemory(level: Int): Boolean {
//        XposedHelpers.callMethod(mThread, MethodConstants.scheduleTrimMemory, level);
        val thread: Any? = try {
            getThread(originalInstance)
        } catch (ignore: Throwable) {
            null
        }
        thread?.let { t ->
            t.callMethod(MethodConstants.scheduleTrimMemory, level)
            return true
        } ?: return false
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

    @JSONField(serialize = false)
    fun getCurAdjNative(): Int {
        return Companion.getCurAdjNative(pid)
    }

    @JSONField(serialize = false)
    fun getFullProcessName(): String {
        return PackageUtils.absoluteProcessName(packageName, processName)
    }

    /* *************************************************************************
     *                                                                         *
     * 进程内存调节                                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * 获取当前的资源占用
     * @return Long 正常获取的值 >= 0, 获取异常 = Long.MIN_VALUE
     */
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
    fun isAllowedCompact(time: Long): Boolean {
        return time - getLastCompactTime() > compactInterval
    }

    @JvmName("getOomAdjScoreFromAtomicInteger")
    @JSONField(serialize = false)
    fun getOomAdjScore(): Int {
        return oomAdjScoreAtomicInteger.get()
    }

    @JvmName("setOomAdjScoreToAtomicInteger")
    @JSONField(serialize = false)
    fun setOomAdjScore(oomAdjScore: Int) {
        this.oomAdjScoreAtomicInteger.set(oomAdjScore)
        this.oomAdjScore = oomAdjScore
    }

    @JSONField(serialize = false)
    private fun getLastCompactTime(): Long {
        return lastCompactTimeAtomicLong.get()
    }

    @JSONField(serialize = false)
    fun setLastCompactTime(time: Long) {
        lastCompactTimeAtomicLong.set(time)
    }

    /**
     * [fixedOomAdjScore]设置器
     *
     * 在设置一次之后就废弃掉。在[resetMaxAdj]中重置
     */
    /*@Volatile
    @JSONField(serialize = false)
    var fixedOomScoreAdjSetter = defaultFixedOomScoreAdjSetter

    private fun clearFixedOomScoreAdjSetter() {
        fixedOomScoreAdjSetter = processIntValueConsumer
    }

    private fun resetFixedOomScoreAdjSetter() {
        fixedOomScoreAdjSetter = defaultFixedOomScoreAdjSetter
    }*/

    /* *************************************************************************
     *                                                                         *
     * 进程拥有的唤醒锁的数量                                                       *
     *                                                                         *
     **************************************************************************/
    @JSONField(serialize = false)
    private var _wakeLockCount = AtomicInteger(0)

    @get:JSONField(serialize = false)
    val wakeLockCount: Int
        get() {
            return _wakeLockCount.get()
        }

    fun incrementWakeLockCount() {
        _wakeLockCount.incrementAndGet()
    }

    fun decrementWakeLockCount() {
        _wakeLockCount.decrementAndGet()
    }

    @JSONField(serialize = false)
    fun hasWakeLock(): Boolean = wakeLockCount > 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ProcessRecord
        return uid == that.uid && pid == that.pid && processName == that.processName && packageName == that.packageName
    }

    override fun hashCode(): Int {
        return Objects.hash(uid, pid, processName, packageName)
    }
}

fun ProcessRecord.isMainProcess(
    packageName: String,
    processName: String
): Boolean = packageName == processName

fun ProcessRecord.isMainProcess(): Boolean {
    return isMainProcess(this.packageName, this.processName)
}

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
