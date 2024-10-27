/*
 * Copyright (C) 2023-2024 BackgroundOpt
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

package com.venus.backgroundopt.common.entity.message

import com.venus.backgroundopt.common.util.PackageUtils
import com.venus.backgroundopt.common.util.UserUtils
import com.venus.backgroundopt.common.util.message.MessageFlag

/**
 * @author XingC
 * @date 2024/10/26
 */
class QueryInstalledAppParam: MessageFlag {
    var userId: Int = UserUtils.MAIN_USER
    lateinit var packageName: String
    var packageInfoFlag: Int = PackageUtils.PACKAGE_INFO_FLAG
}