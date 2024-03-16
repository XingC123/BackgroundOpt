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
                    
 package com.venus.backgroundopt.hook.handle.android.entity;

import com.venus.backgroundopt.annotation.AndroidObject;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;

import de.robv.android.xposed.XposedHelpers;

/**
 * 封装了{@link ClassConstants#OomAdjuster}
 *
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class OomAdjuster {
    @AndroidObject(classPath = ClassConstants.OomAdjuster)
    private final Object oomAdjuster;

    @AndroidObject
    public Object getOomAdjuster() {
        return oomAdjuster;
    }

    private final CachedAppOptimizer cachedAppOptimizer;

    public CachedAppOptimizer getCachedAppOptimizer() {
        return cachedAppOptimizer;
    }

    public OomAdjuster(@AndroidObject Object oomAdjuster, ClassLoader classLoader) {
        this.oomAdjuster = oomAdjuster;

        cachedAppOptimizer = new CachedAppOptimizer(
                XposedHelpers.getObjectField(oomAdjuster, FieldConstants.mCachedAppOptimizer),
                classLoader
        );
    }
}