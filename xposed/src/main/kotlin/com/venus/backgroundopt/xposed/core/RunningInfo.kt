package com.venus.backgroundopt.xposed.core

import android.content.pm.ApplicationInfo
import com.venus.backgroundopt.common.preference.PropertyChangeListener
import com.venus.backgroundopt.common.util.concurrent.ConcurrentUtils
import com.venus.backgroundopt.common.util.concurrent.ExecutorUtils
import com.venus.backgroundopt.common.util.concurrent.lock.writeLock
import com.venus.backgroundopt.common.util.ifFalse
import com.venus.backgroundopt.common.util.ifNull
import com.venus.backgroundopt.common.util.log.ILogger
import com.venus.backgroundopt.common.util.runCatchThrowable
import com.venus.backgroundopt.common.util.unsafeLazy
import com.venus.backgroundopt.xposed.BuildConfig
import com.venus.backgroundopt.xposed.annotation.OriginalObject
import com.venus.backgroundopt.xposed.entity.android.com.android.internal.util.MemInfoReader
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ActivityManagerService
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ProcessRecord
import com.venus.backgroundopt.xposed.entity.android.com.android.server.pm.PackageManagerService
import com.venus.backgroundopt.xposed.entity.android.com.android.server.power.PowerManagerService
import com.venus.backgroundopt.xposed.entity.android.com.android.server.wm.ActivityRecord
import com.venus.backgroundopt.xposed.entity.self.AppInfo
import com.venus.backgroundopt.xposed.entity.self.FindAppResult
import com.venus.backgroundopt.xposed.entity.self.getOrCreateAppInfo
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants
import com.venus.backgroundopt.xposed.manager.application.DefaultApplicationManager
import com.venus.backgroundopt.xposed.manager.application.DefaultApplicationManager.DefaultApplicationNode
import com.venus.backgroundopt.xposed.manager.message.ModuleMessageManager
import com.venus.backgroundopt.xposed.manager.process.ProcessManager
import com.venus.backgroundopt.xposed.point.android.function.ActivitySwitchHook
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import kotlin.concurrent.Volatile

/**
 * 模块运行时的中心调度
 *
 * @author XingC
 * @date 2023/2/10
 */
