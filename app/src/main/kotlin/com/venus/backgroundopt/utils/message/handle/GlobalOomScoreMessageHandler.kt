package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
import com.venus.backgroundopt.hook.handle.android.entity.ProcessList
import com.venus.backgroundopt.utils.message.MessageFlag
import com.venus.backgroundopt.utils.message.MessageHandler
import com.venus.backgroundopt.utils.message.createResponse
import de.robv.android.xposed.XC_MethodHook

/**
 * @author XingC
 * @date 2024/2/20
 */
class GlobalOomScoreMessageHandler : MessageHandler {
    override fun handle(
        runningInfo: RunningInfo,
        param: XC_MethodHook.MethodHookParam,
        value: String?
    ) {
        createResponse<GlobalOomScorePolicy>(
            param = param,
            value = value
        ) { globalOomScorePolicy: GlobalOomScorePolicy ->
            CommonProperties.globalOomScorePolicy.value = globalOomScorePolicy
            logger.info("切换全局oom策略。$globalOomScorePolicy")
            null
        }
    }
}

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
            return ProcessList.NATIVE_ADJ <= score && score < ProcessList.UNKNOWN_ADJ
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

enum class GlobalOomScoreEffectiveScopeEnum(val uiName:String) : MessageFlag {
    MAIN_PROCESS("主进程(有界面)"),
    MAIN_AND_SUB_PROCESS("主+子进程(有界面)"),
    ALL("所有进程"),
}
