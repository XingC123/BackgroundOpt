package com.venus.backgroundopt.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.venus.backgroundopt.R
import com.venus.backgroundopt.utils.message.MessageKeyConstants
import com.venus.backgroundopt.utils.message.sendMessage
import com.venus.backgroundopt.utils.log.ILogger


class MainActivity : AppCompatActivity(), ILogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        packageNameText = findViewById(R.id.packageNameText)

        findViewById<Button>(R.id.recordedProcessInfoBtn)?.setOnClickListener { _ ->
            // java.lang.NoClassDefFoundError: Failed resolution of: Lde/robv/android/xposed/XposedHelpers;
//            val ams = XposedHelpers.callStaticMethod(
//                ActivityManager::class.java,
//                "getService"
//            )
//            val result = XposedHelpers.callMethod(
//                ams, "startService",
//                null,
//                service,
//                MessageKeyConstants.isRecordedProcessInfo,      // resolvedType
//                false,                                          // requireForeground
//                BuildConfig.APPLICATION_ID,                     // callingPackage
//                null,                                           // callingFeatureId
//                0                                               // userId
//            ) as ComponentName

            val result = sendMessage(
                this,
                MessageKeyConstants.isRecordedProcessInfo,
                packageNameText.text.toString().toInt()
            )
            packageNameText.setText("结果是: $result")
        }
    }

    private lateinit var packageNameText: EditText
}