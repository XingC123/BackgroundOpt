package com.venus.backgroundopt.environment

import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.utils.preference.prefAll

/**
 * @author XingC
 * @date 2023/9/26
 */
object CommonProperties {
    // 子进程oom策略映射表
    val subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy>? by lazy {
        prefAll<SubProcessOomPolicy>(PreferenceNameConstants.SUB_PROCESS_OOM_POLICY)?.also { map ->
            // 默认白名单
            setOf(
                "com.tencent.mobileqq:MSF", /* qq */
                "com.tencent.mm:push", /* 微信 */
            ).forEach { processName ->
                if (!map.containsKey(processName)) {
                    map[processName] = SubProcessOomPolicy().apply {
                        policyEnum = SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
                    }
                }
            }
        }
    }
}