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
                    
 package com.venus.backgroundopt.hook.handle.android;

import android.os.UserHandle;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.AfterHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.FieldConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import java.lang.reflect.Array;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
@Deprecated
public class PackageManagerServiceHook extends MethodHook {
    public PackageManagerServiceHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.PackageManagerService,
                        MethodConstants.isFirstBoot,
                        new HookAction[]{
                                (AfterHookAction) this::getActiveLaunchPackageName
                        }
                ),
                new HookPoint(
                        ClassConstants.PackageManagerService,
                        MethodConstants.deletePackageLIF,
                        new HookAction[]{(AfterHookAction) this::handleDeletePackageLIF},
                        String.class,   /* packageName */
                        UserHandle.class,    /* user */
                        boolean.class,  /* deleteCodeAndResources */
                        Array.newInstance(int.class, 0).getClass(), /* allUserHandles */
                        int.class,  /* flags */
                        ClassConstants.PackageRemovedInfo_A12,  /* outInfo */
                        boolean.class,  /* writeSettings */
                        ClassConstants.ParsedPackage    /* replacingPackage */
                ),
        };
    }

    private Object getActiveLaunchPackageName(XC_MethodHook.MethodHookParam param) {
        RunningInfo runningInfo = getRunningInfo();
        // 在ActivityManagerService加载完毕后再获取
        if (runningInfo.getActivityManagerService() == null) {
            return null;
        }

        // 若已获取默认桌面的包名, 则不进行任何操作
        if (runningInfo.getActiveLaunchPackageName() != null) {
            return null;
        }

        Object mPackageManagerService = param.thisObject;
        Object mInjector =
                XposedHelpers.getObjectField(mPackageManagerService, FieldConstants.mInjector);
        Object mDefaultAppProvider =
                XposedHelpers.callMethod(mInjector, MethodConstants.getDefaultAppProvider);

        String packageName = (String) XposedHelpers.callMethod(
                mDefaultAppProvider,
                MethodConstants.getDefaultHome,
                0);

        if (BuildConfig.DEBUG) {
            getLogger().debug("默认启动器为: " + packageName);
        }

        runningInfo.setActiveLaunchPackageName(packageName);

        return null;
    }

    private Object handleDeletePackageLIF(XC_MethodHook.MethodHookParam methodHookParam) {
        Object[] args = methodHookParam.args;
        String packageName = (String) args[0];
        int[] userIds = (int[]) args[3];
        RunningInfo runningInfo = getRunningInfo();

        for (int userId : userIds) {
            runningInfo.removeRecordedFindAppResult(userId, packageName);
        }

        return null;
    }
}