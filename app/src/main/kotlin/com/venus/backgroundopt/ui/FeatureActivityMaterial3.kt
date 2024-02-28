package com.venus.backgroundopt.ui

import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R
import com.venus.backgroundopt.ui.base.BaseActivity
import com.venus.backgroundopt.ui.base.BaseActivityMaterial3
import com.venus.backgroundopt.utils.UiUtils

/**
 * @author XingC
 * @date 2023/11/3
 */
class FeatureActivityMaterial3 : BaseActivityMaterial3() {
    override fun getContentView(): Int {
        return R.layout.activity_feature_material3
    }

    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.toolbar)
    }
}