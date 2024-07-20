/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am

import com.venus.backgroundopt.common.util.OsUtils
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.core.RunningInfo
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.OomAdjusterCompatSinceA12
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat.OomAdjusterCompatUntilA11
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatFlag
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatHelper
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatRule
import com.venus.backgroundopt.xposed.entity.base.IEntityWrapper
import com.venus.backgroundopt.xposed.entity.base.callStaticMethod
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants
import com.venus.backgroundopt.xposed.util.getObjectFieldValue

/**
 * 封装了[ClassConstants.OomAdjuster]
 *
 * @author XingC
 * @date 2023/6/1
 */
abstract class OomAdjuster(
    @OriginalObject(classPath = ClassConstants.OomAdjuster)
    final override val originalInstance: Any,
    classLoader: ClassLoader = RunningInfo.getInstance().classLoader,
) : IEntityWrapper, IEntityCompatFlag {
    val cachedAppOptimizer: CachedAppOptimizer = CachedAppOptimizer(
        originalInstance.getObjectFieldValue(FieldConstants.mCachedAppOptimizer),
        classLoader
    )

    fun updateAppUidRecLSP(@OriginalObject processRecord: Any) {
        updateAppUidRecLSP(originalInstance, processRecord)
    }

    object OomAdjusterHelper : IEntityCompatHelper<OomAdjuster> {
        override val instanceClazz: Class<out OomAdjuster>
        override val instanceCreator: (Any) -> OomAdjuster

        init {
            if (OsUtils.isSOrHigher) {
                instanceClazz = OomAdjusterCompatSinceA12::class.java
                instanceCreator = ::createOomAdjusterSinceA12
            } else {
                instanceClazz = OomAdjusterCompatUntilA11::class.java
                instanceCreator = ::createOomAdjusterUntilA11
            }
        }

        private fun createOomAdjusterSinceA12(@OriginalObject instance: Any): OomAdjuster =
            OomAdjusterCompatSinceA12(instance)

        private fun createOomAdjusterUntilA11(@OriginalObject instance: Any): OomAdjuster =
            OomAdjusterCompatUntilA11(instance)
    }

    companion object : IOomAdjuster {
        @JvmStatic
        fun newInstance(@OriginalObject instance: Any): OomAdjuster {
            return OomAdjusterHelper.instanceCreator(instance)
        }

        @JvmStatic
        override fun updateAppUidRecLSP(instance: Any, processRecord: Any) {
            OomAdjusterHelper.callStaticMethod<Unit>(
                IOomAdjuster::updateAppUidRecLSP.name,
                instance,
                processRecord
            )
        }

    }
}

interface IOomAdjuster: IEntityCompatRule {
    fun updateAppUidRecLSP(@OriginalObject instance: Any, @OriginalObject processRecord: Any)
}
