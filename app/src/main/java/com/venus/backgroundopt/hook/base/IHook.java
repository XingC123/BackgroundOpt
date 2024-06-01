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

package com.venus.backgroundopt.hook.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.utils.log.ILogger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @date 2023/11/20
 */
public abstract class IHook implements ILogger {
    /**
     * 缓存了所有hook实例, 以便通过{@link #getHookInstance(Class)}方法拿到相应实例。<br>
     * 所有的实例会在构造方法内通过{@link #addHookInstance()}方法被添加
     */
    private static final Map<Class<?>, IHook> hookInstanceMap = new HashMap<>(4);

    private static final Map<String, LinkedList<XC_MethodHook.Unhook>> methodHookPointMap = new ConcurrentHashMap<>();

    ClassLoader classLoader;

    RunningInfo runningInfo;

    public IHook(ClassLoader classLoader) {
        this(classLoader, null);
    }

    public IHook(ClassLoader classLoader, RunningInfo runningInfo) {
        this.classLoader = classLoader;
        this.runningInfo = runningInfo;

        try {
            hook();
            addHookInstance();
        } catch (Throwable throwable) {
            getLogger().error("hook失败", throwable);
        }
    }

    private void addHookInstance() {
        hookInstanceMap.put(this.getClass(), this);

        if (BuildConfig.DEBUG) {
            getLogger().debug("注册Hook实例到容器: " + this.getClass().getCanonicalName());
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public RunningInfo getRunningInfo() {
        return runningInfo;
    }

    public abstract void hook();

    /**
     * 执行hook操作 <br>
     * 主要针对{@link de.robv.android.xposed.XC_MethodHook.Unhook}, 在取消后再次设置
     */
    public void doHook() {

    }

    /**
     * 获取hook标签 <br>
     * 主要针对{@link com.venus.backgroundopt.annotation.FunctionHook}
     *
     * @return
     */
    @NonNull
    public String getHookTag() {
        return "";
    }

    @Nullable
    public static <E> E getHookInstance(Class<E> targetType) {
        try {
            return (E) hookInstanceMap.get(targetType);
        } catch (Throwable throwable) {
            return null;
        }
    }

    /**
     * 添加可取消的hook点到指定tag下
     *
     * @param tag
     * @param methodHookPoint
     */
    public static void addMethodHookPoint(@NonNull String tag, @NonNull XC_MethodHook.Unhook methodHookPoint) {
        LinkedList<XC_MethodHook.Unhook> list = methodHookPointMap.computeIfAbsent(tag, k -> new LinkedList<>());
        list.add(methodHookPoint);

        if (BuildConfig.DEBUG) {
            ILogger.getLoggerStatic(IHook.class).debug("添加 [" + tag + "] 的hook。当前tag所属list大小为: " + list.size());
        }
    }

    public static void addMethodHookPoint(@NonNull String tag, @NonNull Set<XC_MethodHook.Unhook> set) {
        LinkedList<XC_MethodHook.Unhook> list = methodHookPointMap.computeIfAbsent(tag, k -> new LinkedList<>());
        list.addAll(set);

        if (BuildConfig.DEBUG) {
            ILogger.getLoggerStatic(IHook.class).debug("添加 [" + tag + "] 的hook。当前tag所属list大小为: " + list.size());
        }
    }

    /**
     * 解除标签下所有的hook
     *
     * @param tag
     */
    public static void unhook(@NonNull String tag) {
        LinkedList<XC_MethodHook.Unhook> list = methodHookPointMap.get(tag);
        if (list != null) {
            list.forEach(XC_MethodHook.Unhook::unhook);
            list.clear();

            if (BuildConfig.DEBUG) {
                ILogger.getLoggerStatic(IHook.class).debug("取消 [" + tag + "] 的hook。当前tag所属list大小为: " + list.size());
            }
        }
    }
}