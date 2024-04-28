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

package com.venus.backgroundopt.manager.application

import android.provider.Settings
import com.venus.backgroundopt.core.RunningInfo
import com.venus.backgroundopt.hook.constants.ClassConstants
import com.venus.backgroundopt.hook.constants.MethodConstants
import com.venus.backgroundopt.reference.PropertyChangeListener
import java.util.concurrent.ConcurrentHashMap

/**
 * 系统默认应用管理器
 *
 * @author XingC
 * @date 2024/3/27
 */
class DefaultApplicationManager {
    /**
     * 默认app的包名映射<DefaultApplicationManager.DEFAULT_APP_XXX, [DefaultApplicationNode]>
     */
    private val defaultAppPkgNameMap = ConcurrentHashMap<String, DefaultApplicationNode>(8)

    /* *************************************************************************
     *                                                                         *
     * 初始化Node                                                               *
     *                                                                         *
     **************************************************************************/
    fun initDefaultApplicationNode(key: String, block: (DefaultApplicationNode) -> Unit) {
        defaultAppPkgNameMap.computeIfAbsent(key) { _ ->
            DefaultApplicationNode().apply { block(this) }
        }
    }

    fun initDefaultApplicationNode(
        key: String,
        block: () -> String?,
        extraBlock: ((DefaultApplicationNode, String?) -> Unit)? = null
    ) {
        defaultAppPkgNameMap.computeIfAbsent(key) { _ ->
            val packageName = block()
            val defaultApplicationNode = DefaultApplicationNode().apply {
                value = packageName
            }
            extraBlock?.invoke(defaultApplicationNode, packageName)
            defaultApplicationNode
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 默认包名的处理                                                             *
     *                                                                         *
     **************************************************************************/
    fun setDefaultPackageName(
        key: String,
        packageName: String
    ) {
        defaultAppPkgNameMap[key]?.let { defaultApplicationNode ->
            defaultApplicationNode.value = packageName
        }
    }

    fun getDefaultPackageName(key: String): String? = defaultAppPkgNameMap[key]?.value

    fun getAllPkgNames(): Collection<String> {
        return defaultAppPkgNameMap.values.map { defaultApplicationNode ->
            defaultApplicationNode.value ?: ""
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 默认包名的包装节点                                                          *
     *                                                                         *
     **************************************************************************/
    class DefaultApplicationNode {
        @Volatile
        var value: String? = null
            set(value) {
                // 处理传入的包名
                val newPackageName = getDefaultPkgNameFromUriStr(value)
                if (field == newPackageName) {
                    return
                }

                // 执行监听
                valueChangeListeners.values.forEach { propertyChangeListener ->
                    propertyChangeListener.change(field, value)
                }
                // 更新set
                defaultAppPkgNameSet.remove(field)
                defaultAppPkgNameSet.add(value)
                // 赋值
                field = value
            }

        inline fun initValue(initBlock: () -> String) {
            this.value = initBlock()
        }

        // 值变化的监听器
        private val valueChangeListeners =
            ConcurrentHashMap<String, PropertyChangeListener<String>>(4)

        fun addListener(key: String, listener: PropertyChangeListener<String>) {
            valueChangeListeners[key] = listener;
        }

        fun removeListener(key: String) {
            valueChangeListeners.remove(key)
        }
    }

    companion object {
        // 桌面
        const val DEFAULT_APP_HOME = "HOME"

        // 浏览器
        const val DEFAULT_APP_BROWSER = "BROWSER"

        // 智能助手
        const val DEFAULT_APP_ASSISTANT = "ASSISTANT"

        // 输入法
        const val DEFAULT_APP_INPUT_METHOD = "INPUT_METHOD"

        // 短信
        const val DEFAULT_APP_SMS = "SMS"

        // 拨号
        const val DEFAULT_APP_DIALER = "DIALER"

        // 默认app的包名
        val defaultAppPkgNameSet = HashSet<String?>(8)

        /**
         * 验证所给包名对应的app是否被设置为默认应用
         * @param packageName String?
         * @return Boolean
         */
        @JvmStatic
        fun isDefaultAppPkgName(packageName: String?): Boolean =
            packageName != null && defaultAppPkgNameSet.contains(packageName)

        /**
         * 从设置的存储中获取指定默认应用的包名
         * @param key String?   设置存储中的key
         * @return String?      包名
         */
        @JvmStatic
        fun getDefaultPkgNameFromSettings(key: String?): String? {
            return getDefaultPkgNameFromUriStr(uriStr = getDefaultPkgNameFromSettingsDirectly(key))
        }

        @JvmStatic
        fun getDefaultPkgNameFromSettingsDirectly(key: String?): String? {
            if (key == null) {
                return null
            }

            return Settings.Secure.getString(
                RunningInfo.getInstance().activityManagerService.context.contentResolver,
                key
            )
        }

        /**
         * 从Uri中获取包名
         *
         * [ClassConstants.Settings_Secure].[MethodConstants.putStringForUser]传入的value值为Uri,
         * 类似 包名/xxxx。因此我们需要从中提取出包名
         *
         * @param uriStr String?    uri字符串
         * @return String?          包名
         */
        @JvmStatic
        fun getDefaultPkgNameFromUriStr(uriStr: String?): String? {
            uriStr ?: return null
            val index = uriStr.indexOf("/")
            if (index < 0) {
                return uriStr
            }

            return uriStr.substring(startIndex = 0, endIndex = index)
        }
    }
}