package com.venus.backgroundopt.hook.base;

import com.venus.backgroundopt.entity.RunningInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

/**
 * @author XingC
 * @version 1.0
 * @date 2023/2/8
 */
public abstract class MethodHook extends AbstractHook {
    public MethodHook(ClassLoader classLoader) {
        this(classLoader, null);
    }
    public MethodHook(ClassLoader classLoader, RunningInfo runningInfo) {
        super(classLoader, runningInfo);
    }

    public abstract Object[] getTargetParam();

    @Override
    public void hook() {
        try {
            // 处理hook方法的参数类型
            List<Object> params = new ArrayList<>(Arrays.asList(getTargetParam()));
            params.add(getActionMethod());  // 将动作方法添加到hook方法传参数类型中

            XposedHelpers.findAndHookMethod(getTargetClass(), classLoader,
                    getTargetMethod(), params.toArray());
            getLogger().debug("[" + getTargetClass() + "." + getTargetMethod() + "]hook成功");
        } catch (Exception e) {
            getLogger().debug("[" + getTargetClass() + "." + getTargetMethod() + "]hook失败", e);
        }
    }
}
