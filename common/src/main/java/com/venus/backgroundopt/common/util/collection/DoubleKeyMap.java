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
                    
package com.venus.backgroundopt.common.util.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 双key map
 * 两个key对应一个value
 * 删除操作对性能影响较大, 因此此map最好只有增/查操作
 *
 * @author XingC
 * @date 2023/8/22
 */
public class DoubleKeyMap<K1, K2, V> {
    private final Map<K1, V> map1 = new HashMap<>();
    private final Map<K2, V> map2 = new HashMap<>();

    private final Class<K1> key1Clazz;
    private final Class<K2> key2Clazz;

    public DoubleKeyMap(Class<K1> key1Clazz, Class<K2> key2Clazz) {
        this.key1Clazz = key1Clazz;
        this.key2Clazz = key2Clazz;
    }

    /**
     * 添加元素
     *
     * @param k1 key
     * @param v  value
     * @return 添加成功或失败
     * @throws IllegalArgumentException 若给定k不满足当前Map的任一key类型, 则抛出此异常
     */
    @SuppressWarnings("unchecked")
    public V put(Object k1, V v) {
        Class<?> aClass = k1.getClass();
        if (aClass == key1Clazz) {
            return map1.put((K1) k1, v);
        } else if (aClass == key2Clazz) {
            return map2.put((K2) k1, v);
        }

        throw new IllegalArgumentException("key类型不正确");
    }

    /**
     * 获取k对应的value
     *
     * @param k 其中一个key
     * @return 若不存在对应value记录, 则返回null。反之, 返回对应value
     */
    public V get(Object k) {
        Class<?> aClass = k.getClass();
        if (aClass == key1Clazz) {
            return map1.get(k);
        } else if (aClass == key2Clazz) {
            return map2.get(k);
        }

        throw new IllegalArgumentException("key类型不正确");
    }

    /**
     * 移除指定k对应的元素
     * 对性能影响较大, 不推荐使用
     *
     * @param k 其中一个key
     * @return 若不存在对应value记录, 则返回null。反之, 返回对应value
     */
    public V remove(Object k) {
        boolean f1 = map1.containsKey(k);
        boolean f2 = map2.containsKey(k);
        if (!(f1 || f2)) {
            return null;
        }

        V remove1 = map1.remove(k);
        V remove2 = map2.remove(k);

        if (!f1) {
            map1.entrySet().removeIf(entry -> Objects.equals(remove2, entry.getValue()));
            return remove2;
        } else {
            map2.entrySet().removeIf(entry -> Objects.equals(remove1, entry.getValue()));
            return remove1;
        }
    }
}