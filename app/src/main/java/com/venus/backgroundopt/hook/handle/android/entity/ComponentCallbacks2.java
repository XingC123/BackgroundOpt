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
                    
 package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.annotation.AndroidMethod;
import com.venus.backgroundopt.annotation.AndroidObjectField;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

/**
 * 封装自安卓{@link ClassConstants#ComponentCallbacks2}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/3
 */
public interface ComponentCallbacks2 {
    /* *************************************************************************
     *                                                                         *
     * 应用处于后台时可能收到的级别                                                 *
     *                                                                         *
     **************************************************************************/
    /**
     * 内存不足，并且该进程在后台进程列表最后一个，马上就要被清理
     * 表示系统内存已经非常低，你的应用即将被杀死，请释放所有可能释放的资源
     * Level for {@link #onTrimMemory(int)}: the process is nearing the end
     * of the background LRU list, and if more memory isn't found soon it will
     * be killed.
     */
    @AndroidObjectField(objectClassPath = ClassConstants.ComponentCallbacks2, fieldName = "TRIM_MEMORY_COMPLETE")
    public static final int TRIM_MEMORY_COMPLETE = 80;

    /**
     * 内存不足，并且该进程在后台进程列表的中部
     * 表示系统内存已经较低，当内存持续减少，你的应用可能会被杀死
     * Level for {@link #onTrimMemory(int)}: the process is around the middle
     * of the background LRU list; freeing memory can help the system keep
     * other processes running later in the list for better overall performance.
     */
    @AndroidObjectField(objectClassPath = ClassConstants.ComponentCallbacks2, fieldName = "TRIM_MEMORY_MODERATE")
    public static final int TRIM_MEMORY_MODERATE = 60;

    /**
     * 内存不足，并且该进程是后台进程
     * 表示系统内存稍低，你的应用被杀的可能性不大。但可以考虑适当释放资源
     * Level for {@link #onTrimMemory(int)}: the process has gone on to the
     * LRU list.  This is a good opportunity to clean up resources that can
     * efficiently and quickly be re-built if the user returns to the app.
     */
    @AndroidObjectField(objectClassPath = ClassConstants.ComponentCallbacks2, fieldName = "TRIM_MEMORY_BACKGROUND")
    public static final int TRIM_MEMORY_BACKGROUND = 40;

    /* *************************************************************************
     *                                                                         *
     * 应用的可见性发生变化时收到的级别                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * 内存不足，并且该进程的UI已经不可见了
     * 表示应用已经处于不可见状态，可以考虑释放一些与显示相关的资源
     * Level for {@link #onTrimMemory(int)}: the process had been showing
     * a user interface, and is no longer doing so.  Large allocations with
     * the UI should be released at this point to allow memory to be better
     * managed.
     */
    @AndroidObjectField(objectClassPath = ClassConstants.ComponentCallbacks2, fieldName = "TRIM_MEMORY_UI_HIDDEN")
    public static final int TRIM_MEMORY_UI_HIDDEN = 20;

    /* *************************************************************************
     *                                                                         *
     * 应用处于Running状态可能收到的级别                                            *
     *                                                                         *
     **************************************************************************/
    /**
     * 内存不足(后台进程不足3个)，并且该进程优先级比较高，需要清理内存
     * 表示系统内存已经非常低，你的应用程序应当考虑释放部分资源
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running extremely low on memory
     * and is about to not be able to keep any background processes running.
     * Your running process should free up as many non-critical resources as it
     * can to allow that memory to be used elsewhere.  The next thing that
     * will happen after this is {@link #onLowMemory()} called to report that
     * nothing at all can be kept in the background, a situation that can start
     * to notably impact the user.
     */
    @AndroidObjectField(objectClassPath = ClassConstants.ComponentCallbacks2, fieldName = "TRIM_MEMORY_RUNNING_CRITICAL")
    public static final int TRIM_MEMORY_RUNNING_CRITICAL = 15;

    /**
     * 内存不足(后台进程不足5个)，并且该进程优先级比较高，需要清理内存
     * 表示系统内存已经相当低
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running low on memory.
     * Your running process should free up unneeded resources to allow that
     * memory to be used elsewhere.
     */
    @AndroidObjectField(objectClassPath = ClassConstants.ComponentCallbacks2, fieldName = "TRIM_MEMORY_RUNNING_LOW")
    public static final int TRIM_MEMORY_RUNNING_LOW = 10;

    /**
     * 内存不足(后台进程超过5个)，并且该进程优先级比较高，需要清理内存
     * 表示系统内存已经稍低
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running moderately low on memory.
     * Your running process may want to release some unneeded resources for
     * use elsewhere.
     */
    @AndroidObjectField(objectClassPath = ClassConstants.ComponentCallbacks2, fieldName = "TRIM_MEMORY_RUNNING_MODERATE")
    public static final int TRIM_MEMORY_RUNNING_MODERATE = 5;

    /**
     * Called when the operating system has determined that it is a good
     * time for a process to trim unneeded memory from its process.  This will
     * happen for example when it goes in the background and there is not enough
     * memory to keep as many background processes running as desired.  You
     * should never compare to exact values of the level, since new intermediate
     * values may be added -- you will typically want to compare if the value
     * is greater or equal to a level you are interested in.
     *
     * <p>To retrieve the processes current trim level at any point, you can
     * use {@link android.app.ActivityManager#getMyMemoryState
     * ActivityManager.getMyMemoryState(RunningAppProcessInfo)}.
     *
     * @param level The context of the trim, giving a hint of the amount of
     *              trimming the application may like to perform.
     */
//    void onTrimMemory(@TrimMemoryLevel int level);
    @AndroidMethod(classPath = ClassConstants.ComponentCallbacks2, methodName = "onTrimMemory")
    void onTrimMemory(int level);   // 注释部分为原方法, 此处避免报错
}