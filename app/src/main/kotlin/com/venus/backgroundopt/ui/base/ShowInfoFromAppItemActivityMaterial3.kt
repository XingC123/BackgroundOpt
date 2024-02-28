package com.venus.backgroundopt.ui.base

import androidx.appcompat.widget.Toolbar
import com.venus.backgroundopt.R

/**
 * @author XingC
 * @date 2023/9/25
 */
abstract class ShowInfoFromAppItemActivityMaterial3 : ShowInfoFromAppItemActivity() {
    override fun initToolBar(): Toolbar? {
        return findViewById(R.id.toolbar)
    }
}