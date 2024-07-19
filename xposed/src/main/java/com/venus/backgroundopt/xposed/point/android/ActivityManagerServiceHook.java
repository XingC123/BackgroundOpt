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
import com.venus.backgroundopt.xposed.BuildConfig;
import com.venus.backgroundopt.xposed.core.RunningInfo;
import com.venus.backgroundopt.xposed.entity.android.android.os.SystemProperties;
import com.venus.backgroundopt.xposed.entity.android.com.android.server.am.ActivityManagerService;
import com.venus.backgroundopt.xposed.hook.base.HookPoint;
import com.venus.backgroundopt.xposed.hook.base.MethodHook;
import com.venus.backgroundopt.xposed.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.DoNotingHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.HookAction;
import com.venus.backgroundopt.xposed.hook.base.action.ReplacementHookAction;
import com.venus.backgroundopt.xposed.hook.constants.ClassConstants;
import com.venus.backgroundopt.xposed.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class ActivityManagerServiceHook extends MethodHook {
    public ActivityManagerServiceHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.ActivityManagerService,
                        MethodConstants.setSystemProcess,
                        new HookAction[]{
                                (BeforeHookAction) this::getAMSObj
                        }
                ),
                new HookPoint(
                        ClassConstants.ActivityManagerService,
                        MethodConstants.checkExcessivePowerUsageLPr,
                        new HookAction[]{
                                (ReplacementHookAction) this::handleCheckExcessivePowerUsageLPr
                        },
                        long.class,
                        boolean.class,
                        long.class,
                        String.class,
                        String.class,
                        int.class,
                        ClassConstants.ProcessRecord
                ).setEnableHook(OsUtils.isSOrHigher),
                new HookPoint(
                        ClassConstants.ActivityManagerService,
                        MethodConstants.checkExcessivePowerUsage,
                        new HookAction[]{
                                new DoNotingHookAction()
                        }
                ).setEnableHook(OsUtils.isSOrHigher),
        };
    }

    /**
     * 获取AMS对象
     */
    private Object getAMSObj(XC_MethodHook.MethodHookParam param) {
        RunningInfo runningInfo = getRunningInfo();
        ActivityManagerService ams = new ActivityManagerService(param.thisObject, getClassLoader(), runningInfo);

        runningInfo.setActivityManagerService(ams);
        runningInfo.initProcessManager();

        if (BuildConfig.DEBUG) {
            getLogger().debug("拿到AMS");
        }

        // 设置persist.sys.spc.enabled禁用小米的杀后台
        try {
            SystemProperties.set("persist.sys.spc.enabled", "false");
        } catch (Throwable throwable) {
            getLogger().warn("米杀后台SystemProperties设置失败");
        }

        return null;
    }

    /* *************************************************************************
     *                                                                         *
     * 杀后台设置                                                                *
     *                                                                         *
     **************************************************************************/
    private Object handleCheckExcessivePowerUsageLPr(XC_MethodHook.MethodHookParam param) {
        return false;
    }

    private Object handleCheckExcessivePowerUsage(XC_MethodHook.MethodHookParam param) {
        return null;
    }
}