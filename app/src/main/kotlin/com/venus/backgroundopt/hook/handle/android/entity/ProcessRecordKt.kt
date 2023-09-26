package com.venus.backgroundopt.hook.handle.android.entity

import android.content.pm.ApplicationInfo
import com.alibaba.fastjson2.annotation.JSONField
import com.venus.backgroundopt.entity.AppInfo
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.entity.base.BaseProcessInfoKt
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import de.robv.android.xposed.XposedHelpers
import java.util.Objects
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * @author XingC
 * @date 2023/9/26
 */
class ProcessRecordKt() : BaseProcessInfoKt() {
    companion object {
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
            processRecord: Any?
        ): ProcessRecordKt {
            val record = ProcessRecordKt(processRecord)
            setMainProcess(appInfo, record)
            addCompactProcess(runningInfo, appInfo, record)
            return record
        }

        @JvmStatic
        fun setMainProcess(appInfo: AppInfo, processRecord: ProcessRecordKt): ProcessRecordKt {
            try {
                processRecord.mainProcess = processRecord.pid == appInfo.getmPid()
            } catch (ignore: Exception) {
            }
            return processRecord
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
            if (RunningInfo.AppGroupEnum.IDLE == appInfo.appGroupEnum) { // 若该进程创建时, app处于IDLE, 则将此进程添加到待压缩列表
                processRecord.addCompactProcess(runningInfo)
            }
        }

        /**
         * 获取进程的用户id
         *
         * @param processRecord 安卓的进程记录
         */
        @JvmStatic
        fun getUserId(processRecord: Any?): Int {
            return XposedHelpers.getIntField(processRecord, FieldConstants.userId)
        }

        /**
         * 获取包名
         *
         * @param processRecord 安卓源码中的进程记录器
         * @return 包名
         */
        @JvmStatic
        fun getPkgName(processRecord: Any?): String? {
            return (XposedHelpers.getObjectField(
                processRecord,
                FieldConstants.info
            ) as ApplicationInfo).packageName
        }

        /**
         * 获取进程名
         *
         * @param processRecord 安卓的ProcessRecord 对象
         * @return 进程名
         */
        @JvmStatic
        fun getProcessName(processRecord: Any?): String {
            return XposedHelpers.getObjectField(processRecord, FieldConstants.processName) as String
        }

        @JvmStatic
        fun isProcessNameSame(expectProcName: String, processRecord: Any?): Boolean {
            return expectProcName == getProcessName(processRecord)
        }

        @JvmStatic
        fun getUID(processRecord: Any?): Int {
            return XposedHelpers.getIntField(processRecord, FieldConstants.uid)
        }

        /**
         * 获取pid
         *
         * @param processRecord 安卓ProcessRecord
         */
        @JvmStatic
        fun getPid(processRecord: Any?): Int {
            return XposedHelpers.getIntField(processRecord, FieldConstants.mPid)
        }
    }

    // 反射拿到的安卓的processRecord对象
    @JSONField(serialize = false)
    var processRecord: Any? = null

    // 反射拿到的安卓的processStateRecord对象
    @JSONField(serialize = false)
    var processStateRecord: ProcessStateRecord? = null

    // 当前ProcessRecord已记录的最大adj
    var recordMaxAdj = 0

    /**
     * All about the state info of the optimizer when the process is cached.
     */
    @JSONField(serialize = false)
    var processCachedOptimizerRecord: ProcessCachedOptimizerRecord? = null
        get() {
            if (field == null) {
                processCachedOptimizerRecord = ProcessCachedOptimizerRecord(
                    XposedHelpers.getObjectField(processRecord, FieldConstants.mOptRecord)
                )
            }
            return field
        }

    constructor(processRecord: Any?) : this() {
        this.processRecord = processRecord
        pid = getPid(processRecord)
        uid = getUID(processRecord)
        userId = getUserId(processRecord)
        val applicationInfo =
            XposedHelpers.getObjectField(processRecord, FieldConstants.info) as ApplicationInfo
        packageName = applicationInfo.packageName
        processName = getProcessName(processRecord)
        processStateRecord =
            ProcessStateRecord(XposedHelpers.getObjectField(processRecord, FieldConstants.mState))
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
     * 在Redmi k30p MIUI13 22.7.11 (Android 12)中, 设置小于0的值(未充分测试, 只设置过-800) 且 打开的app是单进程,
     * 会导致在最近任务上划无法杀死app。
     * 在另一台机器Redmi Note5p Nusantara v5.2 official (Android安全更新2022.11.5, Android 13)中无此问题
    </pre> *
     *
     * @param maxAdj 最大adj的值
     */
    @JSONField(serialize = false)
    fun setMaxAdj(maxAdj: Int) {
        var setSucceed = false
        try {
            processStateRecord!!.maxAdj = maxAdj
            setSucceed = true
        } catch (t: Throwable) {
            try {
                XposedHelpers.setIntField(
                    processStateRecord!!.processStateRecord,
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
     * 获取进程的最大adj
     *
     * @return 进程的最大adj
     */
    @JSONField(serialize = false)
    fun getMaxAdj(): Int {
        return try {
            processStateRecord!!.maxAdj
        } catch (t: Throwable) {
            try {
                XposedHelpers.getIntField(
                    processStateRecord!!.processStateRecord, FieldConstants.mMaxAdj
                )
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
        var thread: Any? = null
        try {
            thread = XposedHelpers.callMethod(processRecord, MethodConstants.getThread)
        } catch (ignore: Throwable) {
        }
        thread ?: return false

        XposedHelpers.callMethod(thread, MethodConstants.scheduleTrimMemory, level)
        return true
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
    fun getCurAdj(): Int {
        return XposedHelpers.callMethod(processRecord, MethodConstants.getCurAdj) as Int
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
    private val compactInterval = TimeUnit.MINUTES.toMillis(7)

    fun addCompactProcess(runningInfo: RunningInfo) {
        runningInfo.processManager.addCompactProcess(this)
    }

    fun isAllowedCompact(time: Long): Boolean {
        return time - getLastCompactTime() > compactInterval
    }

    @JvmName("getOomAdjScoreFromAtomicInteger")
    fun getOomAdjScore(): Int {
        return oomAdjScoreAtomicInteger.get()
    }

    @JvmName("setOomAdjScoreToAtomicInteger")
    fun setOomAdjScore(oomAdjScore: Int) {
        this.oomAdjScoreAtomicInteger.set(oomAdjScore)
    }

    private fun getLastCompactTime(): Long {
        return lastCompactTimeAtomicLong.get()
    }

    fun setLastCompactTime(time: Long) {
        lastCompactTimeAtomicLong.set(time)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ProcessRecordKt
        return uid == that.uid && pid == that.pid && userId == that.userId && processName == that.processName && packageName == that.packageName
    }

    override fun hashCode(): Int {
        return Objects.hash(uid, pid, processName, userId, packageName)
    }
}