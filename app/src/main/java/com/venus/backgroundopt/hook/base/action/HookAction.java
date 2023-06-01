package com.venus.backgroundopt.hook.base.action;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/27
 */
public interface HookAction {
    Object execute(MethodHookParam param);
}
