package com.venus.backgroundopt.xposed.manager.process

/**
 * 进程压缩结果码
 *
 * @author XingC
 * @date 2024/2/26
 */
object ProcessCompactResultCode {
    // 异常
    const val problem = -1

    // 正常执行
    const val success = 1

    // 未执行
    const val doNothing = 2

    // 无需执行(没有执行的必要)
    const val unNecessary = 3
}