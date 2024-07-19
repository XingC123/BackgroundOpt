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
                    
package com.venus.backgroundopt.xposed.hook.base;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.common.util.ThrowableUtilsKt;
import com.venus.backgroundopt.common.util.log.ILogger;
import com.venus.backgroundopt.xposed.hook.base.action.AfterHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.DoNotingHookAction;
import com.venus.backgroundopt.xposed.hook.base.action.HookAction;
import com.venus.backgroundopt.xposed.hook.base.action.ReplacementHookAction;
import com.venus.backgroundopt.common.environment.CommonProperties;
import com.venus.backgroundopt.xposed.util.XposedUtilsKt;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/27
 */
public class HookPoint implements ILogger {
    public static final Set<String> dontPrintLogHookList = new HashSet<>() {
        {
            add(CommonProperties.class.getTypeName() + ".isModuleActive");
        }
    };

    private final String className;
    private final String methodName;

    private String tag;

    private boolean hookAllMatchedMethod = false;

    private final HookAction[] hookActions;

    private final Object[] actionArgs;

    private boolean enableHook = true;

    /**
     * hook构造器
     *
     * @param className   类全限定名
     * @param hookActions hook后的具体执行
     * @param actionArgs  参数
     */
    public HookPoint(@NotNull String className, HookAction[] hookActions, Object... actionArgs) {
        this(className, className, hookActions, actionArgs);
    }

    /**
     * hook普通方法
     *
     * @param className   类全限定名
     * @param methodName  要hook的方法
     * @param hookActions hook后的具体执行
     * @param actionArgs  参数
     */
    public HookPoint(@NotNull String className, String methodName, HookAction[] hookActions, Object... actionArgs) {
        this.className = className;
        this.methodName = methodName;
        this.hookActions = hookActions;
        this.actionArgs = actionArgs;
    }

    public enum MethodType {
        Member,
        Constructor
    }

    public void hook(ClassLoader classLoader, @NonNull MethodType methodType) {
        if (!enableHook) {
            return;
        }

        for (HookAction hookAction : this.hookActions) {
            hookImpl(classLoader, methodType, hookAction);
        }
    }

    private void hookImpl(ClassLoader classLoader, @NonNull MethodType methodType, HookAction hookAction) {
        if (hookAction == null) {
            return;
        }

        String hookClassName = this.getClassName();
        String hookMethodName = this.getMethodName();
        try {
            if (methodType == MethodType.Constructor) {
                XposedHelpers.findAndHookConstructor(hookClassName, classLoader, this.getFinalArgs(hookAction));
            } else {
                if (hookAllMatchedMethod) {
                    Set<XC_MethodHook.Unhook> unhookSet = XposedBridge.hookAllMethods(
                            XposedHelpers.findClass(hookClassName, classLoader),
                            hookMethodName,
                            this.getFinalXCMethodHook(hookAction)
                    );
                    if (tag != null) {
                        IHook.addMethodHookPoint(tag, unhookSet);
                    }
                } else {
                    XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(
                            hookClassName, classLoader, hookMethodName,
                            this.getFinalArgs(hookAction)
                    );
                    if (tag != null) {
                        IHook.addMethodHookPoint(tag, unhook);
                    }
                }
            }

            /*if (!dontPrintLogHookList.contains(hookClassName + "." + hookMethodName)) {
                getLogger().info("[" + hookClassName + "." + hookMethodName + "]hook成功");
            }*/
        } catch (Throwable t) {
            if (!dontPrintLogHookList.contains(hookClassName + "." + hookMethodName)) {
                getLogger().error("[" + hookClassName + "." + hookMethodName + "]hook失败", t);
            }
        }
    }

    private Object[] getFinalArgs(HookAction hookAction) {
        // 处理hook方法的参数类型
        List<Object> params = new ArrayList<>(Arrays.asList(this.getActionArgs()));
        // 将动作方法添加到hook方法传参数类型中
        params.add(getFinalXCMethodHook(hookAction));

        return params.toArray();
    }

    private XC_MethodHook getFinalXCMethodHook(HookAction hookAction) {
        XC_MethodHook xc_methodHook;
        if (hookAction instanceof BeforeHookAction) {
            xc_methodHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    ThrowableUtilsKt.runCatchThrowable(null, throwable -> {
                        XposedUtilsKt.printAfterAppearException(className, methodName, throwable);
                        return null;
                    }, () -> {
                        hookAction.execute(param);
                        return null;
                    });
                }
            };
        } else if (hookAction instanceof AfterHookAction) {
            xc_methodHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    ThrowableUtilsKt.runCatchThrowable(null, throwable -> {
                        XposedUtilsKt.printAfterAppearException(className, methodName, throwable);
                        return null;
                    }, () -> {
                        hookAction.execute(param);
                        return null;
                    });
                }
            };
        } else if (hookAction instanceof ReplacementHookAction) {
            xc_methodHook = new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    return ThrowableUtilsKt.runCatchThrowable(null, throwable -> {
                        XposedUtilsKt.printAfterAppearException(className, methodName, throwable);
                        return null;
                    }, () -> {
                        hookAction.execute(methodHookParam);
                        return null;
                    });
                }
            };
        } else if (hookAction instanceof DoNotingHookAction) {
            xc_methodHook = XC_MethodReplacement.DO_NOTHING;
        } else {
            throw new IllegalArgumentException("hookAction 类型错误");
        }

        return xc_methodHook;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public HookAction[] getHookActions() {
        return hookActions;
    }

    public Object[] getActionArgs() {
        return actionArgs;
    }

    public boolean isHookAllMatchedMethod() {
        return hookAllMatchedMethod;
    }

    public HookPoint setHookAllMatchedMethod(boolean hookAllMatchedMethod) {
        this.hookAllMatchedMethod = hookAllMatchedMethod;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public HookPoint setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public boolean isEnableHook() {
        return enableHook;
    }

    public HookPoint setEnableHook(boolean enableHook) {
        this.enableHook = enableHook;
        return this;
    }
}