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

import android.content.Context
import android.content.Intent
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt
import com.venus.backgroundopt.manager.message.SocketModuleMessageHandler
import com.venus.backgroundopt.utils.JsonUtils
import com.venus.backgroundopt.utils.log.logError
import com.venus.backgroundopt.utils.log.logErrorAndroid
import com.venus.backgroundopt.utils.log.logInfoAndroid
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
import com.venus.backgroundopt.utils.message.handle.KeepMainProcessAliveHasActivityMessageHandler
import com.venus.backgroundopt.utils.message.handle.KillAfterRemoveTaskMessageHandler
import com.venus.backgroundopt.utils.message.handle.ModuleRunningMessageHandler
import com.venus.backgroundopt.utils.message.handle.ProcessRunningInfoMessageHandler
import com.venus.backgroundopt.utils.message.handle.RunningAppInfoMessageHandler
import com.venus.backgroundopt.utils.message.handle.SimpleLmkMessageHandler
import com.venus.backgroundopt.utils.message.handle.SubProcessOomConfigChangeMessageHandler
import com.venus.backgroundopt.utils.message.handle.TargetAppGroupMessageHandler
import com.venus.backgroundopt.utils.runCatchThrowable
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.Executors

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

/* *************************************************************************
 *                                                                         *
 * 消息发送器                                                                *
 *                                                                         *
 **************************************************************************/
/**
 * 消息发送器接口
 */
interface IMessageSender {
    /**
     * 以指定的key[key] ([MessageKeyConstants])发送消息[value], 得到可空的字符串
     */
    fun send(key: String, value: Any = ""): String?

    /**
     * 以指定的key[key] ([MessageKeyConstants])发送消息[value], 并将响应的字符串转换为指定格式[E]
     */
    fun <E> sendAndParse(type: Class<E>, key: String, value: Any = ""): E? {
        return parseObject(
            type = type,
            jsonStr = send(
                key = key,
                value = value
            )
        )
    }

    companion object {
        /**
         * 从json字符串[jsonStr]转换得到[E]的对象
         */
        @JvmStatic
        fun <E> parseObject(jsonStr: String?, type: Class<E>): E? {
            return jsonStr?.let {
                runCatchThrowable {
                    JsonUtils.parseObject(jsonStr, type)
                }
            }
        }

        /**
         * 使用默认的消息发送实现
         *
         * @param context Context
         * @param key String
         * @param value Any
         * @return E?
         */
        @JvmStatic
        inline fun <reified E> sendDefault(context: Context, key: String, value: Any = ""): E? {
            val string = context.startService(Intent().apply {
                `package` = BuildConfig.APPLICATION_ID
                action = JsonUtils.toJsonString(
                    Message(
                        key = key,
                        value = JsonUtils.toJsonString(value)
                    )
                )
                type = Message.TYPE
            })?.packageName
            return parseObject(
                jsonStr = string,
                type = E::class.java
            )
        }
    }
}

class NoImplMessageSender : IMessageSender {
    override fun send(key: String, value: Any): String? = null
}

class DefaultMessageSender(
    private val context: Context,
) : IMessageSender {
    override fun send(key: String, value: Any): String? {
        return IMessageSender.sendDefault(
            context = context,
            key = key,
            value = value
        )
    }
}

class SocketMessageSender(
    val socketPort: Int,
) : IMessageSender {
    override fun send(key: String, value: Any): String? {
        val socket = Socket(InetAddress.getLocalHost(), socketPort)
        val objectOutputStream = ObjectOutputStream(socket.getOutputStream())
        // 发送消息
        objectOutputStream.writeObject(
            Message(
                key = key,
                value = JsonUtils.toJsonString(value)
            )
        )
        // 获取返回值
        val objectInputStream = ObjectInputStream(socket.getInputStream())
        val responseStr = objectInputStream.readObject() as String?

        objectInputStream.close()
        objectOutputStream.close()
        socket.close()

        return responseStr
    }
}

/**
 * 消息发送器
 */
class MessageSender {
    private lateinit var sender: IMessageSender
    private val executor = Executors.newFixedThreadPool(3)

    fun init(context: Context, socketPort: Int) {
        executor.execute {
            // 支持socket传输
            if (socketPort == Int.MIN_VALUE) {
                logWarnAndroid("无任何消息实现~")
                sender = NoImplMessageSender()
            } else if (SocketModuleMessageHandler.isPortValid(socketPort)) {
                logInfoAndroid("Socket通信~")
                sender = SocketMessageSender(socketPort = socketPort)
            } else {
                logInfoAndroid("传统通信~")
                sender = DefaultMessageSender(context = context)
            }
        }
    }

    /**
     * 发送消息
     *
     * [sender]的不同会调用不同的通信方式
     * @param key String
     * @param value Any
     * @return ComponentName?
     */
    fun send(key: String, value: Any = ""): String? = sender.send(key = key, value = value)

    fun <E> send(type: Class<E>, key: String, value: Any = ""): E? {
        return sender.sendAndParse(
            type = type,
            key = key,
            value = value
        )
    }

    /**
     * 根据[sender]的类型决定是否启用独立的线程来执行[block]
     */
    fun autoDetectUseThread(block: () -> Unit) {
        when (sender) {
            is SocketMessageSender -> {
                executor.execute(block)
            }

            is DefaultMessageSender -> {
                block()
            }

            else -> {}
        }
    }
}

@JvmField
val messageSender = MessageSender()

// 已注册的消息处理器
val registeredMessageHandler by lazy {
    mapOf(
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
        MessageKeyConstants.KEEP_MAIN_PROCESS_ALIVE_HAS_ACTIVITY to KeepMainProcessAliveHasActivityMessageHandler(),
        MessageKeyConstants.getProcessRunningInfo to ProcessRunningInfoMessageHandler(),
    )
}

// json传输的载体
data class Message(
    var value: String?,
    var key: String? = null
) : MessageFlag, Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
        const val TYPE = "backgroundopt.message"
    }
}

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
    return messageSender.send(key = key, value = value)
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
    return messageSender.send(
        type = E::class.java,
        key = key,
        value = value
    )
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
    var errorMsg: String? = null
    var result: String? = null
    try {
        value?.let { _ ->
            errorMsg = "Message转换异常"

            JSON.parseObject(value, E::class.java)?.let { eData ->
                errorMsg = "数据处理异常"
                generateData(eData)?.let { responseObj ->
                    result = if (setJsonData)
                        JSON.toJSONString(responseObj)
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