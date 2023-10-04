package com.venus.backgroundopt.hook.handle.miui;

import com.venus.backgroundopt.hook.base.HookPoint;
import com.venus.backgroundopt.hook.base.MethodHook;
import com.venus.backgroundopt.hook.base.action.DoNotingHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.constants.ClassConstants;
import com.venus.backgroundopt.hook.constants.MethodConstants;

import de.robv.android.xposed.XC_MethodHook;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/6/1
 */
public class PowerStateMachineHook extends MethodHook {
    public PowerStateMachineHook(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public HookPoint[] getHookPoint() {
        return new HookPoint[]{
                new HookPoint(
                        ClassConstants.PowerStateMachine,
                        MethodConstants.clearAppWhenScreenOffTimeOut,
                        new HookAction[]{
                                new DoNotingHookAction()
                        }
                ),
                new HookPoint(
                        ClassConstants.PowerStateMachine,
                        MethodConstants.clearAppWhenScreenOffTimeOutInNight,
                        new HookAction[]{
                                new DoNotingHookAction()
                        }
                )
        };
    }

    private Object handleClearAppWhenScreenOffTimeOut(XC_MethodHook.MethodHookParam param) {
        return null;
    }

    private Object handleClearAppWhenScreenOffTimeOutInNight(XC_MethodHook.MethodHookParam param) {
        return null;
    }
}
