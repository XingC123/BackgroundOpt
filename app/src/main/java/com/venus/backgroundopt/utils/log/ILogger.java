package com.venus.backgroundopt.utils.log;

import com.alibaba.fastjson2.annotation.JSONField;
import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.factory.SingletonLoggerFactory;


/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public interface ILogger {
    @JSONField(serialize = false)
    default Logger getLogger() {
        return SingletonLoggerFactory.getLogger(this.getClass());
    }

    static Logger getLoggerStatic(Class<?> clazz) {
        return SingletonLoggerFactory.getLogger(clazz);
    }

    /**
     * debug打印时可以进行调用
     * 实现短路功能。
     * <pre>
     *     debugLog(isDebugMode() && getLogger().debug(""))
     * </pre>
     *
     * @param tmp 这个变量并没有任何作用
     */
    default void debugLog(boolean tmp) {
    }

    /**
     * 返回是否是debug模式
     *
     * @return 是debug模式 -> true
     */
    @JSONField(serialize = false)
    default boolean isDebugMode() {
        return BuildConfig.DEBUG;
    }
}
