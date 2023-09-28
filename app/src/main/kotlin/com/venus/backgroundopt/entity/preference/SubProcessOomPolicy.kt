package com.venus.backgroundopt.entity.preference

import com.venus.backgroundopt.hook.handle.android.entity.ProcessRecordKt

/**
 *  子进程oom配置策略
 *
 * @author XingC
 * @date 2023/9/28
 */
class SubProcessOomPolicy {
    var policyEnum: SubProcessOomPolicyEnum = SubProcessOomPolicyEnum.DEFAULT
    var targetOomAdjScore: Int = ProcessRecordKt.SUB_PROC_ADJ

    enum class SubProcessOomPolicyEnum(val configCode: Int, val configName: String) {
        DEFAULT(1, "默认"),
        MAIN_PROCESS(2, "主进程"),
    }
}