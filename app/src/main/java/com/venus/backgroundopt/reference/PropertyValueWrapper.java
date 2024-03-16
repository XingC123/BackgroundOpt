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

package com.venus.backgroundopt.reference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性值包装器
 *
 * @author XingC
 * @date 2024/2/2
 */
public class PropertyValueWrapper<V> {
    private V value;

    private final Map<String, PropertyChangeListener<V>> valueChangeListener = new ConcurrentHashMap<>();

    public PropertyValueWrapper() {
    }

    public PropertyValueWrapper(V value) {
        this.value = value;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        valueChangeListener.values().forEach(vPropertyChangeListener -> vPropertyChangeListener.change(oldValue, value));
    }

    /**
     * 注册监听器
     *
     * @param key      监听器的key
     * @param listener 监听器{@link PropertyChangeListener}
     */
    public void addListener(@NonNull String key, @NonNull PropertyChangeListener<V> listener) {
        valueChangeListener.putIfAbsent(key, listener);
    }

    /**
     * 根据key返回监听器
     *
     * @param key 监听器所对应的key
     * @return 若key没有对应的监听器, 则返回null
     */
    @Nullable
    public PropertyChangeListener<V> removeListener(@NonNull String key) {
        return valueChangeListener.remove(key);
    }
}