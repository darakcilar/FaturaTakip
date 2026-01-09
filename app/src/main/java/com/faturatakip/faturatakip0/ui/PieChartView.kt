package com.faturatakip.faturatakip0.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val rectF = RectF()
    private var data: Map<String, Float> = emptyMap()

    val colors = listOf(
        "#4285F4", "#DB4437", "#F4B400", "#0F9D58",
        "#AA46BE", "#FF7043", "#5C6BC0", "#26C6DA"
    ).map { Color.parseColor(it) }

    fun setData(input: Map<String, Float>) {
        this.data = input
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val total = data.values.sum()
        if (total == 0f) return

        val width = width.toFloat()
        val height = height.toFloat()
        val size = if (width < height) width else height
        val margin = 40f

        rectF.set(
            (width - size) / 2 + margin,
            (height - size) / 2 + margin,
            (width + size) / 2 - margin,
            (height + size) / 2 - margin
        )

        var startAngle = -90f
        data.entries.forEachIndexed { index, entry ->
            val sweepAngle = (entry.value / total) * 360f
            paint.color = colors[index % colors.size]
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        paint.color = Color.WHITE
        canvas.drawCircle(width / 2, height / 2, size / 3.5f, paint)
    }
}