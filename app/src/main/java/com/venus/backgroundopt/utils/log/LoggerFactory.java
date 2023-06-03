package com.venus.backgroundopt.utils.log;

import android.util.Log;

import com.venus.backgroundopt.BuildConfig;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class LoggerFactory {
    @SuppressWarnings("rawtypes")
    public static Logger getLogger(Class clazz) {
        if (clazz == null) {
            throw new RuntimeException("调用日志的类为空");
        }
        return new LoggerImpl(clazz.getCanonicalName());
    }

    private static class LoggerImpl implements Logger {
        private final String className;

        public LoggerImpl(String className) {
            this.className = className;
        }

        private String getTAG() {
            return BuildConfig.APPLICATION_ID;
        }

        /**
         * 0: [dalvik.system.VMStack.getThreadStackTrace(Native Method),
         * 1: java.lang.Thread.getStackTrace(Thread.java:1841),
         * 2: com.venus.backgroundopt.utils.log.LoggerFactory$LoggerImpl.getMethodName(LoggerFactory.java:35),
         * 3: com.venus.backgroundopt.utils.log.LoggerFactory$LoggerImpl.getLogMessage(LoggerFactory.java:40),
         * 4: com.venus.backgroundopt.utils.log.LoggerFactory$LoggerImpl.debug(LoggerFactory.java:76),
         * 5: com.venus.backgroundopt.hook.android.AppSwitchHook$1.beforeHookedMethod(AppSwitchHook.java:76),
         *
         * @return 调用log的方法名
         */
        private String getMethodName() {
            return className + "." + Thread.currentThread().getStackTrace()[5].getMethodName();
        }

        private String getLogMessage(String msg) {
            return getMethodName() + ": " + msg;
        }

        @Override
        public boolean info(String msg) {
            Log.i(getTAG(), getLogMessage(msg));
            return true;
        }

        @Override
        public boolean info(String msg, Throwable throwable) {
            Log.i(getTAG(), getLogMessage(msg), throwable);
            return true;
        }

        @Override
        public boolean error(String msg) {
            Log.e(getTAG(), getLogMessage(msg));
            return true;
        }

        @Override
        public boolean error(String msg, Throwable throwable) {
            Log.e(getTAG(), getLogMessage(msg), throwable);
            return true;
        }

        @Override
        public boolean warn(String msg) {
            Log.w(getTAG(), getLogMessage(msg));
            return true;
        }

        @Override
        public boolean warn(String msg, Throwable throwable) {
            Log.w(getTAG(), getLogMessage(msg), throwable);
            return true;
        }

        @Override
        public boolean debug(String msg) {
            Log.d(getTAG(), getLogMessage(msg));

            return true;
        }

        @Override
        public boolean debug(String msg, Throwable throwable) {
            Log.d(getTAG(), msg, throwable);

            return true;
        }
    }
}
