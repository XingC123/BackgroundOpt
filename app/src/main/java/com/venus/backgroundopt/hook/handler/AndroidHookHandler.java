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
                    
package com.venus.backgroundopt.hook.handler;

import android.os.Build;

import com.venus.backgroundopt.core.RunningInfo;
import com.venus.backgroundopt.environment.SystemProperties;
import com.venus.backgroundopt.hook.base.PackageHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerConstantsHookNew;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookKt;
import com.venus.backgroundopt.hook.handle.android.ActivityManagerServiceHookNew;
import com.venus.backgroundopt.hook.handle.android.ActivityTaskSupervisorHook;
import com.venus.backgroundopt.hook.handle.android.AppProfilerHook;
import com.venus.backgroundopt.hook.handle.android.CachedAppOptimizerHook;
import com.venus.backgroundopt.hook.handle.android.DeletePackageHelperHook;
import com.venus.backgroundopt.hook.handle.android.DeviceConfigHookNew;
import com.venus.backgroundopt.hook.handle.android.LowMemDetectorHook;
import com.venus.backgroundopt.hook.handle.android.OomAdjusterHookNew;
import com.venus.backgroundopt.hook.handle.android.PackageManagerServiceHookKt;
import com.venus.backgroundopt.hook.handle.android.PackageManagerServiceHookNew;
import com.venus.backgroundopt.hook.handle.android.PhantomProcessListHook;
import com.venus.backgroundopt.hook.handle.android.PowerManagerServiceHook;
import com.venus.backgroundopt.hook.handle.android.ProcessListHookKt;
import com.venus.backgroundopt.hook.handle.android.ProcessListHookNew;
import com.venus.backgroundopt.hook.handle.android.ProcessRecordHook;
import com.venus.backgroundopt.hook.handle.android.RecentTasksHook;
import com.venus.backgroundopt.hook.handle.android.RoleControllerManagerHook;
import com.venus.backgroundopt.hook.handle.android.ServiceManagerHook;
import com.venus.backgroundopt.hook.handle.android.SystemPropertiesHook;

import java.util.HashMap;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/17
 */
//@HookPackageName("android")
public class AndroidHookHandler extends PackageHook {
    public AndroidHookHandler(XC_LoadPackage.LoadPackageParam packageParam) {
        super(packageParam);
    }

    @Override
    public void hook(XC_LoadPackage.LoadPackageParam packageParam) {
        ClassLoader classLoader = packageParam.classLoader;
        RunningInfo runningInfo = new RunningInfo(classLoader);

        initSystemProp();

        // 资源Hook
//        new ResourcesHook(classLoader, runningInfo);

        // hook获取
//        new DeviceConfigHook(classLoader, runningInfo);
        new DeviceConfigHookNew(classLoader, runningInfo);

        // 抓取AMS, 前后台切换
        new ActivityManagerServiceHook(classLoader, runningInfo);
        new ActivityManagerServiceHookKt(classLoader, runningInfo);

        // 默认桌面
        new PackageManagerServiceHookKt(classLoader, runningInfo);

        // 杀后台hook
//        new ProcessHook(classLoader, runningInfo);
//        new ProcessHookKt(classLoader, runningInfo);

        // oom_adj更新hook
        // 2024.3.2: 禁用以通过ProcessList.setOomAdj知晓系统给予当前进程的oom_score_adj
        /*if (CommonProperties.INSTANCE.getOomWorkModePref().getOomMode() == OomWorkModePref.MODE_STRICT) {
            new ProcessStateRecordHook(classLoader, runningInfo);
        }*/
        new ProcessListHookKt(classLoader, runningInfo);

        // 安卓虚进程处理hook
        new PhantomProcessListHook(classLoader, runningInfo);

//        new ActivityManagerConstantsHook(classLoader, runningInfo);

        // 最近任务可见性hook
        new RecentTasksHook(classLoader, runningInfo);

        // 软件卸载
        // 安卓12在PackageManagerServiceHook完成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {    // 安卓13
            new DeletePackageHelperHook(classLoader, runningInfo);
        }

        new SystemPropertiesHook(classLoader, runningInfo);

        new RoleControllerManagerHook(classLoader, runningInfo);

        new LowMemDetectorHook(classLoader, runningInfo);

        new AppProfilerHook(classLoader, runningInfo);

        new ProcessListHookNew(classLoader, runningInfo);

        new OomAdjusterHookNew(classLoader, runningInfo);

        new ActivityManagerServiceHookNew(classLoader, runningInfo);

        new CachedAppOptimizerHook(classLoader, runningInfo);

        new PackageManagerServiceHookNew(classLoader, runningInfo);

        new ServiceManagerHook(classLoader, runningInfo);

        new ActivityTaskSupervisorHook(classLoader, runningInfo);

        new PowerManagerServiceHook(classLoader, runningInfo);

        new ActivityManagerConstantsHookNew(classLoader, runningInfo);

        new ProcessRecordHook(classLoader, runningInfo);
    }

    private void initSystemProp() {
        HashMap<String, String> map = new HashMap<>() {
            {
                // 米杀后台SystemProperties
                put("persist.sys.spc.enabled", "false");
            }
        };

        map.forEach((k, v) -> {
            try {
                SystemProperties.set(k, v);
            } catch (Throwable throwable) {
                getLogger().warn("[" + k + "]设置失败", throwable);
            }
        });
    }
}