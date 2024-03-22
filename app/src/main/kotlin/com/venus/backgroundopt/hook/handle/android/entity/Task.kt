package com.venus.backgroundopt.hook.handle.android.entity

import android.content.Intent
import com.venus.backgroundopt.annotation.AndroidObject
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.utils.getIntFieldValue
import com.venus.backgroundopt.utils.getObjectFieldValue

/**
 * @author XingC
 * @date 2024/3/19
 */
class Task(
    @AndroidObject(classPath = ClassConstants.Task)
    override val originalInstance: Any
) : IAndroidEntity {
    val basePackageName: String = (originalInstance.getObjectFieldValue(
        fieldName = FieldConstants.intent
    ) as? Intent)?.let { intent ->
        intent.`package` ?: intent.component?.packageName
    }!!

    val mUserId: Int = originalInstance.getIntFieldValue(fieldName = "mUserId")
}