class RunningInfo(
    val classLoader: ClassLoader,
) : ILogger {
    init {
        runningInfo = this
    }

    val memInfoReader: MemInfoReader? by unsafeLazy { MemInfoReader.getInstance() }

    /* *************************************************************************
     *                                                                         *
     * 封装的activityManagerService                                             *
     *                                                                         *
     **************************************************************************/
    lateinit var activityManagerService: ActivityManagerService

    /* *************************************************************************
     *                                                                         *
     * ApplicationInfo的结果集                                                   *
     *                                                                         *
     **************************************************************************/
    /**
     * 每次打开app, 都会获取[android.content.pm.ApplicationInfo]。本集合用来缓存结果。
     * - key: 主用户 -> packageName, 其他用户: userId:packageName
     * - value: [FindAppResult]
     *
     * 例:
     * 1) (userId = 0, packageName=com.venus.aaa) -> (key): com.venus.aaa
     * 2) (userId = 999, packageName=com.venus.aaa) -> (key): com.venus.aaa:999
     */
    private val findAppResultMap: MutableMap<String, FindAppResult> = ConcurrentHashMap()

    fun isImportantSystemApp(userId: Int, packageName: String): Boolean {
        return runCatchThrowable(catchBlock = { throwable: Throwable ->
            logger.error(
                "判断是否是重要app出错: userId: $userId, packageName: $packageName",
                throwable
            )
            false
        }) {
            getFindAppResult(userId, packageName).importantSystemApp
        }!!
    }

    private fun getFindAppResult(
        userId: Int,
        packageName: String,
        key: String = getNormalAppKey(userId, packageName),
        applicationInfo: ApplicationInfo? = null,
    ): FindAppResult {
        return findAppResultMap.computeIfAbsent(key) { _ ->
            activityManagerService.getFindAppResult(userId, packageName, applicationInfo)
        }
    }

    /**
     * 获取[findAppResultMap]的key
     */
    private fun getNormalAppKey(userId: Int, packageName: String): String {
        return getAppKey(userId, packageName)
    }

    private fun getAppKey(userId: Int, packageName: String): String {
        return if (userId == ActivityManagerService.MAIN_USER) packageName else "$userId:$packageName"
    }

    /**
     * 移除给定key匹配的[FindAppResult]
     *
     * @param userId      用户id
     * @param packageName 包名
     */
    fun removeRecordedFindAppResult(userId: Int, packageName: String) {
        val key = getNormalAppKey(userId, packageName)
        findAppResultMap.remove(key)

        if (BuildConfig.DEBUG) {
            logger.debug("移除匹配的app记录: userId: $userId, 包名: $packageName")
        }
    }

    fun removeAllRecordedFindAppResult(packageName: String) {
        findAppResultMap.keys.asSequence()
            .filter { key: String ->
                key.contains(
                    packageName
                )
            }
            .forEach { key: String? ->
                findAppResultMap.remove(
                    key
                )
            }
        if (BuildConfig.DEBUG) {
            logger.debug("移除所有匹配的app记录: 包名: $packageName")
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的应用                                                                *
     *                                                                         *
     **************************************************************************/
    /**
     * 运行中的app
     * <userId></userId>+packageName, AppInfo>
     */
    private val runningApps: MutableMap<String, AppInfo> = ConcurrentHashMap<String, AppInfo>()
    val runningAppInfos get() = runningApps.values

    /**
     * 获取运行中的app的标识符
     */
    private fun getRunningAppIdentifier(userId: Int, packageName: String): String {
        return getAppKey(userId, packageName)
    }

    /**
     * 根据[userId], [packageName]获取正在运行的列表中的app信息
     */
    // 在2024.2.18的commit, 即"ApplicationInfo的结果集"初次进版时, runningApps的value不会再有null
    // 但为了保证后续兼容性, 仍然注解为Nullable
    fun getRunningAppInfo(userId: Int, packageName: String): AppInfo? {
        return getRunningAppInfo(getRunningAppIdentifier(userId, packageName))
    }

    private fun getRunningAppInfo(key: String): AppInfo? = runningApps[key]

    /**
     * 若 [.runningApps] 中没有此uid记录, 将进行计算创建
     *
     * @param userId      目标app对应用户id
     * @param uid         目标app的uid
     * @return 生成的目标app信息
     */
    private fun computeRunningAppIfAbsent(
        userId: Int,
        packageName: String,
        uid: Int,
        applicationInfo: ApplicationInfo,
    ): AppInfo {
        return runningApps.computeIfAbsent(
            getRunningAppIdentifier(userId, packageName)
        ) { _: String? ->
            if (BuildConfig.DEBUG) {
                logger.debug("创建新App记录: $packageName, uid: $uid")
            }
            getFindAppResult(
                userId = userId,
                packageName = packageName,
                applicationInfo = applicationInfo
            ).getOrCreateAppInfo { findAppResult: FindAppResult ->
                AppInfo.newInstance(
                    uid = uid,
                    userId = userId,
                    packageName = packageName,
                    findAppResult = findAppResult,
                    runningInfo = this
                )
            }
        }
    }

    /**
     * 根据[AppInfo]从运行列表中移除
     *
     * @param appInfo app信息
     */
    fun removeRunningApp(appInfo: AppInfo) {
        // 设置内存分组到死亡分组
        appInfo.appGroupEnum = AppGroupEnum.DEAD

        val packageName: String = appInfo.packageName ?: return
        runCatchThrowable(catchBlock = { throwable: Throwable ->
            logger.error(
                "杀死app(userId: ${appInfo.userId}, packageName: ${packageName})出现错误",
                throwable
            )
        }) {
            // 从运行列表移除
            val remove: AppInfo? = runningApps.remove(
                getRunningAppIdentifier(appInfo.userId, packageName)
            )
            if (remove != null) {
                // 从待处理列表中移除
                /*activeAppGroup.remove(appInfo)
                idleAppGroup.remove(appInfo)*/

                // app被杀死
                processManager.appDie(remove)

                // 清理AppInfo。也许有助于gc
                appInfo.clearAppInfo()

                if (BuildConfig.DEBUG) {
                    logger.debug("kill: userId: ${appInfo.userId}, packageName: ${packageName} >>> 杀死App")
                }
            } else {
                if (BuildConfig.DEBUG) {
                    logger.warn("kill: 未找到移除项 -> userId: ${appInfo.userId}, packageName: ${packageName}")
                }
            }
        }
    }

    fun forceStopRunningApp(appInfo: AppInfo) {
        val packageName: String = appInfo.packageName ?: return
        val userId = appInfo.userId
        runCatchThrowable(catchBlock = { throwable: Throwable ->
            logger.error(
                "强制停止app出错(userId: ${userId}, packageName: ${packageName})",
                throwable
            )
        }) {
            activityManagerService.forceStopPackage(packageName, userId)
        }
    }

    /* *************************************************************************
     *                                                                         *
     * 运行的进程                                                                *
     *                                                                         *
     **************************************************************************/
    private val runningProcesses: MutableMap<Int, ProcessRecord> = ConcurrentHashMap()
    val runningProcessList get() = runningProcesses.values

    fun getRunningProcess(pid: Int): ProcessRecord? = runningProcesses[pid]

    fun getRunningProcess(@OriginalObject process: Any): ProcessRecord? {
        return getRunningProcess(ProcessRecord.getPid(process))
    }

    private fun putIntoRunningProcesses(pid: Int, processRecord: ProcessRecord) {
        runningProcesses[pid] = processRecord
    }

    private fun computeProcessIfAbsent(pid: Int, @OriginalObject process: Any): ProcessRecord {
        return runningProcesses.computeIfAbsent(pid) { _: Int? ->
            val userId = ProcessRecord.getUserId(process)
            val uid = ProcessRecord.getUID(process)
            val applicationInfo = ProcessRecord.getApplicationInfo(process)
            val packageName = applicationInfo.packageName

            val appInfo = computeRunningAppIfAbsent(userId, packageName, uid, applicationInfo)
            ProcessRecord.newInstance(
                appInfo = appInfo,
                processRecord = process,
                pid = pid,
                uid = uid,
                userId = userId,
                packageName = packageName
            )
        }
    }

    fun removeRunningProcess(pid: Int): ProcessRecord? = runningProcesses.remove(pid)

    /**
     * 进程创建
     *
     * @param proc        安卓的[ClassConstants.ProcessRecord]
     */
    fun startProcess(@OriginalObject proc: Any, pid: Int) {
        addActivityEventChangeAction {
            runCatchThrowable {
                computeProcessIfAbsent(pid, proc)
            }
        }
    }

    /**
     * 移除进程。<br></br>
     * 额外处理与进程相关的任务。
     *
     * @param pid pid
     */
    fun removeProcess(pid: Int) {
        val processRecord: ProcessRecord = getRunningProcess(pid) ?: return
        val appInfo: AppInfo = processRecord.appInfo
        val packageName = appInfo.packageName
        val isMainProcess: Boolean = processRecord.mainProcess

        // 移除进程记录
        removeRunningProcess(pid)
        // 移除内存压缩文件流的缓存
        activityManagerService.oomAdjuster.cachedAppOptimizer.removeCompactOutputStreams(pid)

        appInfo.processes.remove(processRecord)
        if (appInfo.processCount == 0) {
            removeRunningApp(appInfo)
        }
        if (BuildConfig.DEBUG) {
            if (!isMainProcess) {
                logger.debug("kill: userId: ${appInfo.userId}, packageName: ${packageName}, pid:${pid} >>> 子进程被杀")
            }
        }

        ProcessRecord.recycleProcessRecord(processRecord)
    }

    /* *************************************************************************
     *                                                                         *
     * app切换待处理队列                                                          *
     *                                                                         *
     **************************************************************************/
    // 活跃分组
    // 初始容量为4.一般前台app数量不会到达这个数吧?
    /*private val activeAppGroup: MutableSet<AppInfo> = Collections.newSetFromMap(
        ConcurrentHashMap(4)
    )

    // 后台分组
    private val idleAppGroup: MutableSet<AppInfo> = Collections.newSetFromMap(ConcurrentHashMap())*/

    private val doNothing: (AppInfo) -> Unit = {}

    private val putIntoActiveAction: (AppInfo) -> Unit = { appInfo: AppInfo ->
        putIntoActiveAppGroup(appInfo)
    }
    private val putIntoIdleAction: (AppInfo) -> Unit = { appInfo: AppInfo ->
        putIntoIdleAppGroup(appInfo)
    }

    private val activityEventChangeExecutor: ExecutorService = ExecutorUtils.newFixedThreadPool(
        coreSize = ConcurrentUtils.commonTaskThreadCount,
        factoryName = "activityEventChangePool"
    )

    fun addActivityEventChangeAction(block: Runnable) {
        activityEventChangeExecutor.execute(block)
    }

    /**
     * 以异步的方式处理Activity改变事件
     *
     * @param event         当前事件码
     * @param activityRecord [ClassConstants.ActivityRecord]
     */
    fun handleActivityEventChange(event: Int, @OriginalObject activityRecord: Any) {
        addActivityEventChangeAction {
            val userId: Int = ActivityRecord.getUserId(activityRecord)
            val packageName: String = ActivityRecord.getPackageName(activityRecord)
            handleActivityEventChange(event, userId, packageName, activityRecord)
        }
    }

    fun handleActivityEventChange(
        event: Int,
        userId: Int,
        packageName: String,
        activityRecord: Any?,
    ) {
        val appKey = getAppKey(userId, packageName)
        getFindAppResult(
            key = appKey,
            userId = userId,
            packageName = packageName
        ).applicationInfo.ifNull {
            return
        }
        val appInfo: AppInfo = getRunningAppInfo(appKey) ?: return

        appInfo.writeLock {
            handleActivityEventChangeLocked(event, activityRecord, appInfo)
        }
    }

    /**
     * 处理Activity改变事件<br></br>
     * 本方法处理的共有6大种情况, 本质就是从中找出切换app的方法:
     * <pre>
     * a为第一个打开的Activity, b为第二个。
     * 1. 初次打开: event(1[a])
     * 2. 从后台到前台: event(23[a]) -> event(1[a])
     * 3. 同一app内部:
     * 1) Activity(a) -> Activity(b):  event(2[a]) -> event(1[b]) -> event(23[a])
     * 1) Activity(b) -> Activity(a):  event(2[b]) -> event(1[a]) -> event(24[b])
     * 4.   ① 退至后台/息屏?: event(2[a]) -> event(23[a])
     * ② 点击通知进入另一个app: event(2) -> event(23)。实验情况见: /app/docs/部分场景说明/App切换事件
     * 在实验下, 可能会出现lastComponentName != curComponentName。
     * 因此, 加入ActivityManagerServiceHookKt.ACTIVITY_PAUSED,
     * 当lastComponentName != curComponentName时, 不更新当前数据
     * 5. 其他app小窗到全屏, 当前app(失去界面的app): event(1[a]) -> event(23[a])
     * 6. App关闭?: event(x[a]) -> event(24[a])
    </pre> *
     *
     * @param event         事件码
     * @param activityRecord 当前[ClassConstants.ActivityRecord], 在event = [ActivitySwitchHook.ACTIVITY_STOPPED]时可能为空
     * @param appInfo       app
     */
    fun handleActivityEventChangeLocked(
        event: Int,
        @OriginalObject activityRecord: Any?,
        appInfo: AppInfo,
    ) {
        appInfo.increaseActivitySwitchEventHandlingCount()

        when (event) {
            ActivitySwitchHook.ACTIVITY_RESUMED -> {
                // 从后台到前台 || 第一次打开app
                val runnable = if (activityRecord == appInfo.activityRecord
                    && appInfo.appGroupEnum != AppGroupEnum.ACTIVE
                    || appInfo.activityRecord == null
                ) {
                    putIntoActiveAction
                } else {
                    doNothing
                }
                handleAppSwitch(
                    activityRecord = activityRecord,
                    appInfo = appInfo,
                    throwableAction = { throwable: Throwable ->
                        logger.error("ACTIVITY_RESUMED处理出错", throwable)
                    }
                ) {
                    appInfo.activityActive(activityRecord!!)
                    runnable(appInfo)
                }
            }

            ActivitySwitchHook.ACTIVITY_STOPPED -> {
                /*
                    23.10.18: appInfo.getAppGroupEnum() != AppGroupEnum.IDLE可以换成appInfo.getAppGroupEnum() == AppGroupEnum.ACTIVE,
                        但为了往后的兼容性, 暂时保持这样
                 */
                if (activityRecord == appInfo.activityRecord) {
                    if (appInfo.appGroupEnum !== AppGroupEnum.IDLE || !powerManagerService.isGloballyInteractiveInternal()) {
                        handleAppSwitch(
                            activityRecord = activityRecord,
                            appInfo = appInfo,
                            throwableAction = { throwable: Throwable? ->
                                logger.error("ACTIVITY_STOPPED处理出错", throwable)
                            }
                        ) {
                            putIntoIdleAppGroup(appInfo)
                        }
                    }
                }
            }

            ActivitySwitchHook.ACTIVITY_DESTROYED -> {
                appInfo.activityDie(activityRecord!!)

                appInfo.hasActivity().ifFalse {
                    putIntoIdleAppGroup(appInfo)
                }
            }

            else -> {}
        }

        appInfo.decreaseActivitySwitchEventHandlingCount()
    }

    private inline fun handleAppSwitch(
        @OriginalObject activityRecord: Any?,
        appInfo: AppInfo,
        noinline throwableAction: ((Throwable) -> Unit)?,
        action: () -> Unit,
    ) {
        runCatchThrowable(catchBlock = { throwable: Throwable ->
            throwableAction?.invoke(throwable)
        }) {
            updateAppSwitchState(activityRecord, appInfo)
            action()
        }
    }

    private fun updateAppSwitchState(@OriginalObject activityRecord: Any?, appInfo: AppInfo) {
        appInfo.activityRecord = activityRecord
    }

    private fun putIntoActiveAppGroup(appInfo: AppInfo) {
        // activeAppGroup.add(appInfo)
        appInfo.appGroupEnum = AppGroupEnum.ACTIVE

        // 处理当前app
        handleCurApp(appInfo)

        if (BuildConfig.DEBUG) {
            logger.debug("app(userId: ${appInfo.userId}, packageName: ${appInfo.packageName})被放入ActiveGroup")
        }
    }

    private fun handleCurApp(appInfo: AppInfo) {
        if (activeLaunchPackageName == appInfo.packageName) {
            if (BuildConfig.DEBUG) {
                logger.debug("前台app为默认桌面, 不进行处理")
            }
            return
        }

        // 启动前台工作
        processManager.appActive(appInfo)
    }

    private fun putIntoIdleAppGroup(appInfo: AppInfo) {
        // idleAppGroup.add(appInfo)
        appInfo.appGroupEnum = AppGroupEnum.IDLE

        // 处理上个app
        handleLastApp(appInfo)

        if (BuildConfig.DEBUG) {
            logger.debug("app(userId: ${appInfo.userId}, packageName: ${appInfo.packageName})被放入IdleGroup")
        }
    }

    /**
     * 处理上个app
     *
     * @param appInfo app信息
     */
    private fun handleLastApp(appInfo: AppInfo) {
        if (activeLaunchPackageName == appInfo.packageName) {
            if (BuildConfig.DEBUG) {
                logger.debug("当前操作的app为默认桌面, 不进行处理")
            }
            return
        }

        // 启动后台工作
        processManager.appIdle(appInfo)
    }

    /* *************************************************************************
     *                                                                         *
     * 默认桌面                                                                  *
     *                                                                         *
     **************************************************************************/
    private val defaultApplicationManager: DefaultApplicationManager = DefaultApplicationManager()
    val allDefaultPackageNames get() = defaultApplicationManager.getAllPkgNames()

    fun setDefaultPackageName(key: String, packageName: String) {
        defaultApplicationManager.setDefaultPackageName(key, packageName)
    }

    fun getDefaultPackageName(key: String): String? {
        return defaultApplicationManager.getDefaultPackageName(key)
    }

    @Deprecated("")
    @Volatile
    private var _activeLaunchPackageName: String? = null
    val activeLaunchPackageName get() = getDefaultPackageName(DefaultApplicationManager.DEFAULT_APP_HOME)

    @Deprecated("")
    fun setActiveLaunchPackageName(activeLaunchPackageName: String?) {
        val curLaunchPackageName = this._activeLaunchPackageName
        // 将原桌面的adj调节修改
        runningApps.values.stream()
            .filter { appInfo: AppInfo -> appInfo.packageName == curLaunchPackageName }
            .forEach { appInfo: AppInfo ->
                // 已删去 AppInfo.handleAdjDependOnAppOptimizePolicy
                // appInfo.adjHandleFunction = AppInfo.handleAdjDependOnAppOptimizePolicy
            }
        // 保存新桌面包名
        this._activeLaunchPackageName = activeLaunchPackageName
    }

    fun initActiveDefaultAppPackageName() {
        packageManagerService?.let { pms ->
            listOf( /*new DefaultApplicationPkgNameInitializer()
                            .setKey(DefaultApplicationManager.DEFAULT_APP_BROWSER)
                            .setTag("浏览器")
                            .setPkgNameGetter(pms::getDefaultBrowser),*/
                DefaultApplicationPkgNameInitializer().apply {
                    key = DefaultApplicationManager.DEFAULT_APP_HOME
                    tag = "桌面"
                    pkgNameGetter = pms::getDefaultHome
                },
                DefaultApplicationPkgNameInitializer().apply {
                    key = DefaultApplicationManager.DEFAULT_APP_ASSISTANT
                    tag = "智能助手"
                    pkgNameGetter = pms::getDefaultAssistant
                },
                DefaultApplicationPkgNameInitializer().apply {
                    key = DefaultApplicationManager.DEFAULT_APP_INPUT_METHOD
                    tag = "输入法"
                    pkgNameGetter = pms::getDefaultInputMethod
                },
                DefaultApplicationPkgNameInitializer().apply {
                    key = DefaultApplicationManager.DEFAULT_APP_DIALER
                    tag = "通讯录与拨号"
                    pkgNameGetter = pms::getDefaultDialer
                },
                DefaultApplicationPkgNameInitializer().apply {
                    key = DefaultApplicationManager.DEFAULT_APP_SMS
                    tag = "短信"
                    pkgNameGetter = pms::getDefaultSms
                }
            ).forEach { initializer: DefaultApplicationPkgNameInitializer ->
                defaultApplicationManager.initDefaultApplicationNode(
                    initializer.key
                ) { defaultApplicationNode ->
                    initDefaultApplicationNode(
                        defaultApplicationNode = defaultApplicationNode,
                        defaultPkgNameSupplier = initializer.pkgNameGetter,
                        key = initializer.key,
                        tag = initializer.tag,
                        listener = generateDefaultApplicationChangeListener(initializer.tag)
                    )
                }
            }
        }
    }

    private fun initDefaultApplicationNode(
        defaultApplicationNode: DefaultApplicationNode,
        defaultPkgNameSupplier: () -> String?,
        key: String,
        tag: String?,
        listener: PropertyChangeListener<String>,
    ) {
        val defaultPkgName = defaultPkgNameSupplier()
        defaultApplicationNode.value = defaultPkgName
        defaultApplicationNode.addListener(key, listener)
        defaultPkgName?.let {
            logger.info("默认" + tag + "为: " + defaultPkgName)
        } ?: logger.warn("获取默认" + tag + "失败")
    }

    private fun generateDefaultApplicationChangeListener(tag: String?): PropertyChangeListener<String> {
        return PropertyChangeListener<String> { oldValue, newValue ->
            runningApps.values.asSequence() // .filter(AppInfo::isImportSystemApp)
                .filter { appInfo: AppInfo -> appInfo.packageName == oldValue || appInfo.packageName == newValue }
                .forEach { appInfo: AppInfo ->
                    if (appInfo.packageName == oldValue) {
                        appInfo.setAdjHandleFunction()
                    } else {
                        // 设置当前app
                        appInfo.adjHandleFunction = AppInfo.handleAdjAlways
                    }
                }
            logger.info("更换的" + tag + "包名为: " + newValue)
        }
    }

    /**
     * 用于初始化默认应用的包名的事件节点
     */
    private class DefaultApplicationPkgNameInitializer {
        lateinit var key: String
        lateinit var tag: String
        lateinit var pkgNameGetter: () -> String?
    }

    /* *************************************************************************
     *                                                                         *
     * process_manager                                                         *
     *                                                                         *
     **************************************************************************/
    private lateinit var _processManager: ProcessManager
    val processManager get() = _processManager

    fun initProcessManager() {
        _processManager = ProcessManager(this)
    }

    /* *************************************************************************
     *                                                                         *
     * 电源管理                                                                  *
     *                                                                         *
     **************************************************************************/
    private var _powerManagerService: PowerManagerService? = null
    var powerManagerService: PowerManagerService
        get() = _powerManagerService!!
        set(value) {
            _powerManagerService = value
        }

    /* *************************************************************************
     *                                                                         *
     * PackageManagerService                                                   *
     *                                                                         *
     **************************************************************************/
    var packageManagerService: PackageManagerService? = null

    /* *************************************************************************
     *                                                                         *
     * 模块消息管理器                                                             *
     *                                                                         *
     **************************************************************************/
    val moduleMessageManager: ModuleMessageManager = ModuleMessageManager(this).apply {
        start()
    }

    companion object {
        private lateinit var runningInfo: RunningInfo

        @JvmStatic
        fun getInstance(): RunningInfo = runningInfo
    }
}