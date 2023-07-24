package com.venus.backgroundopt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 目标安卓版本代码
 *
 * @author XingC
 * @date 2023/7/24
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TargetAndroidVersionCode {
    String[] value();
}
