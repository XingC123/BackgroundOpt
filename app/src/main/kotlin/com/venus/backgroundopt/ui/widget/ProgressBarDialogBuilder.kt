package com.venus.backgroundopt.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.venus.backgroundopt.R

/**
 * @author XingC
 * @date 2023/9/26
 */
class ProgressBarDialogBuilder {
    companion object {
        @JvmStatic
        fun createProgressBarView(context: Context, text: String): AlertDialog {
            val view = LayoutInflater.from(context).inflate(R.layout.progress_bar_dialog, null)

            view.findViewById<TextView>(R.id.progressBarText)?.let {
                it.text = text
            }

            return AlertDialog.Builder(context)
                .setCancelable(true)
                .setView(view)
                .create()
        }
    }
}