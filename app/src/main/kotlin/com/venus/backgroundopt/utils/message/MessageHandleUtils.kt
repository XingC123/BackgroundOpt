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

package com.venus.backgroundopt.utils.message

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt
import com.venus.backgroundopt.utils.log.logDebug
import com.venus.backgroundopt.utils.log.logDebugAndroid
import com.venus.backgroundopt.utils.log.logError
import com.venus.backgroundopt.utils.log.logErrorAndroid
import com.venus.backgroundopt.utils.log.logWarnAndroid
import com.venus.backgroundopt.utils.message.handle.AppCompactListMessageHandler
import com.venus.backgroundopt.utils.message.handle.AppOptimizePolicyMessageHandler
import com.venus.backgroundopt.utils.message.handle.AppWebviewProcessProtectMessageHandler
import com.venus.backgroundopt.utils.message.handle.AutoStopCompactTaskMessageHandler
import com.venus.backgroundopt.utils.message.handle.BackgroundTasksMessageHandler
import com.venus.backgroundopt.utils.message.handle.EnableForegroundProcTrimMemPolicyHandler
import com.venus.backgroundopt.utils.message.handle.ForegroundProcTrimMemPolicyHandler
import com.venus.backgroundopt.utils.message.handle.GetInstalledPackagesMessageHandler
import com.venus.backgroundopt.utils.message.handle.GetManagedAdjDefaultAppsMessageHandler
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreEffectiveScopeMessageHandler
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreMessageHandler
import com.venus.backgroundopt.utils.message.handle.GlobalOomScoreValueMessageHandler
import com.venus.backgroundopt.utils.message.handle.HomePageModuleInfoMessageHandler
import com.venus.backgroundopt.utils.message.handle.KillAfterRemoveTaskMessageHandler
import com.venus.backgroundopt.utils.message.handle.ModuleRunningMessageHandler
import com.venus.backgroundopt.utils.message.handle.RunningAppInfoMessageHandler
import com.venus.backgroundopt.utils.message.handle.SimpleLmkMessageHandler
import com.venus.backgroundopt.utils.message.handle.SubProcessOomConfigChangeMessageHandler
import com.venus.backgroundopt.utils.message.handle.TargetAppGroupMessageHandler
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * ui与模块主进程通信工具类
 * 其他部分见: [MessageHandler], [MessageKeyConstants], [ActivityManagerServiceHookKt.handleStartService]
 *
 * @author XingC
 * @date 2023/9/21
 */

const val NULL_DATA = "NULL_DATA"
const val NULL_FLAG = "NULL_FLAG"

const val CUR_CLASS_PREFIX = "com.venus.backgroundopt.utils.message.MessageHandleUtils."

@JvmField
val nullComponentName = ComponentName(NULL_DATA, NULL_FLAG)

// 已注册的消息处理器
@JvmField
val registeredMessageHandler = mapOf(
    MessageKeyConstants.getRunningAppInfo to RunningAppInfoMessageHandler(),
    MessageKeyConstants.getTargetAppGroup to TargetAppGroupMessageHandler(),
    MessageKeyConstants.getBackgroundTasks to BackgroundTasksMessageHandler(),
    MessageKeyConstants.getAppCompactList to AppCompactListMessageHandler(),
    MessageKeyConstants.subProcessOomConfigChange to SubProcessOomConfigChangeMessageHandler(),
    MessageKeyConstants.getInstalledApps to GetInstalledPackagesMessageHandler(),
    MessageKeyConstants.autoStopCompactTask to AutoStopCompactTaskMessageHandler(),
    MessageKeyConstants.enableForegroundProcTrimMemPolicy to EnableForegroundProcTrimMemPolicyHandler(),
    MessageKeyConstants.foregroundProcTrimMemPolicy to ForegroundProcTrimMemPolicyHandler(),
    MessageKeyConstants.appOptimizePolicy to AppOptimizePolicyMessageHandler(),
    MessageKeyConstants.appWebviewProcessProtect to AppWebviewProcessProtectMessageHandler(),
    MessageKeyConstants.enableSimpleLmk to SimpleLmkMessageHandler(),
    MessageKeyConstants.enableGlobalOomScore to GlobalOomScoreMessageHandler(),
    MessageKeyConstants.globalOomScoreEffectiveScope to GlobalOomScoreEffectiveScopeMessageHandler(),
    MessageKeyConstants.globalOomScoreValue to GlobalOomScoreValueMessageHandler(),
//    MessageKeyConstants.getTrimMemoryOptThreshold to GetTrimMemoryOptThresholdMessageHandler(),
    MessageKeyConstants.getHomePageModuleInfo to HomePageModuleInfoMessageHandler(),
    MessageKeyConstants.killAfterRemoveTask to KillAfterRemoveTaskMessageHandler(),
    MessageKeyConstants.moduleRunning to ModuleRunningMessageHandler(),
    MessageKeyConstants.getManagedAdjDefaultApps to GetManagedAdjDefaultAppsMessageHandler(),
)

