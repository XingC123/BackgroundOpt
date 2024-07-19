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

package com.venus.backgroundopt.xposed.point.android

import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.hook.base.IHook

/**
 * 测试用hook
 *
 * @author XingC
 * @date 2024/2/9
 */
class ProcessRecordHookForTest(
    classLoader: ClassLoader,
    runningInfo: RunningInfo,
) : IHook(classLoader, runningInfo) {
    // b站国内版(7.68.0)在播放视频的时候进入后台, 一段时间后再回去, 可能遇到重载(主进程已死, 需要重建)。
    // 具体现象为: 重新进入前台之前, 在进程列表只可以见到"tv.danmaku.bili:download"进程, 而主进程已死。再次进入会新建主进程
    // 经过试验性hook, 取到以下日志:
    // Backgroundopt --> [I]: b站被杀了: reason: depends on provider tv.danmaku.bili/com.bilibili.lib.neuron.internal2.provider.NeuronContentProvider in dying proc tv.danmaku.bili:pushservice (adj -10000), reasonCode: 12, subReason: 0, noisy: true
    // Backgroundopt --> [I]: b站被杀了: description: depends on provider tv.danmaku.bili/com.bilibili.lib.neuron.internal2.provider.NeuronContentProvider in dying proc tv.danmaku.bili:pushservice (adj -10000)
    // Backgroundopt --> [D]: kill: userId: 0, packageName: tv.danmaku.bili >>> 杀死App

    // Backgroundopt --> [I]: b站被杀了: reason: bg anr, reasonCode: 6, subReason: 0, noisy: true
    // Backgroundopt --> [I]: b站被杀了: description: bg anr
    // Backgroundopt --> [D]: kill: userId: 0, packageName: tv.danmaku.bili >>> 杀死App
    /*
        文心一言对" depends on provider"的回答:
        com.android.server.am.ProcessRecord.killLocked 是 Android 操作系统内部 ActivityManagerService (AMS) 的一个方法，用于强制结束（或“杀死”）一个进程。当系统决定需要回收资源时，它可能会调用这个方法。

        如果 killLocked 的原因被标记为 “depends on provider”，这通常意味着该进程依赖于某个内容提供者（Content Provider），而这个内容提供者可能已经不再可用或已经被杀死。在 Android 中，内容提供者是一种用于在应用程序之间共享数据的组件。如果一个应用程序依赖于另一个应用程序提供的内容，而提供内容的应用程序不再运行或不可用，那么依赖它的进程可能会变得不稳定或无法正常工作。

        为了保持系统的稳定性和响应性，Android 系统会定期评估正在运行的进程，并基于一系列复杂的算法和策略来决定哪些进程应该被保留，哪些进程应该被回收。如果一个进程被标记为依赖于一个不再可用的内容提供者，系统可能会决定杀死这个进程以释放资源。

        回收这样的进程有助于确保系统资源得到更有效的利用，同时也有助于避免潜在的问题，如应用程序崩溃或界面无响应。当然，这也会对用户体验产生一定的影响，因为正在运行的应用程序可能会被突然中断。但是，在大多数情况下，Android 系统会尽量优化这些决策，以最大程度地减少对用户的影响。

        需要注意的是，这种情况下的进程回收通常是由系统内部逻辑自动触发的，而不是由用户直接控制的。如果你作为开发者遇到了这个问题，可能需要检查你的应用程序是否正确地处理了对内容提供者的依赖，以及是否能够在内容提供者不可用时优雅地降级或恢复。
     */
    /*override fun hook() {
        ClassConstants.ProcessRecord.beforeHook(
            enable = Build.VERSION.SDK_INT == Build.VERSION_CODES.S || Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2,
            classLoader = classLoader,
            methodName = "killLocked",
            paramTypes = arrayOf(
                String::class.java,
                Int::class.java,
                Int::class.java,
                Boolean::class.java,
            ),
        ) { param ->
            val process = param.thisObject
            val pid = ProcessRecordKt.getPid(process)
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@beforeHook
            if (!(processRecord.mainProcess && processRecord.packageName == "tv.danmaku.bili")) {
                return@beforeHook
            }
            val reason = param.args[0] as String
            val reasonCode = param.args[1] as Int
            val subReason = param.args[2] as Int
            val noisy = param.args[3] as Boolean
            logger.info("b站被杀了: reason: ${reason}, reasonCode: ${reasonCode}, subReason: ${subReason}, noisy: ${noisy}")
        }

        ClassConstants.ProcessList.beforeHook(
            classLoader = classLoader,
            methodName = "noteAppKill",
            paramTypes = arrayOf(
                ClassConstants.ProcessRecord,
                Int::class.java,
                Int::class.java,
                String::class.java
            ),
        ) { param ->
            val pid = ProcessRecordKt.getPid(param.args[0])
            val processRecord = runningInfo.getRunningProcess(pid) ?: return@beforeHook
            if (!(processRecord.mainProcess && processRecord.packageName == "tv.danmaku.bili")) {
                return@beforeHook
            }
            logger.info("b站被杀了: description: ${param.args[3]}")
        }
    }*/
    override fun hook() {
        // do nothing
    }
}