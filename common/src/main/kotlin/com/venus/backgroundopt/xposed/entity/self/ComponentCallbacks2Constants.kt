package com.venus.backgroundopt.xposed.entity.self

/**
 * @author XingC
 * @date 2024/7/15
 */
object ComponentCallbacks2Constants {
    /* *************************************************************************
     *                                                                         *
     * 应用处于后台时可能收到的级别                                                 *
     *                                                                         *
     **************************************************************************/
    /**
     * 内存不足，并且该进程在后台进程列表最后一个，马上就要被清理
     * 表示系统内存已经非常低，你的应用即将被杀死，请释放所有可能释放的资源
     * Level for [.onTrimMemory]: the process is nearing the end
     * of the background LRU list, and if more memory isn't found soon it will
     * be killed.
     */
    const val TRIM_MEMORY_COMPLETE: Int = 80

    /**
     * 内存不足，并且该进程在后台进程列表的中部
     * 表示系统内存已经较低，当内存持续减少，你的应用可能会被杀死
     * Level for [.onTrimMemory]: the process is around the middle
     * of the background LRU list; freeing memory can help the system keep
     * other processes running later in the list for better overall performance.
     */
    const val TRIM_MEMORY_MODERATE: Int = 60

    /**
     * 内存不足，并且该进程是后台进程
     * 表示系统内存稍低，你的应用被杀的可能性不大。但可以考虑适当释放资源
     * Level for [.onTrimMemory]: the process has gone on to the
     * LRU list.  This is a good opportunity to clean up resources that can
     * efficiently and quickly be re-built if the user returns to the app.
     */
    const val TRIM_MEMORY_BACKGROUND: Int = 40


    /* *************************************************************************
     *                                                                         *
     * 应用的可见性发生变化时收到的级别                                              *
     *                                                                         *
     **************************************************************************/
    /**
     * 内存不足，并且该进程的UI已经不可见了
     * 表示应用已经处于不可见状态，可以考虑释放一些与显示相关的资源
     * Level for [.onTrimMemory]: the process had been showing
     * a user interface, and is no longer doing so.  Large allocations with
     * the UI should be released at this point to allow memory to be better
     * managed.
     */
    const val TRIM_MEMORY_UI_HIDDEN: Int = 20

    /* *************************************************************************
     *                                                                         *
     * 应用处于Running状态可能收到的级别                                            *
     *                                                                         *
     **************************************************************************/
    /**
     * 内存不足(后台进程不足3个)，并且该进程优先级比较高，需要清理内存
     * 表示系统内存已经非常低，你的应用程序应当考虑释放部分资源
     * Level for [.onTrimMemory]: the process is not an expendable
     * background process, but the device is running extremely low on memory
     * and is about to not be able to keep any background processes running.
     * Your running process should free up as many non-critical resources as it
     * can to allow that memory to be used elsewhere.  The next thing that
     * will happen after this is [.onLowMemory] called to report that
     * nothing at all can be kept in the background, a situation that can start
     * to notably impact the user.
     */
    const val TRIM_MEMORY_RUNNING_CRITICAL: Int = 15

    /**
     * 内存不足(后台进程不足5个)，并且该进程优先级比较高，需要清理内存
     * 表示系统内存已经相当低
     * Level for [.onTrimMemory]: the process is not an expendable
     * background process, but the device is running low on memory.
     * Your running process should free up unneeded resources to allow that
     * memory to be used elsewhere.
     */
    const val TRIM_MEMORY_RUNNING_LOW: Int = 10

    /**
     * 内存不足(后台进程超过5个)，并且该进程优先级比较高，需要清理内存
     * 表示系统内存已经稍低
     * Level for [.onTrimMemory]: the process is not an expendable
     * background process, but the device is running moderately low on memory.
     * Your running process may want to release some unneeded resources for
     * use elsewhere.
     */
    const val TRIM_MEMORY_RUNNING_MODERATE: Int = 5
}