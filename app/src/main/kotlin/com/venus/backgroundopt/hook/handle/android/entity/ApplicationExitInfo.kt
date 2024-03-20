package com.venus.backgroundopt.hook.handle.android.entity

/**
 * @author XingC
 * @date 2024/3/19
 */
class ApplicationExitInfo(
    override val originalInstance: Any
) :IAndroidEntity {
    companion object {
        /**
         * Application process died due to unknown reason.
         */
        const val REASON_UNKNOWN = 0

        const val REASON_EXIT_SELF = 1

        const val REASON_SIGNALED = 2

        const val REASON_LOW_MEMORY = 3

        const val REASON_CRASH = 4

        const val REASON_CRASH_NATIVE = 5

        const val REASON_ANR = 6

        const val REASON_INITIALIZATION_FAILURE = 7

        const val REASON_PERMISSION_CHANGE = 8

        const val REASON_EXCESSIVE_RESOURCE_USAGE = 9

        const val REASON_USER_REQUESTED = 10

        const val REASON_USER_STOPPED = 11

        const val REASON_DEPENDENCY_DIED = 12

        const val REASON_OTHER = 13

        const val REASON_FREEZER = 14

        const val REASON_PACKAGE_STATE_CHANGE = 15

        const val REASON_PACKAGE_UPDATED = 16

        const val SUBREASON_UNKNOWN = 0

        const val SUBREASON_WAIT_FOR_DEBUGGER = 1

        const val SUBREASON_TOO_MANY_CACHED = 2

        const val SUBREASON_TOO_MANY_EMPTY = 3

        const val SUBREASON_TRIM_EMPTY = 4

        const val SUBREASON_LARGE_CACHED = 5

        const val SUBREASON_MEMORY_PRESSURE = 6

        const val SUBREASON_EXCESSIVE_CPU = 7

        const val SUBREASON_SYSTEM_UPDATE_DONE = 8

        const val SUBREASON_KILL_ALL_FG = 9

        const val SUBREASON_KILL_ALL_BG_EXCEPT = 10

        const val SUBREASON_KILL_UID = 11

        const val SUBREASON_KILL_PID = 12

        const val SUBREASON_INVALID_START = 13

        const val SUBREASON_INVALID_STATE = 14

        const val SUBREASON_IMPERCEPTIBLE = 15

        const val SUBREASON_REMOVE_LRU = 16

        const val SUBREASON_ISOLATED_NOT_NEEDED = 17

        const val SUBREASON_CACHED_IDLE_FORCED_APP_STANDBY = 18

        const val SUBREASON_FREEZER_BINDER_IOCTL = 19

        const val SUBREASON_FREEZER_BINDER_TRANSACTION = 20

        const val SUBREASON_FORCE_STOP = 21

        const val SUBREASON_REMOVE_TASK = 22

        const val SUBREASON_STOP_APP = 23

        const val SUBREASON_KILL_BACKGROUND = 24

        const val SUBREASON_PACKAGE_UPDATE = 25

        const val SUBREASON_UNDELIVERED_BROADCAST = 26

        const val SUBREASON_SDK_SANDBOX_DIED = 27

        const val SUBREASON_SDK_SANDBOX_NOT_NEEDED = 28
    }
}