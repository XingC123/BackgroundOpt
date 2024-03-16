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

import com.venus.backgroundopt.exception.InstanceCreateFailedException;
import com.venus.backgroundopt.utils.log.Logger;
import com.venus.backgroundopt.utils.log.LoggerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * <a href="https://gitee.com/SecretOpen/secret-performance-desktop/blob/master/src/main/java/cn/chenc/performs/factory/SingletonFactory.java">...</a>
 *
 * @author secret
 * @version 1.0
 * @date 2023/1/16
 */
public class SingletonFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingletonFactory.class);

    @SuppressWarnings("rawtypes")
    private static final Map<Class, Object> instances = new ConcurrentHashMap<Class, Object>();
    @SuppressWarnings("rawtypes")
    private static final Map<Class, WeakReference<Object>> weakReferenceInstances = new ConcurrentHashMap<Class, WeakReference<Object>>();

    /**
     * 创建实例
     *
     * @param className  要创建的实例对象的类
     * @param paramsType 构造方法参数类型
     * @param params     构造方法参数值
     * @param <E>        要创建的实例对象的类型
     * @return 创建后的实例对象
     */
    @SuppressWarnings("rawtypes")
    public static <E> Object createInstance(Class<E> className, Class[] paramsType, Object... params) {
        try {
            if (paramsType == null || paramsType.length == 0) {
                return className.getDeclaredConstructor().newInstance();
            } else {
                return className.getDeclaredConstructor(paramsType).newInstance(params);
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.error("实例创建错误", e);
            throw new InstanceCreateFailedException();
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 获取一般实例                                                              *
     *                                                                         *
     **************************************************************************/

    /**
     * 创建可不被回收的单例模式,当没有对象引用，单例对象将被gc掉
     *
     * @param className 要创建的类的class
     * @return 创建后的实例
     */
    public static <E> E getInstance(Class<E> className) {
        return getInstance(className, null, 1);
    }

    @SuppressWarnings("unchecked rawtypes")
    public static <E> E getInstance(Class<E> className, Class[] paramsType, Object... params) {
        Object instance = instances.get(className);
        if (instance == null) {
            synchronized (SingletonFactory.class) {
                instance = instances.get(className);
                if (instance == null) {
                    instance = createInstance(className, paramsType, params);
                    instances.put(className, instance);
                }
            }
        }
        return (E) instance;
    }

    @SuppressWarnings("unchecked")
    public static <E> E getInstance(Class<E> className, Supplier<E> instanceSupplier) {
        Object instance = instances.get(className);
        if (instance == null) {
            synchronized (SingletonFactory.class) {
                instance = instances.get(className);
                if (instance == null) {
                    instance = instanceSupplier.get();
                    instances.put(className, instance);
                }
            }
        }
        return (E) instance;
    }

    /* *************************************************************************
     *                                                                         *
     * 获取弱引用实例                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * 创建可回收的单例模式,当没有对象引用，单例对象将被gc掉
     *
     * @param className 要创建的类的class
     * @return 创建后的实例
     */
    public static <E> E getWeakInstance(Class<E> className) {
        return getWeakInstance(className, null, 1);
    }

    /**
     * 创建可回收的单例模式,当没有对象引用，单例对象将被gc掉
     *
     * @param className  要创建的类的class
     * @param paramsType 参数的class
     * @param params     参数的值
     * @param <E>        创建后实例的泛型
     * @return 创建后的实例
     */
    @SuppressWarnings("unchecked rawtypes")
    public static <E> E getWeakInstance(Class<E> className, Class[] paramsType, Object... params) {
        WeakReference<Object> reference = weakReferenceInstances.get(className);
        Object instance = reference == null ? null : reference.get();
        if (instance == null) {
            synchronized (SingletonFactory.class) {
                reference = weakReferenceInstances.get(className);
                instance = reference == null ? null : reference.get();
                if (instance == null) {
                    instance = createInstance(className, paramsType, params);
                    weakReferenceInstances.put(className, new WeakReference<>(instance));
                }
            }
        }
        return (E) instance;
    }

    @SuppressWarnings("unchecked")
    public static <E> E getWeakInstance(Class<E> className, Supplier<E> instanceSupplier) {
        WeakReference<Object> reference = weakReferenceInstances.get(className);
        Object instance = reference == null ? null : reference.get();
        if (instance == null) {
            synchronized (SingletonFactory.class) {
                reference = weakReferenceInstances.get(className);
                instance = reference == null ? null : reference.get();
                if (instance == null) {
                    instance = instanceSupplier.get();
                    weakReferenceInstances.put(className, new WeakReference<>(instance));
                }
            }
        }
        return (E) instance;
    }
}