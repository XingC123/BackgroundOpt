package com.venus.backgroundopt.factory;

import com.venus.backgroundopt.utils.log.Logger;
import com.venus.backgroundopt.utils.log.LoggerFactoryBaseXposedLog;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public class SingletonLoggerFactory {
    @SuppressWarnings("rawtypes")
    private static final WeakHashMap<Class, WeakReference<Logger>> loggerMap = new WeakHashMap<>();

    @SuppressWarnings("rawtypes")
    public static Logger getLogger(Class clazz) {
        WeakReference<Logger> weakReference = loggerMap.get(clazz);
        Logger logger = weakReference == null ? null : weakReference.get();
        if (logger == null) {
            logger = LoggerFactoryBaseXposedLog.getLogger(clazz);
            loggerMap.put(clazz, new WeakReference<>(logger));
        }

        return logger;
    }
}
