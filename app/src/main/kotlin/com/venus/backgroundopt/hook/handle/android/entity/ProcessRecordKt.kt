package com.venus.backgroundopt.hook.handle.android.entity

import android.content.pm.ApplicationInfo
import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.core.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.Process.PROC_NEWLINE_TERM
import com.venus.backgroundopt.hook.handle.android.entity.Process.PROC_OUT_LONG
import com.venus.backgroundopt.utils.PackageUtils
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getIntFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.getStringFieldValue
import com.venus.backgroundopt.utils.log.ILogger
import com.venus.backgroundopt.utils.setIntFieldValue
import java.util.Objects
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * @author XingC
 * @date 2023/9/26
 */
class ProcessRecordKt() : BaseProcessInfoKt(), ILogger {
    companion object {
        val LONG_FORMAT by lazy {
            intArrayOf(PROC_NEWLINE_TERM or PROC_OUT_LONG)
        }

        // 默认的最大adj
        const val DEFAULT_MAX_ADJ = ProcessList.VISIBLE_APP_ADJ

        // 默认的主进程要设置的adj
        const val DEFAULT_MAIN_ADJ = ProcessList.FOREGROUND_APP_ADJ

        // 默认的子进程要设置的adj
        const val SUB_PROC_ADJ = DEFAULT_MAX_ADJ + 1

        @JvmStatic
        fun newInstance(
            runningInfo: RunningInfo,
            appInfo: AppInfo,
            processRecord: Any
        ): ProcessRecordKt {
            val record = ProcessRecordKt(runningInfo.activityManagerService, processRecord)
            addCompactProcess(runningInfo, appInfo, record)
            return record
        }

        @JvmStatic
        fun setMainProcess(processRecord: ProcessRecordKt): ProcessRecordKt {
            try {
                processRecord.mainProcess = isMainProcess(processRecord)
            } catch (ignore: Exception) {
            }
            return processRecord
        }

        @JvmStatic
        fun isMainProcess(processRecord: ProcessRecordKt): Boolean {
            return isMainProcess(processRecord.packageName, processRecord.processName)
        }

        @JvmStatic
        fun isMainProcess(packageName: String, processName: String): Boolean {
            return packageName == processName
        }

        /**
         * 若该进程创建时, app处于IDLE, 则将此进程添加到待压缩列表
         *
         * @param runningInfo   运行信息
         * @param appInfo       应用信息
         * @param processRecord 进程记录
         */
        @JvmStatic
        fun addCompactProcess(
            runningInfo: RunningInfo,
            appInfo: AppInfo,
            processRecord: ProcessRecordKt
        ) {
            // 若该进程创建时, app处于IDLE且当前app不是桌面, 则将此进程添加到待压缩列表
            if (AppGroupEnum.IDLE == appInfo.appGroupEnum && runningInfo.activeLaunchPackageName != appInfo.packageName) {
                processRecord.appInfo = appInfo
                processRecord.addCompactProcess(runningInfo)
            }
        }

        /**
         * 获取进程的用户id
         *
         * @param processRecord 安卓的进程记录
         */
        @JvmStatic
        fun getUserId(processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.userId)
        }

        /**
         * 获取包名
         *
         * @param processRecord 安卓源码中的进程记录器
         * @return 包名
         */
        @JvmStatic
        fun getPkgName(processRecord: Any): String {
            return (processRecord.getObjectFieldValue(FieldConstants.info) as ApplicationInfo).packageName
        }

        /**
         * 获取进程名
         *
         * @param processRecord 安卓的ProcessRecord 对象
         * @return 进程名
         */
        @JvmStatic
        fun getProcessName(processRecord: Any): String {
            return processRecord.getStringFieldValue(FieldConstants.processName, "")!!
        }

        @JvmStatic
        fun isProcessNameSame(expectProcName: String, processRecord: Any): Boolean {
            return expectProcName == getProcessName(processRecord)
        }

