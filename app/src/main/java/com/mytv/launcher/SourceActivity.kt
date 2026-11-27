package com.mytv.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast

// Прозрачный экран: телевизор показывает выбранный источник (ТВ или HDMI) под системным слоем.
// «Назад» возвращает в меню лаунчера.
class SourceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFormat(PixelFormat.TRANSLUCENT)
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val v = View(this)
        v.setBackgroundColor(Color.TRANSPARENT)
        setContentView(v)

        val id = intent.getIntExtra("id", -1)
        Toast.makeText(this, "Назад — вернуться в меню", Toast.LENGTH_SHORT).show()
        Thread {
            CvteTv.setSource(id)
            CvteTv.showFull()
        }.start()
    }

    override fun onDestroy() {
        // Возвращаем «системный» источник, чтобы звук и картинка выбранного входа не продолжались в меню
        Thread {
            try {
                val app = CvteTv.sources().firstOrNull { it.isApp }
                if (app != null) CvteTv.setSource(app.id)
            } catch (e: Throwable) {
            }
        }.start()
        super.onDestroy()
    }
}
