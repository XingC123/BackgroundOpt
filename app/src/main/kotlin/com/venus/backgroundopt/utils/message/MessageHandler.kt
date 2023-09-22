package com.venus.backgroundopt.utils.message

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.BuildConfig
import de.robv.android.xposed.XC_MethodHook.MethodHookParam

/**
 * @author XingC
 * @date 2023/9/21
 */

const val NULL_DATA = "NULL_DATA"
const val NULL_FLAG = "NULL_FLAG"

@JvmField
val nullComponentName = ComponentName(NULL_DATA, NULL_FLAG)

data class Message<T>(var v: T)

/**
 * 发送消息
 *
 * @param context 需要上下文来调用方法
 * @param key 信息所在的key
 * @param value 信息
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
 * 创建响应
 *
 * @param param hook的方法的原生参数
 * @param value ui发送过来的消息
 * @param generateData 生成数据使用的方法
 */
@Suppress("UNCHECKED_CAST")
fun <E> createResponse(
    param: MethodHookParam,
    value: String?,
    generateData: (value: E) -> Any
) {
    try {
        param.result = value?.let { v ->
            ((JSON.parseObject(v, Message::class.java)).v as? E)?.let {
                ComponentName(
                    generateData(it).toString(),
                    NULL_FLAG
                )
            } ?: nullComponentName
        } ?: nullComponentName
    } catch (e: Exception) {
        param.result = nullComponentName
        Log.e(BuildConfig.APPLICATION_ID, "创建对象出错", e)
    }
}