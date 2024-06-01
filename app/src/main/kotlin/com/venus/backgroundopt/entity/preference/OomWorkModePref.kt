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

package com.venus.backgroundopt.entity.preference

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecord

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
         * maxAdj = 见[ProcessRecord.defaultMaxAdj], defaultAdj = 见[ProcessRecord.DEFAULT_MAIN_ADJ]。进程始终处于Foreground
         */
        const val MODE_STRICT = 5

        /**
         * 次严格模式
         *
         * maxAdj = 见[ProcessRecord.defaultMaxAdj], defaultAdj = 见[ProcessRecord.DEFAULT_MAIN_ADJ]。进程始终处于Foreground
         */
        const val MODE_STRICT_SECONDARY = 1

        /**
         * 宽松模式
         *
         *  maxAdj = 见[ProcessRecord.defaultMaxAdj], defaultAdj = 0。进程可以进入Background
         */
        const val MODE_NEGATIVE = 2

        /**
         * 平衡模式
         *
         * maxAdj = 不限制, defaultAdj = 0。进程可以进入Background
         */
        const val MODE_BALANCE = 3

        /**
         * 平衡模式
         *
         *  maxAdj = 见[ProcessRecord.defaultMaxAdj], defaultAdj = 见[ProcessRecord.DEFAULT_MAIN_ADJ]。进程可以进入Background
         */
        const val MODE_BALANCE_PLUS = 4

        @JvmStatic
        fun getDefault(): OomWorkModePref = OomWorkModePref()
    }

    var oomMode: Int = MODE_BALANCE

    constructor(oomMode: Int) : this() {
        this.oomMode = oomMode
    }
}