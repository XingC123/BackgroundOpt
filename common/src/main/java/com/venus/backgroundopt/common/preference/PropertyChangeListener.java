/*
 * Copyright (C) 2023 BackgroundOpt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *3
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.venus.backgroundopt.common.preference;

/**
 * 属性值变化的监听器<br>
 * <p>
 * 实现此接口并注册到{@link PropertyValueWrapper}中以在配置变化时做相应操作
 *
 * @param <V> {@link PropertyValueWrapper}的值的类型
 */
@FunctionalInterface
public interface PropertyChangeListener<V> {
    void change(V oldValue, V newValue);
}