package com.venus.backgroundopt.ui.base

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.venus.backgroundopt.R

/**
 * 自动换行横向布局
 *
 * 原地址: [Android自定义控件 | 源码里有宝藏之自动换行控件](https://www.jianshu.com/p/ecd8c6936eac)
 *
 * XingC修改说明:
 * - 在原本的实现中, 如果第一个元素view.visibility == View.GONE, 则会导致行高和第二个元素左侧有空白边距。因此增加了可视性的判断
 * - 原类继承自ViewGroup, 会导致: java.lang.ClassCastException: android.view.ViewGroup$LayoutParams cannot be cast to android.view.ViewGroup$MarginLayoutParams。
 * 因此修改为继承自LinearLayout
 * - 构造方法修改。增加了获取自定义属性值的代码
 *
 * @date 2024/2/6
 */
class LineFeedLayout : LinearLayout {
    //'横向间距'
    private var horizontalGap: Int = 10

    //'纵向间距'
    private var verticalGap: Int = 10

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(
        context,
        attrs,
        R.style.LineFeedLayoutDefault
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        context!!.obtainStyledAttributes(
            attrs,
            R.styleable.LineFeedLayout
        ).use { typedArray ->
            horizontalGap =
                typedArray.getDimension(
                    R.styleable.LineFeedLayout_horizontalSpacing,
                    context.resources.getDimension(R.dimen.item_space)
                ).toInt()
            verticalGap =
                typedArray.getDimension(
                    R.styleable.LineFeedLayout_verticalSpacing,
                    context.resources.getDimension(R.dimen.item_space)
                ).toInt()

            if (horizontalGap < 0) {
                horizontalGap = 10
            }
            if (verticalGap < 0) {
                verticalGap = 10
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        //'获取容器控件高度模式'
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        //'获取容器控件的宽度（即布局文件中指定的宽度）'
        val width = MeasureSpec.getSize(widthMeasureSpec)
        //'定义容器控件的初始高度为0'
        var height = 0
        //'当容器控件的高度被指定为精确的数值'
        if (heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec)
        }
        //'手动计算容器控件高度'
        else {
            //'容器控件当前行剩下的空间'
            var remainWidth = width
            //'遍历所有子控件并用自动换行的方式累加其高度'
            (0 until childCount).map { getChildAt(it) }.forEach { child ->
                val lp = child.layoutParams as MarginLayoutParams
                //'当前行已满，在新的一行放置子控件'
                if (isNewLine(lp, child, remainWidth, horizontalGap)) {
                    remainWidth = width - child.measuredWidth
                    //'容器控件新增一行的高度'
                    height += (lp.topMargin + lp.bottomMargin + child.measuredHeight + verticalGap)
                } else {  //'当前行未满，在当前行右侧放置子控件'
                    //'消耗当前行剩余宽度'
                    remainWidth -= child.measuredWidth
                    // XingC: 当前元素可视性不是GONE的时候才更新值
                    if (child.visibility != View.GONE && height == 0) {
                        height =
                            (lp.topMargin + lp.bottomMargin + child.measuredHeight + verticalGap)
                    }
                }
                //将子控件的左右边距和间隙也考虑在内
                remainWidth -= (lp.leftMargin + lp.rightMargin + horizontalGap)
            }
        }
        //'控件测量的终点，即容器控件的宽高已确定'
        setMeasuredDimension(width, height)
    }

    //'判断是否需要新起一行'
    private fun isNewLine(
        lp: MarginLayoutParams,
        child: View,
        remainWidth: Int,
        horizontalGap: Int
    ): Boolean {
        //'子控件所占宽度'
        val childOccupation = lp.leftMargin + child.measuredWidth + lp.rightMargin
        //'当子控件加上横向间距 > 剩余行空间时则新起一行'
        //'特别地，每一行的最后一个子控件不需要考虑其右边的横向间距，所以附加了第二个条件，当不考虑间距时能让得下就不换行'
        return (childOccupation + horizontalGap > remainWidth) && (childOccupation > remainWidth)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        //'当前横坐标（相对于容器控件左边界的距离）'
        var left = 0
        //'当前纵坐标（相对于容器控件上边界的距离）'
        var top = 0
        //'上一行底部的纵坐标（相对于容器控件上边界的距离）'
        var lastBottom = 0
        //'遍历所有子控件以确定它们相对于容器控件的位置'
        (0 until childCount).map { getChildAt(it) }.forEach { child ->
            val lp = child.layoutParams as MarginLayoutParams
            //'新起一行'
            if (isNewLine(lp, child, r - l - left, horizontalGap)) {
                left = -lp.leftMargin
                //'更新当前纵坐标'
                top = lastBottom
                //'上一行底部纵坐标置0，表示需要重新被赋值'
                lastBottom = 0
            }
            //'子控件左边界'
            val childLeft = left + lp.leftMargin
            //'子控件上边界'
            val childTop = top + lp.topMargin
            //'确定子控件上下左右边界相对于父控件左上角的距离'
            child.layout(
                childLeft,
                childTop,
                childLeft + child.measuredWidth,
                childTop + child.measuredHeight
            )
            //'更新上一行底部纵坐标'
            if (lastBottom == 0) lastBottom = child.bottom + lp.bottomMargin + verticalGap
            // XingC: 当前元素可视性不是GONE的时候才更新值
            if (child.visibility != View.GONE) {
                left += child.measuredWidth + lp.leftMargin + lp.rightMargin + horizontalGap
            }
        }
    }
}