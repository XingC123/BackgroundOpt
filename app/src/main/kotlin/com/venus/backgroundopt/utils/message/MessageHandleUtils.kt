package com.venus.backgroundopt.utils.message

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.utils.message.handle.RunningAppInfoMessageHandler
import com.venus.backgroundopt.utils.message.handle.TargetAppGroupMessageHandler
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * ui与模块主进程通信工具类
 * 其他部分见: [MessageHandler], [MessageKeyConstants], [com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt.handleStartService]
 *
 * @author XingC
 * @date 2023/9/21
 */

const val NULL_DATA = "NULL_DATA"
const val NULL_FLAG = "NULL_FLAG"

@JvmField
val nullComponentName = ComponentName(NULL_DATA, NULL_FLAG)

// 已注册的消息处理器
@JvmField
val registeredMessageHandler = mapOf(
    MessageKeyConstants.getRunningAppInfo to RunningAppInfoMessageHandler(),
    MessageKeyConstants.getTargetAppGroup to TargetAppGroupMessageHandler(),
)

// json传输的载体
data class Message<T>(var v: T)

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
fun sendMessage(context: Context, key: String, value: Any): String? {
    context.startService(Intent().apply {
        `package` = BuildConfig.APPLICATION_ID
        action = JSON.toJSONString(Message(value))
        type = key
    })?.let {
        // componentName.packageName被征用为存放返回数据
        return if (it.packageName == NULL_DATA) null else it.packageName
    } ?: run { return null }
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
fun <E> sendMessage(context: Context, key: String, value: Any): E? {
    return try {
        sendMessage(context, key, value)?.let {
            JSON.parseObject<E>(it)
        }
    } catch (t: Throwable) {
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
@Suppress("UNCHECKED_CAST")
inline fun <E> createResponse(
    param: MethodHookParam,
    value: String?,
    setJsonData: Boolean = false,
    generateData: (value: E) -> Any
) {
    try {
        param.result = value?.let { v ->
            ((JSON.parseObject(v, Message::class.java)).v as? E)?.let {
                val d = generateData(it)
                ComponentName(
                    if (setJsonData) JSON.toJSONString(d) else d.toString(),
                    NULL_FLAG
                )
            } ?: nullComponentName
        } ?: nullComponentName
    } catch (e: Exception) {
        param.result = nullComponentName
//        Log.e(BuildConfig.APPLICATION_ID, "创建对象出错", e)
    }
}