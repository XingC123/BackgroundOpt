package com.venus.backgroundopt.hook.handle.android.entity

import com.venus.backgroundopt.hook.constants.FieldConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.utils.callMethod
import com.venus.backgroundopt.utils.getObjectFieldValue
import com.venus.backgroundopt.utils.runCatchThrowable

/**
 * @author XingC
 * @date 2024/2/29
 */
class PackageManagerService(private val pmsInstance: Any) : IAndroidEntity {
    override fun getInstance(): Any = pmsInstance

    fun getDefaultHome(): String? {
        return runCatchThrowable(defaultValue = null) {
            pmsInstance.getObjectFieldValue(fieldName = FieldConstants.mInjector)?.let { mInjector->
                mInjector.callMethod(methodName = MethodConstants.getDefaultAppProvider)?.let { mDefaultAppProvider->
                    mDefaultAppProvider.callMethod(methodName = MethodConstants.getDefaultHome, 0) as String?
                }
            }
        }
    }
}