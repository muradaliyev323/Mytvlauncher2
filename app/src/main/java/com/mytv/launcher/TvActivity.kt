package com.mytv.launcher

import android.app.Activity
import android.graphics.Color
import android.media.tv.TvContract
import android.media.tv.TvView
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

// Показывает выбранный вход (например HDMI) прямо внутри лаунчера. «Назад» возвращает на главный экран.
class TvActivity : Activity() {

    private var tv: TvView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inputId = intent.getStringExtra("input")
        val label = intent.getStringExtra("label") ?: ""
        if (inputId == null) {
            finish()
            return
        }
        val ui = Ui(this)
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        val view = TvView(this)
        tv = view
        root.addView(view, FrameLayout.LayoutParams(-1, -1))

        val msg = ui.text("Подключаю " + label + "…", 30, true)
        msg.setShadowLayer(8f, 0f, 0f, Color.BLACK)
        val mlp = FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)
        root.addView(msg, mlp)

        view.setCallback(object : TvView.TvInputCallback() {
            override fun onConnectionFailed(inputId: String) {
                msg.text = "Не удалось подключиться к " + label
                msg.visibility = View.VISIBLE
            }

            override fun onVideoAvailable(inputId: String) {
                msg.visibility = View.GONE
            }

            override fun onVideoUnavailable(inputId: String, reason: Int) {
                msg.text = label + ": нет сигнала (код " + reason + ")"
                msg.visibility = View.VISIBLE
            }
        })

        setContentView(root)
        try {
            view.tune(inputId, TvContract.buildChannelUriForPassthroughInput(inputId))
        } catch (e: Exception) {
            msg.text = "Ошибка: " + e.message
        }
    }

    override fun onDestroy() {
        try {
            tv?.reset()
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
