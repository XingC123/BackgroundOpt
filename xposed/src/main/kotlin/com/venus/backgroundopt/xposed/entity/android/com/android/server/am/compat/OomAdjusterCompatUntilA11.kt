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

package com.venus.backgroundopt.xposed.entity.android.com.android.server.am.compat

import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.IOomAdjuster
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.OomAdjuster
import com.venus.backgroundopt.xposed.entity.base.IEntityCompatMethod
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants
import com.venus.backgroundopt.xposed.util.callMethod

/**
 * @author XingC
 * @date 2024/7/14
 */
class OomAdjusterCompatUntilA11(originalInstance: Any) : OomAdjuster(originalInstance) {
    companion object: IOomAdjuster {
        @JvmStatic
        @IEntityCompatMethod
        override fun updateAppUidRecLSP(instance: Any, processRecord: Any) {
            instance.callMethod(
                MethodConstants.updateAppUidRecLocked,
                processRecord
            )
        }
    }
}