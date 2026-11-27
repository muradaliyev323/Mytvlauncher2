package com.mytv.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView

// Все размеры задаются как в макете (экран 1280 px шириной) и масштабируются под реальный экран.
class Ui(private val ctx: Context) {
    val scale: Float = ctx.resources.displayMetrics.widthPixels / 1280f

    fun px(v: Int): Int = (v * scale).toInt()

    fun text(t: String, size: Int, bold: Boolean = false): TextView {
        val tv = TextView(ctx)
        tv.text = t
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * scale)
        tv.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        tv.includeFontPadding = false
        return tv
    }

    private fun shape(fill: Int, stroke: Int, strokeW: Int, radius: Int, oval: Boolean): GradientDrawable {
        val g = GradientDrawable()
        g.shape = if (oval) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
        g.setColor(fill)
        g.setStroke(if (strokeW < 1) 1 else strokeW, stroke)
        g.cornerRadius = radius.toFloat()
        return g
    }

    // Фон плитки: обычный и «в фокусе» (белая рамка)
    fun focusBg(radius: Int, oval: Boolean = false): StateListDrawable {
        val sl = StateListDrawable()
        val focused = shape(Color.argb(60, 255, 255, 255), Color.WHITE, px(3), radius, oval)
        sl.addState(intArrayOf(android.R.attr.state_focused), focused)
        sl.addState(intArrayOf(android.R.attr.state_pressed), focused)
        sl.addState(intArrayOf(), shape(Color.argb(28, 255, 255, 255), Color.argb(44, 255, 255, 255), px(1), radius, oval))
        return sl
    }

    fun selector(radius: Int): GradientDrawable =
        shape(Color.argb(60, 255, 255, 255), Color.WHITE, px(3), radius, false)

    fun background(): ImageView {
        val iv = ImageView(ctx)
        iv.setImageResource(R.drawable.bg)
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        return iv
    }
}
