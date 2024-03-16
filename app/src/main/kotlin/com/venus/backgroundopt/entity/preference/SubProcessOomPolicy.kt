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

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt
import com.venus.backgroundopt.utils.message.MessageFlag

/**
 *  子进程oom配置策略
 *
 * @author XingC
 * @date 2023/9/28
 */
class SubProcessOomPolicy : MessageFlag {
    var policyEnum: SubProcessOomPolicyEnum = SubProcessOomPolicyEnum.DEFAULT
    var targetOomAdjScore: Int = ProcessRecordKt.SUB_PROC_ADJ

    enum class SubProcessOomPolicyEnum(val configCode: Int, val configName: String) {
        DEFAULT(1, "默认"),
        MAIN_PROCESS(2, "主进程"),
    }
}