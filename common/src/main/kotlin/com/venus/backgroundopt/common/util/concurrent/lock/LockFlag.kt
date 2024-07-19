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
                    
package com.venus.backgroundopt.common.util.concurrent.lock

import java.util.concurrent.locks.Lock

/**
 * 普通锁标识
 *
 * @author XingC
 */
interface LockFlag {
    val lock: Lock
}

inline fun LockFlag.lock(block: () -> Unit) {
    lock.lock()
    try {
        block()
    } finally {
        lock.unlock()
    }
}