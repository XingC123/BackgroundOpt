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
import com.venus.backgroundopt.utils.message.handle.RunningAppInfoMessageHandler
import com.venus.backgroundopt.utils.message.handle.SimpleLmkMessageHandler
import com.venus.backgroundopt.utils.message.handle.SubProcessOomConfigChangeMessageHandler
import com.venus.backgroundopt.utils.message.handle.TargetAppGroupMessageHandler
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.util.concurrent.ConcurrentHashMap

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

/**
 * 已注册的消息处理器
 * <key name, Class or instance: MessageHandler>
 */
@JvmField
val registeredMessageHandler = ConcurrentHashMap<String, Any>().apply {
    this[MessageKeyConstants.getRunningAppInfo] = RunningAppInfoMessageHandler::class.java
    this[MessageKeyConstants.getTargetAppGroup] = TargetAppGroupMessageHandler::class.java
    this[MessageKeyConstants.getBackgroundTasks] = BackgroundTasksMessageHandler::class.java
    this[MessageKeyConstants.getAppCompactList] = AppCompactListMessageHandler::class.java
    this[MessageKeyConstants.subProcessOomConfigChange] =
        SubProcessOomConfigChangeMessageHandler::class.java
    this[MessageKeyConstants.getInstalledApps] = GetInstalledPackagesMessageHandler::class.java
    this[MessageKeyConstants.autoStopCompactTask] = AutoStopCompactTaskMessageHandler::class.java
    this[MessageKeyConstants.enableForegroundProcTrimMemPolicy] =
        EnableForegroundProcTrimMemPolicyHandler::class.java
    this[MessageKeyConstants.foregroundProcTrimMemPolicy] =
        ForegroundProcTrimMemPolicyHandler::class.java
    this[MessageKeyConstants.appOptimizePolicy] = AppOptimizePolicyMessageHandler::class.java
    this[MessageKeyConstants.appWebviewProcessProtect] =
        AppWebviewProcessProtectMessageHandler::class.java
    this[MessageKeyConstants.enableSimpleLmk] = SimpleLmkMessageHandler::class.java
}

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
                "${CUR_CLASS_PREFIX}sendMessage",
                "客户端收到的原始信息: ${it.packageName}"
            )
        }
        // componentName.packageName被征用为存放返回数据
        return if (it.packageName == NULL_DATA) null else it.packageName
    } ?: run {
        if (BuildConfig.DEBUG) {
            logWarnAndroid(
                "${CUR_CLASS_PREFIX}sendMessage",
                "模块主进程回复内容为null, 无法进行转换"
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
        logErrorAndroid("${CUR_CLASS_PREFIX}sendMessage<E>", "响应消息转换失败", t)
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
        logErrorAndroid("${CUR_CLASS_PREFIX}sendMessage<E>", "响应消息转换失败", t)
        null
    }
}

/* *************************************************************************
 *                                                                         *
 * 创建响应                                                                  *
 *                                                                         *
 **************************************************************************/
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
    logDebug("${CUR_CLASS_PREFIX}createResponse", "模块进程接收的数据为: $value")
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
        logError("${CUR_CLASS_PREFIX}createResponse", "响应对象创建错误。errorMsg: $errorMsg", t)
        param.result = nullComponentName
    }
}