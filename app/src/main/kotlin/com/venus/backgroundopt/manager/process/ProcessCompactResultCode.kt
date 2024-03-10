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
                    
 package com.venus.backgroundopt.manager.process

/**
 * 进程压缩结果码
 *
 * @author XingC
 * @date 2024/2/26
 */
class ProcessCompactResultCode {
    companion object {
        // 异常
        const val problem = -1

        // 正常执行
        const val success = 1

        // 未执行
        const val doNothing = 2

        // 无需执行(没有执行的必要)
        const val unNecessary = 3
    }
}