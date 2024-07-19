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

package com.venus.backgroundopt.xposed.point.miui;

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
public class SleepModeControllerNewHook extends MethodHook {
    public SleepModeControllerNewHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.SleepModeControllerNew,
                        MethodConstants.clearApp,
                        new HookAction[]{
                                new DoNotingHookAction()
                        }
                )
        };
    }

    private Object handleClearApp(XC_MethodHook.MethodHookParam param) {
        return null;
    }
}