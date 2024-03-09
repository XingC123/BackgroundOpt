package com.venus.backgroundopt.utils.log;

import com.venus.backgroundopt.BuildConfig;

import de.robv.android.xposed.XposedBridge;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class LoggerFactoryBaseXposedLog {
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
            return /*getMethodName() + ": " + */msg;
        }

        private void log(String flag, String msg) {
            XposedBridge.log("Backgroundopt --> [" + flag + "]: " + getLogMessage(msg));
        }

        private void logThrowable(String flag, String msg, Throwable throwable) {
            log(flag, msg);
            XposedBridge.log(throwable);
        }

        @Override
        public boolean info(String msg) {
            log("I", msg);
            return true;
        }

        @Override
        public boolean info(String msg, Throwable throwable) {
            logThrowable("I", msg, throwable);
            return true;
        }

        @Override
        public boolean error(String msg) {
            log("E", msg);
            return true;
        }

        @Override
        public boolean error(String msg, Throwable throwable) {
            logThrowable("E", msg, throwable);
            return true;
        }

        @Override
        public boolean warn(String msg) {
            log("W", msg);
            return true;
        }

        @Override
        public boolean warn(String msg, Throwable throwable) {
            logThrowable("W", msg, throwable);
            return true;
        }

        @Override
        public boolean debug(String msg) {
            log("D", msg);

            return true;
        }

        @Override
        public boolean debug(String msg, Throwable throwable) {
            logThrowable("D", msg, throwable);

            return true;
        }
    }
}
