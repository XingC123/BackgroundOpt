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

package com.venus.backgroundopt.hook.handle.android.entity

/**
 * @author XingC
 * @date 2024/4/13
 */
class ActivityManagerInternal {
    companion object {
        /**
         * Oom Adj Reason: none - internal use only, do not use it.
         * @hide
         */
        const val OOM_ADJ_REASON_NONE = 0

        /**
         * Oom Adj Reason: activity changes.
         * @hide
         */
        const val OOM_ADJ_REASON_ACTIVITY = 1

        /**
         * Oom Adj Reason: finishing a broadcast receiver.
         * @hide
         */
        const val OOM_ADJ_REASON_FINISH_RECEIVER = 2

        /**
         * Oom Adj Reason: starting a broadcast receiver.
         * @hide
         */
        const val OOM_ADJ_REASON_START_RECEIVER = 3

        /**
         * Oom Adj Reason: binding to a service.
         * @hide
         */
        const val OOM_ADJ_REASON_BIND_SERVICE = 4

        /**
         * Oom Adj Reason: unbinding from a service.
         * @hide
         */
        const val OOM_ADJ_REASON_UNBIND_SERVICE = 5

        /**
         * Oom Adj Reason: starting a service.
         * @hide
         */
        const val OOM_ADJ_REASON_START_SERVICE = 6

        /**
         * Oom Adj Reason: connecting to a content provider.
         * @hide
         */
        const val OOM_ADJ_REASON_GET_PROVIDER = 7

        /**
         * Oom Adj Reason: disconnecting from a content provider.
         * @hide
         */
        const val OOM_ADJ_REASON_REMOVE_PROVIDER = 8

        /**
         * Oom Adj Reason: UI visibility changes.
         * @hide
         */
        const val OOM_ADJ_REASON_UI_VISIBILITY = 9

        /**
         * Oom Adj Reason: device power allowlist changes.
         * @hide
         */
        const val OOM_ADJ_REASON_ALLOWLIST = 10

        /**
         * Oom Adj Reason: starting a process.
         * @hide
         */
        const val OOM_ADJ_REASON_PROCESS_BEGIN = 11

        /**
         * Oom Adj Reason: ending a process.
         * @hide
         */
        const val OOM_ADJ_REASON_PROCESS_END = 12

        /**
         * Oom Adj Reason: short FGS timeout.
         * @hide
         */
        const val OOM_ADJ_REASON_SHORT_FGS_TIMEOUT = 13

        /**
         * Oom Adj Reason: system initialization.
         * @hide
         */
        const val OOM_ADJ_REASON_SYSTEM_INIT = 14

        /**
         * Oom Adj Reason: backup/restore.
         * @hide
         */
        const val OOM_ADJ_REASON_BACKUP = 15

        /**
         * Oom Adj Reason: instrumented by the SHELL.
         * @hide
         */
        const val OOM_ADJ_REASON_SHELL = 16

        /**
         * Oom Adj Reason: task stack is being removed.
         */
        const val OOM_ADJ_REASON_REMOVE_TASK = 17

        /**
         * Oom Adj Reason: uid idle.
         */
        const val OOM_ADJ_REASON_UID_IDLE = 18

        /**
         * Oom Adj Reason: stop service.
         */
        const val OOM_ADJ_REASON_STOP_SERVICE = 19

        /**
         * Oom Adj Reason: executing service.
         */
        const val OOM_ADJ_REASON_EXECUTING_SERVICE = 20

        /**
         * Oom Adj Reason: background restriction changes.
         */
        const val OOM_ADJ_REASON_RESTRICTION_CHANGE = 21

        /**
         * Oom Adj Reason: A package or its component is disabled.
         */
        const val OOM_ADJ_REASON_COMPONENT_DISABLED = 22
    }
}