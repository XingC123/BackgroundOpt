package com.venus.backgroundopt.ui

import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.ui.base.PreferenceActivity
import com.venus.backgroundopt.utils.UiUtils

/**
 * @author XingC
 * @date 2023/10/16
 */
class SettingsActivity : PreferenceActivity() {
    override fun preferenceFragmentContainerResId(): Int {
        return R.id.settingsActivityContainer
    }

    override fun getContentView(): Int {
        return R.layout.activity_settings
    }

    override fun initToolBar(): Toolbar? {
        return UiUtils.getToolbar(
            this,
            R.id.toolbarLeftTitleToolbar,
            titleResId = R.string.menu_main_activity_toolbar_settings,
            enableCancelBtn = true
        )
    }
}