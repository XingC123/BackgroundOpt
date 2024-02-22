package com.venus.backgroundopt.ui.base

import android.os.Bundle
import com.venus.backgroundopt.ui.SettingsPreferenceFragment

/**
 * 需要实现设置页面的Activity可继承自本类
 *
 * @author XingC
 * @date 2023/10/16
 */
abstract class PreferenceActivityMaterial3 : BaseActivityMaterial3() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.beginTransaction()
            .add(preferenceFragmentContainerResId(), SettingsPreferenceFragment()).commit()
    }

    abstract fun preferenceFragmentContainerResId(): Int
}