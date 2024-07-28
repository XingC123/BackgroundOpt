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

import android.content.ComponentName
import android.content.Intent
import com.venus.backgroundopt.common.environment.CommonProperties
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.log.logDebug
import com.venus.backgroundopt.common.util.log.logError
import com.venus.backgroundopt.common.util.log.logInfo
import com.venus.backgroundopt.common.util.message.Message
import com.venus.backgroundopt.common.util.message.NULL_FLAG
import com.venus.backgroundopt.common.util.parseObject
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.core.RunningInfo
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.InetAddress
import java.net.ServerSocket

/**
 * @author XingC
 * @date 2024/5/29
 */
class ModuleMessageManager(
    private val runningInfo: RunningInfo,
) : IModuleMessageHandler {
    val baseModuleMessageHandler = DefaultModuleMessageHandler(runningInfo)
    private val socketModuleMessageHandler = SocketModuleMessageHandler(runningInfo)

    val socketPort: Int
        get() = socketModuleMessageHandler.port

    fun start() {
        socketModuleMessageHandler.start()
    }
}

/**
 * 模块消息处理器
 */
interface ModuleMessageHandler : ILogger {
    val runningInfo: RunningInfo
}

class DefaultModuleMessageHandler(
    override val runningInfo: RunningInfo,
) : ModuleMessageHandler {
    fun handleMessage(
        param: MethodHookParam,
    ) {
        val dataIntent = param.args[1] as Intent
        if (dataIntent.`package` != CommonProperties.PACKAGE_NAME) {
            return
        }

        // 不是UI消息
        if (dataIntent.type != Message.TYPE) {
            return
        }
        val message = dataIntent.action!!.parseObject(Message::class.java)

        registeredMessageHandler[message.key]?.handle(runningInfo, param, message.value)
        param.result = param.result?.let { result ->
            ComponentName((result as String), NULL_FLAG)
        }
    }
}

/**
 * 使用Socket进行通信的消息处理器
 */
class SocketModuleMessageHandler(
    override val runningInfo: RunningInfo,
) : ModuleMessageHandler {
    private val executor = ExecutorUtils.newFixedThreadPool(
        coreSize = 3,
        factoryName = "SocketModuleMessageHandlePool"
    )
    private val socket: ServerSocket? = initSocket()
    val port: Int
        get() = if (socket != null && !socket.isClosed) {
            socket.localPort
        } else {
            Int.MIN_VALUE
        }

    private fun initSocket(): ServerSocket? {
        var socket: ServerSocket? = null
        var curPort = 11011
        val localHost = InetAddress.getLocalHost()
        do {
            runCatchThrowable {
                socket = ServerSocket(curPort, 50, localHost)
            }
            ++curPort
        } while (socket == null && curPort <= 49152)
        return socket
    }

    fun start() {
        socket ?: run {
            logError("Socket建立失败")
            return
        }
        logInfo("Socket建立成功。端口号: ${socket.localPort}")

        executor.execute {
            val methodHookParam = MethodHookParam::class.java.getDeclaredConstructor().apply {
                isAccessible = true
            }.newInstance()
            while (true) {
                val accept = socket.accept()
                val objectInputStream = runCatchThrowable(catchBlock = { throwable ->
                    logger.error("创建消息接收流时发生异常！", throwable)
                    runCatchThrowable {
                        accept.close()
                    }
                    null
                }) {
                    ObjectInputStream(accept.getInputStream())
                } ?: continue

                executor.execute {
                    runCatchThrowable(finallyBlock = {
                        runCatchThrowable {
                            objectInputStream.close()
                            accept.close()
                        }
                    }) {
                        val message = objectInputStream.readObject() as Message
                        if (BuildConfig.DEBUG) {
                            logDebug(
                                logStr = "模块进程接收的数据为: $message"
                            )
                        }
                        registeredMessageHandler[message.key]?.handle(
                            runningInfo,
                            methodHookParam,
                            message.value.toString()
                        )
                        ObjectOutputStream(accept.getOutputStream()).use { objectOutputStream ->
                            val result = methodHookParam.result as String?
                            objectOutputStream.writeObject(result)
                        }
                    }
                }
            }
        }
    }

    class SocketMessage(
        val key: String,
        val value: String?,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    class SocketValue(
        val value: String?,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}