// json传输的载体
data class Message<T>(var v: T?) : MessageFlag

/* *************************************************************************
 *                                                                         *
 * 发送消息                                                                  *
 *                                                                         *
 **************************************************************************/
/**
 * 发送消息
 *
 * @param context 需要上下文来调用方法
 * @param key 消息所在的key
 * @param value 消息
 * @return 响应的信息
 */
fun sendMessage(context: Context, key: String, value: Any = ""): String? {
    context.startService(Intent().apply {
        `package` = BuildConfig.APPLICATION_ID
        action = JSON.toJSONString(Message(value))
        type = key
    })?.let {
        if (BuildConfig.DEBUG) {
            logDebugAndroid(
                methodName = "${CUR_CLASS_PREFIX}sendMessage",
                logStr = "客户端收到的原始信息: ${it.packageName}"
            )
        }
        // componentName.packageName被征用为存放返回数据
        return if (it.packageName == NULL_DATA) null else it.packageName
    } ?: run {
        if (BuildConfig.DEBUG) {
            logWarnAndroid(
                methodName = "${CUR_CLASS_PREFIX}sendMessage",
                logStr = "模块主进程回复内容为null, 无法进行转换"
            )
        }
        return null
    }
}

/**
 * 发送消息
 *
 * @param E 使用json转换后的类型
 * @param context 需要上下文来调用方法
 * @param key 消息所在的key
 * @param value 消息
 * @return json转换后的对象
 */
inline fun <reified E> sendMessage(context: Context, key: String, value: Any = ""): E? {
    return try {
        sendMessage(context, key, value)?.let {
            JSON.parseObject(it, E::class.java)
        }
    } catch (t: Throwable) {
        logErrorAndroid(
            methodName = "${CUR_CLASS_PREFIX}sendMessage<E>",
            logStr = "响应消息转换失败",
            t = t
        )
        null
    }
}

inline fun <reified E> sendMessageAcceptList(
    context: Context,
    key: String,
    value: Any = ""
): MutableList<E>? {
    return try {
        sendMessage(context, key, value)?.let {
            JSON.parseArray(it, E::class.java)
        }
    } catch (t: Throwable) {
        logErrorAndroid(
            methodName = "${CUR_CLASS_PREFIX}sendMessage<E>",
            logStr = "响应消息转换失败",
            t = t
        )
        null
    }
}

/* *************************************************************************
 *                                                                         *
 * 创建响应                                                                  *
 *                                                                         *
 **************************************************************************/
inline fun <reified E> createResponseWithNullData(
    param: MethodHookParam,
    value: String?,
    setJsonData: Boolean = false,
    generateData: (value: E) -> Unit
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
    generateData: (value: E) -> Any?
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
    generateData: (value: E) -> Any?
) {
//    if (BuildConfig.DEBUG) {
    logDebug(
        methodName = "${CUR_CLASS_PREFIX}createResponse",
        logStr = "模块进程接收的数据为: $value"
    )
//    }
    var errorMsg: String? = null
    try {
        param.result = value?.let { v ->
            errorMsg = "Message转换异常"
            val message = JSON.parseObject(v, Message::class.java)
            val final = if (message.v is JSONObject) {
                errorMsg = "Message.v转换异常"
                JSON.parseObject(message.v.toString(), E::class.java)
            } else {
                errorMsg = "Message.v类型转换异常"
                message.v as? E
            }

            final?.let { eData ->
                errorMsg = "数据处理异常"
                generateData(eData)?.let { responseObj ->
                    errorMsg = "组件生成异常"
                    ComponentName(
                        if (setJsonData) JSON.toJSONString(responseObj) else responseObj.toString(),
                        NULL_FLAG
                    )
                } ?: nullComponentName
            } ?: nullComponentName
        } ?: nullComponentName
    } catch (t: Throwable) {
        logError(
            methodName = "${CUR_CLASS_PREFIX}createResponse",
            logStr = "响应对象创建错误。errorMsg: $errorMsg",
            t = t
        )
        param.result = nullComponentName
    }
}

/**
 * 将[JSONObject]转换为指定的类型[E]
 *
 * 若类class A { var value: Any? = null }导致fastjson无法正确映射value的类型。则使用本方法来进行转换
 * @param jsonObject Any? 确保此字段为[JSONObject]
 * @return E?
 */
inline fun <reified E> parseObjectFromJsonObject(jsonObject: Any?): E? {
    return JSON.parseObject(jsonObject.toString(), E::class.java)
}