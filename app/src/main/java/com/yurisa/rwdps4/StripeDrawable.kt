package com.yurisa.rwdps4

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class StripedDrawable(private val stripeColor: Int, private val backgroundColor: Int) : Drawable() {
    private val paint = Paint()
    private val stripeWidth = 20f
    private val stripeGap = 80f
    private var offset = 0f

    fun getStripeWidth(): Float {
        return stripeWidth
    }

    fun getStripeGap(): Float {
        return stripeGap
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        paint.style = Paint.Style.FILL

        // 绘制背景色
        paint.color = backgroundColor
        canvas.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), paint)

        // 绘制斜条纹
        paint.color = stripeColor
        val path = Path()
        var x = bounds.left.toFloat() - stripeGap
        while (x < bounds.right + stripeWidth) {
            path.moveTo(x, bounds.top.toFloat())
            path.lineTo(x + stripeWidth*0.5f, bounds.top.toFloat())   //乘数可以控制粗细
            path.lineTo(x + stripeWidth*0.5f + stripeGap, bounds.bottom.toFloat())
            path.lineTo(x + stripeGap, bounds.bottom.toFloat())
            path.close()

            canvas.drawPath(path, paint)
            path.reset()
            x += stripeWidth + stripeGap
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun setOffset(newOffset: Float) {
        offset = newOffset
        invalidateSelf()  // 重绘
    }
}
