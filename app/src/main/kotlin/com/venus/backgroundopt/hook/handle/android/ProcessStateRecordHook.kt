package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.hook.handle.android.entity.ProcessStateRecord
import com.venus.backgroundopt.utils.log.logInfo
import com.venus.backgroundopt.utils.preference.PreferencesUtil
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/25
 */
class ProcessStateRecordHook(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.ProcessStateRecord,
                MethodConstants.setCurAdj,
                arrayOf(
                    beforeHookAction { handleSetCurAdj(it) }
                ),
                Int::class.javaPrimitiveType    // curAdj
            ),
            HookPoint(
                ClassConstants.ProcessStateRecord,
                MethodConstants.getCurAdj,
                arrayOf(
                    beforeHookAction { handleGetCurAdj(it) }
                )
            ),
        )
    }

    private fun handleSetCurAdj(param: MethodHookParam) {
        if (!enableStrictOomMode) {
            return
        }
        val processStateRecord = param.thisObject
        val processRecordKt =
            ProcessRecordKt(
                runningInfo.activityManagerService,
                ProcessStateRecord.getProcessRecord(processStateRecord)
            )
        val appInfo = runningInfo.getRunningAppInfo(processRecordKt.uid)
        // 主进程首次创建时appInfo还未初始化, 此情况无需关心
        appInfo ?: return

        val mPid = try {
            appInfo.getmPid()
        } catch (t: Throwable) {
            Int.MIN_VALUE
        }

        if (processRecordKt.pid == mPid) {
            if (appInfo.mainProcCurAdj != ProcessRecordKt.DEFAULT_MAIN_ADJ) {
                // 放行
            } else {
                param.result = null
            }
        }
    }

    private fun handleGetCurAdj(param: MethodHookParam) {
        if (!enableStrictOomMode) {
            return
        }
        val processStateRecord = param.thisObject
        val processRecordKt =
            ProcessRecordKt(
                runningInfo.activityManagerService,
                ProcessStateRecord.getProcessRecord(processStateRecord)
            )
        val appInfo = runningInfo.getRunningAppInfo(processRecordKt.uid)
        // 主进程首次创建时appInfo还未初始化, 此情况无需关心
        appInfo ?: return

        val mPid = try {
            appInfo.getmPid()
        } catch (t: Throwable) {
            Int.MIN_VALUE
        }

        if (processRecordKt.pid == mPid) {
            /*if (BuildConfig.DEBUG) {
                logger.debug("getCurAdj() >>> 包名: ${processRecordKt.packageName}, uid: ${processRecordKt.uid}, pid: ${processRecordKt.pid}, 目标主进程, 给你返回${ProcessRecordKt.DEFAULT_MAIN_ADJ}")
            }*/
            param.result = ProcessRecordKt.DEFAULT_MAIN_ADJ
        }
    }

    companion object {
        var enableStrictOomMode = true

        init {
            enableStrictOomMode = PreferencesUtil.getBoolean(
                PreferenceNameConstants.MAIN_SETTINGS,
                PreferenceKeyConstants.STRICT_OOM_MODE,
                false
            )
            logInfo(logStr = "OOM严格模式: $enableStrictOomMode")
        }
    }
}