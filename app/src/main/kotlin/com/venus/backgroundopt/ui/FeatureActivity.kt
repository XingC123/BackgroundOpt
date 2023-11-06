package com.venus.backgroundopt.ui

import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.utils.UiUtils

/**
 * @author XingC
 * @date 2023/11/3
 */
class FeatureActivity : BaseActivity() {
    override fun getContentView(): Int {
        return R.layout.activity_feature
    }

    override fun initToolBar(): Toolbar? {
        return UiUtils.getToolbar(
            this,
            R.id.toolbarLeftTitleToolbar,
            titleResId = R.string.menu_main_activity_toolbar_feature,
            enableCancelBtn = true
        )
    }
}