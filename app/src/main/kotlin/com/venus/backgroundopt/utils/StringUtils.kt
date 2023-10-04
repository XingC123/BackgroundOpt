package com.venus.backgroundopt.utils

/**
 * @author XingC
 * @date 2023/9/29
 */

object StringUtils {
    /**
     * 判断给定字符串是否是空
     *
     * @param str
     * @return true if (str == null or str.isBlank())
     */
    fun isEmpty(str: String?): Boolean = str?.isBlank() ?: true
}
