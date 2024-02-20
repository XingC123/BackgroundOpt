package com.venus.backgroundopt.utils.message.handle

import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.environment.CommonProperties
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
            null
        }
    }
}

class GlobalOomScorePolicy : MessageFlag {
    var enabled: Boolean = false
    var globalOomScoreEffectiveScope: GlobalOomScoreEffectiveScopeEnum =
        GlobalOomScoreEffectiveScopeEnum.MAIN_PROCESS
    var customGlobalOomScore: Int = Int.MIN_VALUE
}

enum class GlobalOomScoreEffectiveScopeEnum : MessageFlag {
    MAIN_PROCESS,
    MAIN_AND_SUB_PROCESS,
    ALL,
}
