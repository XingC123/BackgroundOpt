package com.venus.backgroundopt.factory;

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
