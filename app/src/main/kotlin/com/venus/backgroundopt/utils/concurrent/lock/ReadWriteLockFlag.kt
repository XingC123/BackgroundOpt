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
                    
 package com.venus.backgroundopt.utils.concurrent.lock

import java.util.concurrent.locks.ReadWriteLock

/**
 * 读写锁标识
 *
 * @author XingC
 */
interface ReadWriteLockFlag {
    fun getReadWriteLock(): ReadWriteLock
}

inline fun ReadWriteLockFlag.readLock(block: () -> Unit) {
    val readLock = getReadWriteLock().readLock()
    readLock.lock()
    try {
        block()
    } finally {
        readLock.unlock()
    }
}

inline fun ReadWriteLockFlag.writeLock(block: () -> Unit) {
    val writeLock = getReadWriteLock().writeLock()
    writeLock.lock()
    try {
        block()
    } finally {
        writeLock.unlock()
    }
}