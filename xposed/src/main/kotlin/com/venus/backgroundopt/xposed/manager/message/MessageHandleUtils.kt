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

package com.venus.backgroundopt.xposed.manager.message

import com.venus.backgroundopt.common.util.log.logError
import com.venus.backgroundopt.common.util.parseObject
import com.venus.backgroundopt.common.util.toJsonString
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2024/7/15
 */
class MessageHandleUtils

/* *************************************************************************
 *                                                                         *
 * 创建响应                                                                  *
 *                                                                         *
 **************************************************************************/
inline fun <reified E> createResponseWithNullData(
    param: MethodHookParam,
    value: String?,
    setJsonData: Boolean = false,
    generateData: (value: E) -> Unit,
) {
    createResponse<E>(
        param = param,
        value = value,
        setJsonData = setJsonData,
        generateData = {
            generateData(it)
            null
        }
    )
}

inline fun <reified E> createJsonResponse(
    param: MethodHookParam,
    value: String?,
    generateData: (value: E) -> Any?,
) {
    createResponse<E>(
        param = param,
        value = value,
        setJsonData = true,
        generateData
    )
}

/**
 * 创建响应
 *
 * @param param hook的方法的原生参数
 * @param value ui发送过来的消息
 * @param generateData 生成数据使用的方法
 */
inline fun <reified E> createResponse(
    param: MethodHookParam,
    value: String?,
    setJsonData: Boolean = false,
    generateData: (value: E) -> Any?,
) {
    var errorMsg: String? = null
    var result: String? = null
    try {
        value?.let { _ ->
            errorMsg = "Message转换异常"

            value.parseObject(E::class.java)?.let { eData ->
                errorMsg = "数据处理异常"
                generateData(eData)?.let { responseObj ->
                    result = if (setJsonData)
                        responseObj.toJsonString()
                    else responseObj.toString()
                }
            }
        }
    } catch (t: Throwable) {
        logError(
            logStr = "响应对象创建错误。errorMsg: $errorMsg",
            t = t
        )
    }
    param.result = result
}