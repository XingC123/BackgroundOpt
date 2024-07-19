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
                    
 package com.venus.backgroundopt.common.factory;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * 键和值均为弱引用。放置在此集合中的数据非常不必要, 可以随时重建或销毁
 *
 * @author XingC
 * @version 1.0
 * @date 2023/2/6
 */
public class WeakReferenceMap {
    @SuppressWarnings("rawtypes")
    private static final WeakHashMap<Class, WeakReference<Object>> weakHashMap = new WeakHashMap<>();

    public static <M> void setObject(Class<M> master, Object obj) {
        weakHashMap.put(master, new WeakReference<>(obj));
    }

    @SuppressWarnings("unchecked")
    public static <M, E> E getObject(Class<M> master, Class<E> objType) {
        WeakReference<Object> weakReference = weakHashMap.get(master);
        return weakReference == null ? null : (E) weakReference.get();
    }

    @SuppressWarnings("unchecked rawtypes")
    public static <M, E> E getObject(Class<M> master, Class<E> objType, Class[] paramTypes, Object... params) {
        WeakReference<Object> weakReference = weakHashMap.get(master);
        Object instance = weakReference == null ? null : (E) weakReference.get();
        if (instance == null) {
            synchronized (WeakReferenceMap.class) {
                weakReference = weakHashMap.get(master);
                instance = weakReference == null ? null : (E) weakReference.get();

                if (instance == null) {
                    instance = SingletonFactory.createInstance(objType, paramTypes, params);
                    setObject(master, instance);
                }
            }
        }

        return (E) instance;
    }
}