        @JvmStatic
        fun getUID(processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.uid)
        }

        /**
         * 获取pid
         *
         * @param processRecord 安卓ProcessRecord
         */
        @JvmStatic
        fun getPid(processRecord: Any): Int {
            return processRecord.getIntFieldValue(FieldConstants.mPid)
        }

        /**
         * The process ID which will be set when we're killing this process.
         *
         * @param processRecord Any 安卓ProcessRecord
         * @return Int mDyingPid
         */
        @JvmStatic
        fun getMDyingPid(processRecord: Any):Int {
            return processRecord.getIntFieldValue(FieldConstants.mDyingPid)
        }

        /**
         * 设置实际的oom_adj_score
         *
         * @param collection 待设置的集合
         */
        @JvmStatic
        fun setActualAdj(collection: Collection<ProcessRecordKt>) {
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
        fun getFullProcessName(processRecord: Any): String {
            return PackageUtils.absoluteProcessName(
                getPkgName(processRecord),
                getProcessName(processRecord)
            )
        }

        /**
         * 检查当前进程记录是否合法
         *
         * @return 合法 -> true
         */
        @JvmStatic
        fun isValid(runningInfo: RunningInfo, processRecord: ProcessRecordKt): Boolean {
            // pid是否合法
            if (processRecord.pid <= 0) {
                return false
            }
            // 主进程则查看当前应用内存分组
            if (processRecord.mainProcess) {
                return processRecord.appInfo.appGroupEnum != AppGroupEnum.DEAD
            }
            // 子进程在当前运行的app列表中来判断
            return runningInfo.getRunningAppInfo(processRecord.uid)
                ?.getProcess(processRecord.pid) != null
        }

        /**
         * processRecord.pid纠正
         *
         * @param processRecord 要纠正的ProcessRecord
         */
        @JvmStatic
        fun correctProcessPid(processRecord: ProcessRecordKt?) {
            if (processRecord == null || processRecord.pid > 0) {
                return
            }

            var curCorrectTimes = 1
            while (curCorrectTimes <= 5) {
                processRecord.pid = getPid(processRecord.processRecord)
                curCorrectTimes++
                if (processRecord.pid <= 0) {
                    TimeUnit.MILLISECONDS.sleep(20)
                }
            }
        }
    }

    // 反射拿到的安卓的processRecord对象
    @JSONField(serialize = false)
    lateinit var processRecord: Any

    // 反射拿到的安卓的processStateRecord对象
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
                    processRecord.getObjectFieldValue(FieldConstants.mOptRecord)
                )
            }
            return field
        }

    @JSONField(serialize = false)
    private lateinit var activityManagerService: ActivityManagerService

    @JSONField(serialize = false)
    lateinit var appInfo: AppInfo

    constructor(activityManagerService: ActivityManagerService, processRecord: Any) : this() {
        this.processRecord = processRecord
        pid = getPid(processRecord)
        uid = getUID(processRecord)
        userId = getUserId(processRecord)
        val applicationInfo =
            processRecord.getObjectFieldValue(FieldConstants.info) as ApplicationInfo
        packageName = applicationInfo.packageName
        processName = getProcessName(processRecord)
        processStateRecord =
            ProcessStateRecord(processRecord.getObjectFieldValue(FieldConstants.mState))
        this.activityManagerService = activityManagerService
        mainProcess = isMainProcess(packageName, processName)
    }

    /**
     * 设置默认的最大adj
     */
    @JSONField(serialize = false)
    fun setDefaultMaxAdj() {
        setMaxAdj(DEFAULT_MAX_ADJ)
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
            processRecord.callMethod(MethodConstants.getThread)
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
    fun getFullPackageName(): String {
        return PackageUtils.absoluteProcessName(packageName, processName)
    }

    /* *************************************************************************
     *                                                                         *
     * 进程内存调节                                                              *
     *                                                                         *
     **************************************************************************/
    @JSONField(serialize = false)
    var rssInBytes = Long.MIN_VALUE

    fun updateRssInBytes() {
        rssInBytes =
            MemoryStatUtil.readMemoryStatFromFilesystem(uid, pid)?.rssInBytes ?: Long.MIN_VALUE
    }

    /**
     * 先获当前值, 再更新值
     *
     * @return 返回这次更新前的值
     */
    @JSONField(serialize = false)
    fun getAndUpdateRssInBytes(): Long {
        val bytes = rssInBytes
        // 更新
        updateRssInBytes()
        return bytes
    }

    /**
     * 是否需要应用内存调整
     *
     * @return true if necessary
     */
    @JSONField(serialize = false)
    fun isNecessaryToOptimize(): Boolean {
        val bytes = getAndUpdateRssInBytes()
        var b = true
        // 只有成功获取当前已用内存时才进行按比率判断, 其他情况直接默认处理
        if (bytes != Long.MIN_VALUE) {
            MemoryStatUtil.readMemoryStatFromFilesystem(uid, pid)?.let { memoryStat ->
                b = (memoryStat.rssInBytes / bytes.toDouble()) > 0.5
            }
        }
        return b
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ProcessRecordKt
        return uid == that.uid && pid == that.pid && processName == that.processName && packageName == that.packageName
    }

    override fun hashCode(): Int {
        return Objects.hash(uid, pid, processName, packageName)
    }
}