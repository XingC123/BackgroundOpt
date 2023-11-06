package com.venus.backgroundopt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识的对象未使用的原因
 *
 * @author XingC
 * @date 2023/11/5
 */
@Target({
        ElementType.FIELD,
        ElementType.CONSTRUCTOR,
        ElementType.TYPE,
        ElementType.METHOD
})
@Retention(RetentionPolicy.SOURCE)
public @interface UnusedReason {
    String value() default "";
}
