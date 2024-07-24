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

package com.venus.backgroundopt.common.entity.preference

import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy.SubProcessOomPolicyEnum
import com.venus.backgroundopt.common.entity.preference.SubProcessOomPolicy.SubProcessOomPolicyEnum.DEFAULT
import com.venus.backgroundopt.common.util.message.MessageFlag
import com.venus.backgroundopt.xposed.entity.self.ProcessAdjConstants

/**
 *  子进程oom配置策略
 *
 * @author XingC
 * @date 2023/9/28
 */
class SubProcessOomPolicy : MessageFlag, JsonPreferenceFlag {
    var policyEnum: SubProcessOomPolicyEnum = DEFAULT

    var targetFgAdj: Int = Int.MIN_VALUE
    var targetBgAdj: Int = Int.MIN_VALUE

    enum class SubProcessOomPolicyEnum(val configCode: Int, val configName: String) {
        DEFAULT(1, "默认"),
        MAIN_PROCESS(2, "主进程"),
        CUSTOM_ADJ(3, "自定义分数"),
    }
}

fun SubProcessOomPolicy?.isCustomProcessAdjValid(): Boolean {
    if (this?.policyEnum != SubProcessOomPolicyEnum.CUSTOM_ADJ) {
        return false
    }

    return ProcessAdjConstants.isValidAdj(targetFgAdj) || ProcessAdjConstants.isValidAdj(targetBgAdj)
}

fun SubProcessOomPolicy?.getCustomFgAdj(): Int? {
    return if (this?.policyEnum == SubProcessOomPolicyEnum.CUSTOM_ADJ
        && ProcessAdjConstants.isValidAdj(targetFgAdj)
    ) {
        targetFgAdj
    } else {
        null
    }
}

fun SubProcessOomPolicy?.getCustomBgAdj(): Int? {
    return if (this?.policyEnum == SubProcessOomPolicyEnum.CUSTOM_ADJ
        && ProcessAdjConstants.isValidAdj(targetBgAdj)
    ) {
        targetBgAdj
    } else {
        null
    }
}