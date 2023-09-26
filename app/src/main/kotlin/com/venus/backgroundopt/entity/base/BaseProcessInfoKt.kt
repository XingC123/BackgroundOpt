package com.venus.backgroundopt.entity.base

/**
 * @author XingC
 * @date 2023/9/26
 */
open class BaseProcessInfoKt(
    var uid: Int = Int.MIN_VALUE,
    var pid: Int = Int.MIN_VALUE,
    var userId: Int = 0,
) {
    var oomAdjScore: Int = Int.MIN_VALUE

    /**
     * 修正过的oomAdjScore。
     * 即本模块为了优化后台而对进程的oomAdjScore修改的值
     */
    var fixedOomAdjScore = Int.MIN_VALUE

    var mainProcess = false // app主进程

    lateinit var packageName: String
    lateinit var processName: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BaseProcessInfoKt

        if (uid != other.uid) return false
        if (pid != other.pid) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid
        result = 31 * result + pid
        result = 31 * result + userId
        return result
    }
}