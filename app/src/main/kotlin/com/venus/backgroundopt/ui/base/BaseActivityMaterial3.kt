package com.venus.backgroundopt.ui.base

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar

/**
 * 在使用时, 仅需选择性重写此类中的方法。父类中的方法不必再次重写, 除非它是安卓的方法
 *
 * @author XingC
 * @date 2023/10/1
 */
abstract class BaseActivityMaterial3 : BaseActivity() {
    /* *************************************************************************
     *                                                                         *
     * 工具栏                                                                   *
     *                                                                         *
     **************************************************************************/
    override fun initToolBar(): Toolbar? {
        return null
    }

    /**
     * 设置菜单项的点击监听
     *
     * @param menuItem 被点击的菜单项
     */
    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
    }

    /**
     * 设置ToolBar的返回按钮事件
     *
     * @param view
     */
    override fun setNavigationOnClickListener(view: View) {
        finish()
    }

    /* *************************************************************************
     *                                                                         *
     * Activity布局的ViewId                                                     *
     *                                                                         *
     **************************************************************************/
    abstract override fun getContentView(): Int
}