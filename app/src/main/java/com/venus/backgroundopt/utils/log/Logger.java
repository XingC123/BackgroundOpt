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
    boolean info(String msg);
    boolean info(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * error                                                                   *
     *                                                                         *
     **************************************************************************/
    boolean error(String msg);
    boolean error(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * warning                                                                 *
     *                                                                         *
     **************************************************************************/
    boolean warn(String msg);
    boolean warn(String msg, Throwable throwable);

    /* *************************************************************************
     *                                                                         *
     * debug                                                                   *
     *                                                                         *
     **************************************************************************/
    boolean debug(String msg);
    boolean debug(String msg, Throwable throwable);
}
