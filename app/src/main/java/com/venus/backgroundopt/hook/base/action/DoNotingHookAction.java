package com.venus.backgroundopt.hook.base.action;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.robv.android.xposed.XC_MethodHook;

/**
 * 无作为
 *
 * @author XingC
 * @date 2023/9/6
 */
public class DoNotingHookAction implements HookAction {
    @Nullable
    @Override
    public Object execute(@NonNull XC_MethodHook.MethodHookParam param) {
        return null;
    }
}
