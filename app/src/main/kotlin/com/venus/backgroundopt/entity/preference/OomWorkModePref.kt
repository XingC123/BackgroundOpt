package com.venus.backgroundopt.entity.preference

/**
 * OOM工作模式的配置对应的实体
 *
 * @author XingC
 * @date 2023/11/22
 */
class OomWorkModePref() {
    companion object {
        /**
         * 严格模式
         *
         * maxAdj = 100, defaultAdj = 0。进程始终处于Foreground
         */
        const val MODE_STRICT = 1

        /**
         * 宽松模式
         *
         * maxAdj = 700, defaultAdj = 0。进程可以进入Background
         */
        const val MODE_NEGATIVE = 2

        /**
         * 平衡模式
         *
         * maxAdj = 不限制, defaultAdj = 0。进程可以进入Background
         */
        const val MODE_BALANCE = 3

        @JvmStatic
        fun getDefault(): OomWorkModePref = OomWorkModePref()
    }

    var oomMode: Int = MODE_BALANCE

    constructor(oomMode: Int) : this() {
        this.oomMode = oomMode
    }
}