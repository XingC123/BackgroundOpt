package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.entity.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.base.action.beforeHookAction
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/25
 */
class OomAdjusterHook(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            HookPoint(
                ClassConstants.OomAdjuster,
                MethodConstants.computeOomAdjLSP,
                arrayOf(
                    beforeHookAction { handleComputeOomAdjLSP(it) }
                ),
                ClassConstants.ProcessRecord,           // app
                Int::class.javaPrimitiveType,           // cachedAdj
                ClassConstants.ProcessRecord,           // topApp
                Boolean::class.javaPrimitiveType,       // doingAll
                Long::class.javaPrimitiveType,          // now
                Boolean::class.javaPrimitiveType,       // cycleReEval
                Boolean::class.javaPrimitiveType        // computeClients
            ),
        )
    }

    private fun handleComputeOomAdjLSP(param: MethodHookParam) {
        val app = param.args[0]

        val runningInfo = runningInfo
        val appInfo = runningInfo.getRunningAppInfo(ProcessRecord.getUID(app))
        appInfo ?: return

        // 如果此次调节的不是主进程, 则返回
        val pid = ProcessRecord.getPid(app)
        val mPid = try {
            appInfo.getmPid()
        } catch (t: Throwable) {
            return
        }
        if (pid != mPid) {
            return
        }

        // 如果app的主进程已设置过oom_adj_score, 则使系统不进行任何操作
        if (appInfo.mainProcCurAdj == ProcessRecord.DEFAULT_MAIN_ADJ) {
            param.result = false

            // 日志打印非常猛烈。在Redmi Note5 pro(whyred) Nusantara rom上, 被com.android.launcher3刷屏
            /*if (BuildConfig.DEBUG) {
                logger.debug("包名: ${ProcessRecord.getPkgName(app)}, pid: $pid 为app主进程且已设置过oom_adj_score, 无需再次计算")
            }*/
        }
    }
}