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

import com.venus.backgroundopt.common.util.OsUtils;
import com.venus.backgroundopt.xposed.core.RunningInfo;
import com.venus.backgroundopt.xposed.hook.base.HookPoint;
import com.venus.backgroundopt.xposed.hook.base.MethodHook;
import com.venus.backgroundopt.xposed.hook.base.action.DoNotingHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.HookAction;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class PhantomProcessListHook extends MethodHook {
    public PhantomProcessListHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[] {
                new HookPoint(
                        ClassConstants.PhantomProcessList,
                        MethodConstants.trimPhantomProcessesIfNecessary,
                        new HookAction[]{
                                new DoNotingHookAction()
                        }
                ).setEnableHook(OsUtils.isSOrHigher),
        };
    }

    private Object handleTrimPhantomProcessesIfNecessary(XC_MethodHook.MethodHookParam param) {
        return null;
    }
}