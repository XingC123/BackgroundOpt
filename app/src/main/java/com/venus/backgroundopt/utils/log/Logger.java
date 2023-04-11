package com.venus.backgroundopt.utils.log;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public interface Logger {
    /* *************************************************************************
     *                                                                         *
     * info                                                                    *
     *                                                                         *
     **************************************************************************/
    void info(String msg);
    void info(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * error                                                                   *
     *                                                                         *
     **************************************************************************/
    void error(String msg);
    void error(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * warning                                                                 *
     *                                                                         *
     **************************************************************************/
    void warn(String msg);
    void warn(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * debug                                                                   *
     *                                                                         *
     **************************************************************************/
    void debug(String msg);
    void debug(String msg, Throwable throwable);
}
