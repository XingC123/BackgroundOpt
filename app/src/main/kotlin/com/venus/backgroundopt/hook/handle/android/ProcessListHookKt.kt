package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/22
 */
class ProcessListHookKt(
    classLoader: ClassLoader?,
    hookInfo: RunningInfo?
) : MethodHook(classLoader, hookInfo) {
    companion object {
        @JvmField
        val processedAppGroup = arrayOf(AppGroupEnum.NONE, AppGroupEnum.IDLE)
    }

    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.ProcessList,
                MethodConstants.setOomAdj,
                arrayOf(
                    beforeHookAction { handleSetOomAdj(it) }
                ),
                Int::class.javaPrimitiveType,   // pid
                Int::class.javaPrimitiveType,   // uid
                Int::class.javaPrimitiveType    // oom_adj_score
            ),
        )
    }

    /**
     * 是否升级子进程的等级
     *
     * @param processName 进程名
     * @return 升级 -> true
     */
    private fun isUpgradeSubProcessLevel(processName: String): Boolean =
        CommonProperties.subProcessOomPolicyMap[processName]?.let {
            it.policyEnum == SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
        } ?: false

    private fun logProcessOomChanged(
        packageName: String,
        uid: Int,
        pid: Int,
        mainProcess: Boolean = false,
        curAdj: Int
    ) = logger.debug(
        "设置${if (mainProcess) "主" else "子"}进程: [${packageName}, uid: ${uid}] ->>> " +
                "pid: ${pid}, adj: $curAdj"
    )

    private fun handleSetOomAdj(param: MethodHookParam) {
        val uid = param.args[1] as Int
        val runningInfo = runningInfo
        val appInfo = runningInfo.computeRunningAppIfAbsent(uid) ?: return
        // 若app未进入后台, 则不进行设置
        if (appInfo.appGroupEnum !in processedAppGroup) {
            return
        }

        val pid = param.args[0] as Int
        val oomAdjScore = param.args[2] as Int
        /*var mPid = Int.MIN_VALUE
        try {
            mPid = appInfo.getmPid()
        } catch (e: Exception) {
            runningInfo.addRunningApp(appInfo)
            try {
                mPid = appInfo.getmPid()
                if (BuildConfig.DEBUG) {
                    logger.debug("[${appInfo.packageName}, uid: ${uid}]的主进程信息补全完毕")
                }
            } catch (ex: Exception) {
                if (BuildConfig.DEBUG) {
                    logger.warn(
                        "获取: [${appInfo.packageName}, uid: ${uid}] 的mPid出错",
                        ex
                    )
                }
            }
        }*/

        // 获取当前进程对象
        var process =
            appInfo.getProcess(pid)
                ?: appInfo.addProcess(runningInfo.activityManagerService.getProcessRecord(pid))
        val mainProcess = process.mainProcess.also { b ->
            if (b) {
                appInfo.correctMainProcess(pid)?.let { process = it }
            }
        }

        if (mainProcess || isUpgradeSubProcessLevel(process.processName)) { // 主进程
            if (process.fixedOomAdjScore != ProcessRecordKt.DEFAULT_MAIN_ADJ) {
                process.oomAdjScore = oomAdjScore
                process.fixedOomAdjScore = ProcessRecordKt.DEFAULT_MAIN_ADJ
                process.setDefaultMaxAdj()

                param.args[2] = ProcessRecordKt.DEFAULT_MAIN_ADJ

                if (BuildConfig.DEBUG) {
                    logProcessOomChanged(
                        appInfo.packageName,
                        uid,
                        pid,
                        mainProcess,
                        param.args[2] as Int
                    )
                }
            } else {
                param.result = null
                appInfo.modifyProcessRecord(pid, oomAdjScore)
            }
        } /*else if (mPid == Int.MIN_VALUE) {
            if (BuildConfig.DEBUG) {
                logger.warn("${appInfo.packageName}, uid: $uid 的pid = $mPid 不符合规范, 无法添加至进程列表")
            }
            return
        } */ else { // 子进程的处理
            if (process.fixedOomAdjScore != ProcessRecordKt.SUB_PROC_ADJ) { // 第一次记录子进程 或 进程调整策略置为默认
                val expectedOomAdjScore = ProcessRecordKt.SUB_PROC_ADJ
                val finalOomAdjScore = if (oomAdjScore > expectedOomAdjScore) {
                    oomAdjScore
                } else {
                    param.args[2] = expectedOomAdjScore
                    expectedOomAdjScore
                }

                process.oomAdjScore = finalOomAdjScore
                process.fixedOomAdjScore = expectedOomAdjScore

                // 如果修改过maxAdj则重置
                process.resetMaxAdj()

                if (BuildConfig.DEBUG) {
                    logProcessOomChanged(
                        appInfo.packageName,
                        uid,
                        pid,
                        mainProcess,
                        finalOomAdjScore
                    )
                }
            } else {
                // 新的oomAdj小于修正过的adj 或 修正过的adj为不可能取值
                if (oomAdjScore < process.fixedOomAdjScore) {
                    param.result = null
                }
                appInfo.modifyProcessRecord(pid, oomAdjScore)
            }
        }
        return
    }
}