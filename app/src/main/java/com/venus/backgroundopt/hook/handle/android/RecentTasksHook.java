package com.venus.backgroundopt.hook.handle.android;

import com.venus.backgroundopt.entity.RunningInfo;
import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class RecentTasksHook extends MethodHook {
    public RecentTasksHook(ClassLoader classLoader, RunningInfo hookInfo) {
        super(classLoader, hookInfo);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[] {
                new HookPoint(
                        ClassConstants.RecentTasks,
                        MethodConstants.isInVisibleRange,
                        new HookAction[]{
                                (BeforeHookAction) this::handleIsInVisibleRange
                        },
                        ClassConstants.Task,
                        int.class,
                        int.class,
                        boolean.class
                ),
        };
    }

    private Object handleIsInVisibleRange(XC_MethodHook.MethodHookParam param) {
        // 将可见窗口数(numVisibleTasks)设置为0
        param.args[2] = 0;

        return null;
    }
}
