package com.venus.backgroundopt.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

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
            toolbar.setNavigationOnClickListener { finish() }
        }
    }

    open fun initToolBar(): Toolbar? {
        return null
    }

    abstract fun getContentView():Int

    class ToolBarBuilder {
        lateinit var title: String
        var resId: Int = Int.MIN_VALUE
    }
}