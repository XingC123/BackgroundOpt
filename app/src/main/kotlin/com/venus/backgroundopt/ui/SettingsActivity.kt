package com.venus.backgroundopt.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.entity.AllPreference
import com.venus.backgroundopt.ui.base.PreferenceActivity
import com.venus.backgroundopt.utils.UiUtils

/**
 * @author XingC
 * @date 2023/10/16
 */
class SettingsActivity : PreferenceActivity() {
    private lateinit var allPreference: AllPreference

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

    override fun getToolBarMenusResId(): Int {
        return R.menu.menu_settings_activity_toolbar
    }

    override fun setOnMenuItemClickListener(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.settingsToolbarBackupMenuItem -> {
                allPreference.backup()
            }

            R.id.settingsToolbarRestoreMenuItem -> {
                allPreference.restore()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        allPreference = AllPreference(this)
    }
}