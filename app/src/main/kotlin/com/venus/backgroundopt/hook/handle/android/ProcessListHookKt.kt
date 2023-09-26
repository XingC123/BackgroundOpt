package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.entity.RunningInfo.AppGroupEnum
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
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
        var mPid = Int.MIN_VALUE
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
        }
        if (pid == mPid) { // 主进程
            if (appInfo.mainProcCurAdj != ProcessRecordKt.DEFAULT_MAIN_ADJ) {    // 第一次打开app
                param.args[2] = ProcessRecordKt.DEFAULT_MAIN_ADJ

                appInfo.getmProcess()?.let { processRecord ->
                    processRecord.fixedOomAdjScore = ProcessRecordKt.DEFAULT_MAIN_ADJ
                    processRecord.oomAdjScore = oomAdjScore
                }

                if (BuildConfig.DEBUG) {
                    logger.debug(
                        "设置主进程: [${appInfo.packageName}, uid: ${uid}] ->>> " +
                                "pid: ${pid}, adj: ${param.args[2]}"
                    )
                }
            } else {
                param.result = null
                appInfo.modifyProcessRecord(pid, oomAdjScore)
            }
        } else if (pid == Int.MIN_VALUE) {
            if (BuildConfig.DEBUG) {
                logger.warn("${appInfo.packageName}, uid: $uid 的pid = $pid 不符合规范, 无法添加至进程列表")
            }
            return
        } else { // 子进程的处理
            appInfo.getProcess(pid)?.let { processRecord ->
                val upgradeSubProcessLevel =
                    processRecord.getFullPackageName() in CommonProperties.WHITE_SUB_PROCESSES

                if (upgradeSubProcessLevel) {
                    param.result = null
                } else {
                    val fixedOomAdjScore = processRecord.fixedOomAdjScore
                    // 新的oomAdj小于修正过的adj 或 修正过的adj为不可能取值
                    if (oomAdjScore < fixedOomAdjScore || fixedOomAdjScore == ProcessList.IMPOSSIBLE_ADJ) {
                        param.result = null
                    }
                }
                appInfo.modifyProcessRecord(pid, oomAdjScore)
            } ?: run {
                val processRecordKt = runningInfo.activityManagerService.getProcessRecord(pid)

                // 子进程oom调整白名单(拥有和主进程一样的oom_adj_score, 不是模块不对其进行设置的意思)
                val upgradeSubProcessLevel =
                    processRecordKt?.getFullPackageName() in CommonProperties.WHITE_SUB_PROCESSES

                val expectedOomAdjScore: Int
                var finalOomAdjScore: Int
                if (upgradeSubProcessLevel) {
                    expectedOomAdjScore = ProcessRecordKt.DEFAULT_MAIN_ADJ
                    finalOomAdjScore = expectedOomAdjScore
                    param.args[2] = finalOomAdjScore
                } else {
                    expectedOomAdjScore = ProcessRecordKt.SUB_PROC_ADJ
                    finalOomAdjScore = expectedOomAdjScore
                    if (oomAdjScore > expectedOomAdjScore) {
                        finalOomAdjScore = oomAdjScore
                    } else {
                        param.args[2] = finalOomAdjScore
                    }
                }

                processRecordKt?.let {
                    it.oomAdjScore = oomAdjScore
                    it.fixedOomAdjScore = expectedOomAdjScore
                    appInfo.addProcess(processRecordKt)
                }

//                if (Objects.equals(AppGroupEnum.IDLE, appInfo.getAppGroupEnum())) {
//                    runningInfo.getProcessManager().setPidToBackgroundProcessGroup(pid, appInfo);
//                }
                if (BuildConfig.DEBUG) {
                    logger.debug(
                        "设置子进程: [${appInfo.packageName}, uid: ${uid}] ->>> " +
                                "pid: ${pid}, adj: $finalOomAdjScore"
                    )
                }
            }
        }
        return
    }
}