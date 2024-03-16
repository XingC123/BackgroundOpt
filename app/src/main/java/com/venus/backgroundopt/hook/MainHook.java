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
                    
 package com.venus.backgroundopt.hook;

import com.venus.backgroundopt.BuildConfig;
import com.venus.backgroundopt.hook.handler.AndroidHookHandler;
import com.venus.backgroundopt.hook.handler.PowerKeeperHookHandler;
import com.venus.backgroundopt.hook.handler.SelfHookHandler;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        switch (loadPackageParam.packageName) {
            case "android" -> new AndroidHookHandler(loadPackageParam);
            // miui
            case "com.miui.powerkeeper" -> new PowerKeeperHookHandler(loadPackageParam);
            // 自己
            case BuildConfig.APPLICATION_ID -> new SelfHookHandler(loadPackageParam);
        }
    }
}