package com.venus.backgroundopt.environment

import com.venus.backgroundopt.entity.preference.SubProcessOomPolicy
import com.venus.backgroundopt.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.utils.preference.prefAll
import java.util.concurrent.ConcurrentHashMap

/**
 * @author XingC
 * @date 2023/9/26
 */
object CommonProperties {
    // 默认白名单
    val subProcessNameDefaultUpgrade: Set<String> by lazy {
        setOf(
            "com.tencent.mobileqq:MSF", /* qq */
            "com.tencent.mm:push", /* 微信 */
        )
    }

    // 子进程oom策略映射表
    val subProcessOomPolicyMap: MutableMap<String, SubProcessOomPolicy> by lazy {
        (prefAll(PreferenceNameConstants.SUB_PROCESS_OOM_POLICY)
            ?: ConcurrentHashMap<String, SubProcessOomPolicy>()).apply {
            subProcessNameDefaultUpgrade.forEach { processName ->
                if (!this.containsKey(processName)) {
                    this[processName] = SubProcessOomPolicy().apply {
                        policyEnum = SubProcessOomPolicy.SubProcessOomPolicyEnum.MAIN_PROCESS
                    }
                }
            }
        }
    }
}