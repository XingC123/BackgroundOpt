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

import android.content.Context;
import android.os.Handler;

import com.venus.backgroundopt.xposed.BuildConfig;
import com.venus.backgroundopt.xposed.core.RunningInfo;
import com.venus.backgroundopt.xposed.hook.base.HookPoint;
import com.venus.backgroundopt.xposed.hook.base.MethodHook;
import com.venus.backgroundopt.xposed.hook.base.action.AfterHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.HookAction;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.FieldConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class ActivityManagerConstantsHook extends MethodHook {
    public ActivityManagerConstantsHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.ActivityManagerConstants,
                        MethodConstants.setOverrideMaxCachedProcesses,
                        new HookAction[]{
                                (BeforeHookAction) this::handleSetOverrideMaxCachedProcesses
                        },
                        int.class
                ),
        };
    }

    @Override
    public HookPoint[] getConstructorHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.ActivityManagerConstants,
                        new HookAction[]{
                                (AfterHookAction) this::handleAfterConstructorActivityManagerConstants,
                        },
                        Context.class,
                        ClassConstants.ActivityManagerService,
                        Handler.class
                ),
        };
    }

    private Object handleAfterConstructorActivityManagerConstants(XC_MethodHook.MethodHookParam param) {
        /*
            1. mCustomizedMaxCachedProcesses已通过Resources.getInteger进行处理
            2. CUR_MAX_CACHED_PROCESSES无论是构造方法还是ActivityManagerConstants.updateMaxCachedProcesses()都取决于其他参数。
                而其它参数都已经被处理
            3. CUR_TRIM_CACHED_PROCESSES计算方式为:(Integer.min(CUR_MAX_CACHED_PROCESSES, MAX_CACHED_PROCESSES) - rawMaxEmptyProcesses) / 3
         */
//        setCUR_TRIM_CACHED_PROCESSES(param);
//        setCUR_MAX_CACHED_PROCESSES(param); // 保险起见
        setMOverrideMaxCachedProcesses(param);

        return null;
    }

    private void setMAX_CACHED_PROCESSES(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.MAX_CACHED_PROCESSES, Integer.MAX_VALUE);
    }

    /**
     * 设置: CUR_TRIM_CACHED_PROCESSES
     */
    private void setCUR_TRIM_CACHED_PROCESSES(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.CUR_TRIM_CACHED_PROCESSES, Integer.MAX_VALUE);
    }

    /**
     * 设置: CUR_MAX_CACHED_PROCESSES
     */
    private void setCUR_MAX_CACHED_PROCESSES(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.CUR_TRIM_CACHED_PROCESSES, Integer.MAX_VALUE);
    }

    /**
     * 设置: mOverrideMaxCachedProcesses
     */
    private void setMOverrideMaxCachedProcesses(XC_MethodHook.MethodHookParam param) {
        setValue(param, FieldConstants.mOverrideMaxCachedProcesses, Integer.MAX_VALUE);
    }

    private void setValue(XC_MethodHook.MethodHookParam param, String field, Object value) {
        try {
            XposedHelpers.setObjectField(param.thisObject, field, value);
            if (Objects.equals(XposedHelpers.getObjectField(param.thisObject, field), value)) {
                if (BuildConfig.DEBUG) {
                    getLogger().debug(field + "设置成功");
                }
            } else {
                getLogger().warn(field + "设置失败");
            }
        } catch (Throwable t) {
            getLogger().warn(field + "设置出现异常", t);
        }
    }

    private Object handleSetOverrideMaxCachedProcesses(XC_MethodHook.MethodHookParam param) {
        param.args[0] = Integer.MAX_VALUE;

        return null;
    }
}