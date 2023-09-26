package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.hook.handle.android.entity.ProcessStateRecord
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
                    beforeHookAction { handleSetOomAdj(it) }
                ),
                Int::class.javaPrimitiveType    // curAdj
            ),
        )
    }

    private fun handleSetOomAdj(param: MethodHookParam) {
        val processStateRecord = param.thisObject
        val processRecordKt = ProcessRecordKt(ProcessStateRecord.getProcessRecord(processStateRecord))
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
}