/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
                    
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