package com.venus.backgroundopt.hook.base.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/27
 */
public interface HookAction {
    @Nullable
    Object execute(@NotNull MethodHookParam param);
}
