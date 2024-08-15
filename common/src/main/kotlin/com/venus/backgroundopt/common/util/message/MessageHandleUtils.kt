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

package com.venus.backgroundopt.common.util.message

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.TextView
import com.alibaba.fastjson2.JSON
import com.venus.backgroundopt.common.BuildConfig
import com.venus.backgroundopt.common.environment.PreferenceDefaultValue
import com.venus.backgroundopt.common.environment.constants.PreferenceKeyConstants
import com.venus.backgroundopt.common.environment.constants.PreferenceNameConstants
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.log.logErrorAndroid
import com.venus.backgroundopt.common.util.log.logInfoAndroid
import com.venus.backgroundopt.common.util.log.logWarnAndroid
import com.venus.backgroundopt.common.util.parseObject
import com.venus.backgroundopt.common.util.preference.prefBoolean
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.common.util.toJsonString
import com.venus.backgroundopt.xposed.manager.message.IModuleMessageHandler
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.InetAddress
import java.net.Socket

/**
 * ui与模块主进程通信工具类
 *
 * @author XingC
 * @date 2023/9/21
 */

const val NULL_DATA = "NULL_DATA"
const val NULL_FLAG = "NULL_FLAG"

const val CUR_CLASS_PREFIX = "com.venus.backgroundopt.common.util.message.MessageHandleUtils."

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
                    jsonStr.parseObject(type)
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
                action = Message(
                    key = key,
                    value = value.toJsonString()
                ).toJsonString()
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
                value = value.toJsonString()
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
    private val executor = ExecutorUtils.newFixedThreadPool(
        coreSize = 3,
        factoryName = "MessageSenderPool"
    )

    fun init(
        context: Context,
        socketPort: Int?,
        socketPortText: TextView?,
    ) {
        executor.execute {
            var socketPortStr = "null"

            val enableSecondaryMessageSender = context.prefBoolean(
                name = PreferenceNameConstants.MAIN_SETTINGS,
                key = PreferenceKeyConstants.SECONDARY_MESSAGE_SENDER,
                defaultValue = PreferenceDefaultValue.enableSecondaryMessageSender
            )
            if (enableSecondaryMessageSender) {
                logInfoAndroid("传统通信~")
                sender = DefaultMessageSender(context = context)
                socketPortStr = "次级消息发送器"
            } else if (socketPort == null) {
                logWarnAndroid("无任何消息实现~")
                sender = NoImplMessageSender()
                socketPortStr = "重启后生效..."
            }
            // 支持socket传输
            else if (IModuleMessageHandler.isPortValid(socketPort)) {
                logInfoAndroid("Socket通信~")
                sender = SocketMessageSender(socketPort = socketPort)
                socketPortStr = socketPort.toString()
            } else {
                logInfoAndroid("传统通信~")
                sender = DefaultMessageSender(context = context)
                socketPortStr = "其他实现"
            }

            (context as? Activity)?.runOnUiThread {
                socketPortText?.text = socketPortStr
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

// json传输的载体
data class Message(
    var value: String?,
    var key: String? = null,
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
    value: Any = "",
): MutableList<E>? {
    return try {
        sendMessage(context, key, value)?.let {
            JSON.parseArray(it, E::class.java)
        }
    } catch (t: Throwable) {
        logErrorAndroid(
            // methodName = "${CUR_CLASS_PREFIX}sendMessage<E>",
            logStr = "响应消息转换失败",
            t = t
        )
        null
    }
}