package com.venus.backgroundopt.hook.handle.android

import com.venus.backgroundopt.annotation.UnusedReason
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.base.HookPoint
import com.venus.backgroundopt.hook.base.MethodHook
import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/25
 */
class OomAdjusterHook(classLoader: ClassLoader?, hookInfo: RunningInfo?) :
    MethodHook(classLoader, hookInfo) {
    override fun getHookPoint(): Array<HookPoint> {
        return arrayOf(
            /* HookPoint(
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
             ),*/
            /*generateMatchedMethodHookPoint(
                true,
                ClassConstants.OomAdjuster,
                MethodConstants.updateAndTrimProcessLSP,
                arrayOf(
                    replacementHookAction { handleUpdateAndTrimProcessLSP(it) }
                )
            ),*/
            /*generateMatchedMethodHookPoint(
                true,
                ClassConstants.OomAdjuster,
                MethodConstants.handleUpdateAndTrimProcessLSP,
                arrayOf(
                    replacementHookAction { handleShouldKillExcessiveProcesses(it) }
                )
            ),*/
        )
    }

    private fun handleComputeOomAdjLSP(param: MethodHookParam) {
        val app = param.args[0]
        val pid = ProcessRecordKt.getPid(app)
        val runningInfo = runningInfo
        val processRecord = runningInfo.getRunningProcess(pid) ?: return
        val appInfo = processRecord.appInfo

        // 如果此次调节的不是主进程, 则返回
        val mPid = try {
            appInfo.getmPid()
        } catch (t: Throwable) {
            return
        }
        if (pid != mPid) {
            return
        }

        // 如果app的主进程已设置过oom_adj_score, 则使系统不进行任何操作
        if (appInfo.mainProcCurAdj == ProcessRecordKt.DEFAULT_MAIN_ADJ) {
            param.result = false

            // 日志打印非常猛烈。在Redmi Note5 pro(whyred) Nusantara rom上, 被com.android.launcher3刷屏
            /*if (BuildConfig.DEBUG) {
                logger.debug("包名: ${ProcessRecord.getPkgName(app)}, pid: $pid 为app主进程且已设置过oom_adj_score, 无需再次计算")
            }*/
        }
    }

    /**
     * 用于查杀已缓存进程（cached process）以及空进程（empty process，可视为不含组件的已缓存进程），表示不需要过多的缓存进程。
     * 这种算法会抑制LRU的长度，换个角度来看就是压制可运行进程的个数，让内存保持良好状态且不影响用户体验
     *
     * 参考自: [AOSP的进程管理](https://www.bluepuni.com/archives/aosp-process-management/)
     *
     * @param param MethodHookParam
     */
    @UnusedReason("过长的lru列表会影响很多处源码的遍历效率")
    fun handleUpdateAndTrimProcessLSP(param: MethodHookParam): Boolean {
        return true
    }

    @UnusedReason("与handleUpdateAndTrimProcessLSP()一起")
    fun handleShouldKillExcessiveProcesses(param: MethodHookParam): Boolean {
        return false
    }
}