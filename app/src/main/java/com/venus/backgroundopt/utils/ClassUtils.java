package com.venus.backgroundopt.utils;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/5/31
 */
public class ClassUtils {
    @SuppressWarnings("unchecked")
    private static  <E> Object castToTargetClass(Class<E> targetClass, Object obj) {
        return (E) obj;
    }
}
