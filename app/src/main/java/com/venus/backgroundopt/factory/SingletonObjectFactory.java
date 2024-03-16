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

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/1/16
 */
public class SingletonObjectFactory {
    @SuppressWarnings("rawtypes")
    private static final Map<Class, WeakReference<Object>> weakReferenceInstances = new ConcurrentHashMap<>();

    public static <M> void setObject(Class<M> clazz, Object obj) {
        weakReferenceInstances.put(clazz, new WeakReference<>(obj));
    }

    @SuppressWarnings("unchecked")
    public static <M, E> E getObject(Class<M> clazz, Class<E> targetType) {
        WeakReference<Object> reference = weakReferenceInstances.get(clazz);
        return reference == null ? null : (E) reference.get();
    }

    public static <M> void remove(Class<M> clazz) {
        weakReferenceInstances.remove(clazz);
    }
}