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

import com.venus.backgroundopt.utils.message.MessageFlag

/**
 * @author XingC
 * @date 2023/10/14
 */
open class ProcessingResult : MessageFlag {
    // 上次执行时间
    var lastProcessingTime: Long = 0

    // 上次执行结果
    var lastProcessingCode: Int = 0
}