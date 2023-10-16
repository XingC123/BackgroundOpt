package com.venus.backgroundopt.ui.widget

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.venus.backgroundopt.BuildConfig
import com.venus.backgroundopt.R
import com.venus.backgroundopt.environment.newThreadTask
import com.venus.backgroundopt.utils.setNegativeBtn

/**
 * @author XingC
 * @date 2023/9/26
 */
class ProgressBarDialogBuilder {
    companion object {
        @JvmStatic
        fun createProgressBarView(
            context: Context,
            text: String,
            cancelable: Boolean = false,
            enableNegativeBtn: Boolean = true,
            negativeBtnText: String = "放弃"
        ): AlertDialog {
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress_bar, null)

            view.findViewById<TextView>(R.id.progressBarText)?.let {
                it.text = text
            }

            return AlertDialog.Builder(context)
                .setCancelable(cancelable)
                .setView(view)
                .setNegativeBtn(
                    context,
                    enableNegativeBtn,
                    negativeBtnText
                )
                .create()
        }

        @JvmStatic
        fun showProgressBarViewForAction(
            context: Context, text: String,
            cancelable: Boolean = false,
            enableNegativeBtn: Boolean = true,
            negativeBtnText: String = "放弃", action: () -> Unit
        ) {
            val dialog =
                createProgressBarView(context, text, cancelable, enableNegativeBtn, negativeBtnText)
            dialog.show()
            newThreadTask {
                try {
                    action()
                } catch (ignore: Exception) {
                    Log.e(
                        BuildConfig.APPLICATION_ID,
                        "showProgressBarViewForAction: 进度条事件执行出错",
                        ignore
                    )
                } finally {
                    dialog.dismiss()
                }
            }
        }
    }
}

fun createProgressBarView(context: Context, text: String): AlertDialog {
    return ProgressBarDialogBuilder.createProgressBarView(context, text)
}

fun showProgressBarViewForAction(
    context: Context, text: String,
    cancelable: Boolean = false,
    enableNegativeBtn: Boolean = true,
    negativeBtnText: String = "放弃", action: () -> Unit
) {
    ProgressBarDialogBuilder.showProgressBarViewForAction(
        context,
        text,
        cancelable,
        enableNegativeBtn,
        negativeBtnText,
        action
    )
}