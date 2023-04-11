package com.venus.backgroundopt.interfaces;

import com.venus.backgroundopt.factory.SingletonLoggerFactory;
import com.venus.backgroundopt.utils.log.Logger;


/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/10
 */
public interface ILogger {
    default Logger getLogger() {
        return SingletonLoggerFactory.getLogger(this.getClass());
    }
}
