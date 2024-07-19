package com.venus.backgroundopt.xposed.entity.android.com.android.server.wm

import android.content.Intent
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.util.getIntFieldValue
import com.venus.backgroundopt.xposed.util.getObjectFieldValue

/**
 * @author XingC
 * @date 2024/3/19
 */
class Task(
    @OriginalObject(classPath = ClassConstants.Task)
    override val originalInstance: Any,
) : IEntityWrapper {
    val basePackageName: String = (originalInstance.getObjectFieldValue(
        fieldName = FieldConstants.intent
    ) as? Intent)?.let { intent ->
        intent.`package` ?: intent.component?.packageName
    }!!

    val mUserId: Int = originalInstance.getIntFieldValue(fieldName = "mUserId")
}