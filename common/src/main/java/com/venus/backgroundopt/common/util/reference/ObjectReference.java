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
                    
package com.venus.backgroundopt.common.util.reference;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/2
 */
public class ObjectReference<T> {
    private T value;

    public ObjectReference() {
    }

    public ObjectReference(T initialValue) {
        this.value = initialValue;
    }

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return this.value;
    }

    @NonNull
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectReference<?> that = (ObjectReference<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}