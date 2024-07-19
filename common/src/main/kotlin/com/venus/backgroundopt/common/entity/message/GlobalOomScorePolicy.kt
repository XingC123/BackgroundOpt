package com.venus.backgroundopt.common.entity.message

import com.venus.backgroundopt.common.util.message.MessageFlag
import com.venus.backgroundopt.xposed.entity.self.ProcessAdjConstants

/**
 * @author XingC
 * @date 2024/2/20
 */
class GlobalOomScorePolicy : MessageFlag {
    var enabled: Boolean = false
    var globalOomScoreEffectiveScope: GlobalOomScoreEffectiveScopeEnum =
        GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS
    var customGlobalOomScore: Int = Int.MIN_VALUE

    override fun toString(): String {
        return if (enabled) {
            "[启用]全局OOM, 范围: ${globalOomScoreEffectiveScope.uiName}, oom分数: $customGlobalOomScore"
        } else {
            "[禁用]全局OOM"
        }
    }

    companion object {
        @JvmStatic
        fun isCustomGlobalOomScoreIllegal(score: Int): Boolean {
            return ProcessAdjConstants.NATIVE_ADJ <= score && score < ProcessAdjConstants.UNKNOWN_ADJ
        }

        @JvmStatic
        fun getCustomGlobalOomScoreIfIllegal(score: Int, defaultValue: Int): Int {
            return if (isCustomGlobalOomScoreIllegal(score)) {
                score
            } else {
                defaultValue
            }
        }
    }
}

enum class GlobalOomScoreEffectiveScopeEnum(val uiName: String) : MessageFlag {
    MAIN_PROCESS("主进程(adj>0)"),
    MAIN_AND_SUB_PROCESS("主+子进程(adj>0)"),
    MAIN_PROCESS_ANY("主进程(所有)"),
    ALL("所有进程"),
}
