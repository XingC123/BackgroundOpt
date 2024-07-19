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
                    
package com.venus.backgroundopt.xposed.point.android;

import static com.venus.backgroundopt.xposed.entity.android.android.provider.DeviceConfig.NAMESPACE_ACTIVITY_MANAGER;
import static com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ActivityManagerConstants.KEY_MAX_CACHED_PROCESSES;

import com.venus.backgroundopt.xposed.core.RunningInfo;
import com.venus.backgroundopt.xposed.hook.base.HookPoint;
import com.venus.backgroundopt.xposed.hook.base.MethodHook;
import com.venus.backgroundopt.xposed.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.HookAction;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class DeviceConfigHook extends MethodHook {
    public DeviceConfigHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.DeviceConfig,
                        MethodConstants.getProperty,
                        new HookAction[]{
                                (BeforeHookAction) this::setKEY_MAX_CACHED_PROCESSES
                        },
                        String.class, String.class
                )
        };
    }

    private Object setKEY_MAX_CACHED_PROCESSES(XC_MethodHook.MethodHookParam param) {
        Object namespace = param.args[0];
        Object name = param.args[1];
        if (Objects.equals(NAMESPACE_ACTIVITY_MANAGER, namespace)) {
            // Android 14 可能会炸(安卓虚拟机开了, 但是别的可能炸掉), 因此应取消
            if (Objects.equals(KEY_MAX_CACHED_PROCESSES, name)) {
                param.setResult(String.valueOf(Integer.MAX_VALUE));
            }
        }

        return null;
    }
}