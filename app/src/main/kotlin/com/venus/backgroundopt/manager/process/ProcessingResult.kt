package com.venus.backgroundopt.manager.process

import com.venus.backgroundopt.utils.message.MessageFlag

/**
 * @author XingC
 * @date 2023/10/14
 */
class ProcessingResult : MessageFlag {
    // 上次执行时间
    var lastProcessingTime: Long? = null

    // 上次执行结果
    var lastProcessingCode: Int = 0
}