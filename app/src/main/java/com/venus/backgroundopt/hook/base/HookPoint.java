package com.venus.backgroundopt.hook.base;

import androidx.annotation.NonNull;

import com.venus.backgroundopt.hook.base.action.AfterHookAction;
import com.venus.backgroundopt.hook.base.action.BeforeHookAction;
import com.venus.backgroundopt.hook.base.action.HookAction;
import com.venus.backgroundopt.hook.base.action.ReplacementHookAction;
import com.venus.backgroundopt.utils.log.ILogger;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/4/27
 */
public class HookPoint implements ILogger {
    private final String className;
    private final String methodName;

    private final HookAction[] hookActions;

    private final Object[] actionArgs;

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

    enum MethodType {
        Member,
        Constructor
    }

    public void hook(ClassLoader classLoader, @NonNull MethodType methodType) {
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
                XposedHelpers.findAndHookMethod(
                        hookClassName, classLoader, hookMethodName,
                        this.getFinalArgs(hookAction)
                );
            }

            getLogger().info("[" + hookClassName + "." + hookMethodName + "]hook成功");
        } catch (Throwable t) {
            getLogger().error("[" + hookClassName + "." + hookMethodName + "]hook失败", t);
        }
    }

    private Object[] getFinalArgs(HookAction hookAction) {
        // 处理hook方法的参数类型
        List<Object> params = new ArrayList<>(Arrays.asList(this.getActionArgs()));
        // 将动作方法添加到hook方法传参数类型中
        XC_MethodHook xc_methodHook;
        if (hookAction instanceof BeforeHookAction) {
            xc_methodHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);

                    hookAction.execute(param);
                }
            };
        } else if (hookAction instanceof AfterHookAction) {
            xc_methodHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    hookAction.execute(param);
                }
            };
        } else if (hookAction instanceof ReplacementHookAction) {
            xc_methodHook = new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    return hookAction.execute(methodHookParam);
                }
            };
        } else {
            throw new IllegalArgumentException("hookAction 类型错误");
        }

        params.add(xc_methodHook);

        return params.toArray();
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
}
