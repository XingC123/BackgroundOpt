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
