package com.venus.backgroundopt.core;

import com.venus.backgroundopt.factory.SingletonObjectFactory;
import com.venus.backgroundopt.utils.log.Logger;
import com.venus.backgroundopt.utils.log.LoggerFactory;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class GetBean {
    public static Logger getLogger(Object clazz) {
        Logger logger = SingletonObjectFactory.getObject(clazz.getClass(), Logger.class);
        if (logger == null) {
            logger = LoggerFactory.getLogger(clazz.getClass());
            SingletonObjectFactory.setObject(clazz.getClass(), logger);
        }
        return logger;
    }
}
