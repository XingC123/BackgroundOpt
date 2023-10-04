package com.venus.backgroundopt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对类/方法/属性的使用说明
 *
 * @author XingC
 * @date 2023/9/30
 */
@Target({
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.TYPE,
        ElementType.METHOD,
})
@Retention(RetentionPolicy.SOURCE)
public @interface UsageComment {
    String value() default "";
}
