package com.venus.backgroundopt.hook.handle.android.entity

import com.venus.backgroundopt.annotation.AndroidObject

/**
 * 所有对安卓源码中实体的包装类都需要继承此接口
 *
 * @author XingC
 * @date 2024/2/29
 */
interface IAndroidEntity {
    /**
     * 被包装的原生对象
     */
    @AndroidObject
    val originalInstance: Any
}