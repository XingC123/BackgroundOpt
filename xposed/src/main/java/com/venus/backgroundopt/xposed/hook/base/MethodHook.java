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

package com.venus.backgroundopt.xposed.hook.base;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.xposed.core.RunningInfo;
import com.venus.backgroundopt.xposed.hook.IneffectiveHookPoint;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public abstract class MethodHook extends AbstractHook {
    public MethodHook(@NonNull ClassLoader classLoader) {
        super(classLoader, null);
    }

    public MethodHook(@NonNull ClassLoader classLoader, @NonNull RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public void hook() {
        for (HookPoint hookPoint : getConstructorHookPoint()) {
            hookPoint.hook(classLoader, HookPoint.MethodType.Constructor);
        }

        for (HookPoint hookPoint : getHookPoint()) {
            if (hookPoint instanceof IneffectiveHookPoint) {
                getLogger().debug("因不满足条件, [" + hookPoint.getClassName() + "." + hookPoint.getMethodName() + "]不进行hook");
            } else {
                hookPoint.hook(classLoader, HookPoint.MethodType.Member);
            }
        }
    }

    private static final HookPoint[] defaultHookPoint = new HookPoint[0];

    public HookPoint[] getConstructorHookPoint() {
        return defaultHookPoint;
    }
}