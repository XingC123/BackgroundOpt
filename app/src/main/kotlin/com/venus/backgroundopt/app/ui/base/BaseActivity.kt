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

package com.venus.backgroundopt.app.ui.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.common.entity.message.ModuleRunningMessage
import com.venus.backgroundopt.common.environment.CommonProperties
import com.venus.backgroundopt.common.util.ifFalse
import com.venus.backgroundopt.common.util.message.MessageKeyConstants
import com.venus.backgroundopt.common.util.message.sendMessage
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.app.utils.UiUtils
import com.venus.backgroundopt.app.utils.showProgressBarViewForAction

/**
 * @author XingC
 * @date 2023/10/1
 */
abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getContentView())

        // 初始化ToolBar
        initToolBar()?.let { toolbar ->
            // 设置返回按钮的行为
            toolbar.setNavigationOnClickListener { setNavigationOnClickListener(it) }
            // 设置菜单
            getToolBarMenusResId()?.let { menuResId ->
                toolbar.inflateMenu(menuResId)
            }
            toolbar.setOnMenuItemClickListener { menuItem ->
                setOnMenuItemClickListener(menuItem)
                true
            }
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 工具栏                                                                   *
     *                                                                         *
     **************************************************************************/
    open fun initToolBar(): Toolbar? {
        return null
    }

    /**
     * 获取菜单的资源id
     *
     * @return
     */
    open fun getToolBarMenusResId(): Int? {
        return null
    }

    /**
     * 设置菜单项的点击监听
     *
     * @param menuItem 被点击的菜单项
     */
    open fun setOnMenuItemClickListener(menuItem: MenuItem) {
    }

    /**
     * 设置ToolBar的返回按钮事件
     *
     * @param view
     */
    open fun setNavigationOnClickListener(view: View) {
        finish()
    }

    /* *************************************************************************
     *                                                                         *
     * Activity布局的ViewId                                                     *
     *                                                                         *
     **************************************************************************/
    abstract fun getContentView(): Int

    class ToolBarBuilder {
        lateinit var title: String
        var resId: Int = Int.MIN_VALUE
    }
}

fun BaseActivity.sendMessage(key: String, value: Any = ""): String? {
    return sendMessage(this, key, value)
}

inline fun <reified E> BaseActivity.sendMessage(key: String, value: Any = ""): E? {
    return sendMessage<E>(this, key, value)
}

/**
 * 检查模块后端是否运行
 * @receiver BaseActivity
 * @return Boolean 正在运行 -> true
 */
fun BaseActivity.isModuleRunning(): Boolean {
    // 如果模块未启动, 这里就是正常的调用消息通知借用的方法
    return runCatchThrowable(defaultValue = false) {
        sendMessage<ModuleRunningMessage>(
            context = this,
            key = MessageKeyConstants.moduleRunning,
            value = ModuleRunningMessage().apply {
                messageType = ModuleRunningMessage.MODULE_RUNNING
            }
        )!!.value as Boolean
    }!!
}

/**
 * 比较模块后端的版本和[targetVersionCode]
 *
 * 1. 如果 versionCode < 0 && [isNeedModuleRunning] = true, 则 -> [moduleNotRunningBlock]并退出
 * 2. 如果 versionCode < [targetVersionCode], 则 -> [incompatibleBlock]
 *     - 若 [isForcible] = false, 则 -> [compatibleBlock]
 * 3. 如果 versionCode 符合 [targetVersionCode], 则 -> [compatibleBlock]
 */
inline fun BaseActivity.ifVersionIsCompatible(
    targetVersionCode: Int,
    isNeedModuleRunning: Boolean = false,
    noinline moduleNotRunningBlock: ((BaseActivity, Int/* versionCode */) -> Unit)? = null,
    isForcible: Boolean = true,
    noinline incompatibleBlock: ((BaseActivity) -> Unit)? = null,
    crossinline compatibleBlock: (BaseActivity) -> Unit,
) {
    // 检查版本是否匹配
    showProgressBarViewForAction(text = "版本校验中...") {
        val versionCode = CommonProperties.moduleVersionCode ?: run {
            val returnVersionCode = runCatchThrowable {
                sendMessage<ModuleRunningMessage>(
                    key = MessageKeyConstants.moduleRunning,
                    value = ModuleRunningMessage().apply {
                        messageType = ModuleRunningMessage.MODULE_VERSION_CODE
                    }
                )
            }?.value as? Int ?: Int.MIN_VALUE
            CommonProperties.moduleVersionCode = returnVersionCode

            returnVersionCode
        }

        if (versionCode < 0 && isNeedModuleRunning) {
            moduleNotRunningBlock?.invoke(this, versionCode) ?: run {
                runOnUiThread {
                    UiUtils.createDialog(
                        context = this,
                        cancelable = false,
                        text = "仅模块正在运作时才可使用, 请重启系统以完全激活模块",
                        enablePositiveBtn = true,
                    ).show()
                }
            }
            return@showProgressBarViewForAction
        } else if (versionCode < targetVersionCode) {
            incompatibleBlock?.invoke(this) ?: run {
                runOnUiThread {
                    UiUtils.createDialog(
                        context = this,
                        cancelable = false,
                        text = "app与模块版本不匹配, ${if (isForcible) "请重启后再试" else "一些参数的设置需要重启后才能生效"}",
                        enablePositiveBtn = true,
                        positiveBlock = { dialogInterface, _ ->
                            dialogInterface.dismiss()
                            isForcible.ifFalse {
                                compatibleBlock(this)
                            }
                        },
                        enableNegativeBtn = !isForcible
                    ).show()
                }
            }
            return@showProgressBarViewForAction
        }

        compatibleBlock(this)
    }
}