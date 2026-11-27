package com.mytv.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var ui: Ui
    private lateinit var greetView: TextView
    private lateinit var dateView: TextView
    private lateinit var timeView: TextView
    private lateinit var hdmiLabel: TextView
    private lateinit var youtubeTile: View

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 10000)
        }
    }

    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private val days = arrayOf("Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота")
    private val months = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = Ui(this)

        val root = FrameLayout(this)
        root.addView(ui.background(), FrameLayout.LayoutParams(-1, -1))

        // затемнение сверху и снизу, чтобы текст читался
        val top = View(this)
        top.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(115, 0, 0, 0), Color.argb(0, 0, 0, 0))
        )
        root.addView(top, FrameLayout.LayoutParams(-1, ui.px(220), Gravity.TOP))
        val bottom = View(this)
        bottom.background = GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(Color.argb(153, 0, 0, 0), Color.argb(0, 0, 0, 0))
        )
        root.addView(bottom, FrameLayout.LayoutParams(-1, ui.px(330), Gravity.BOTTOM))

        // название слева сверху
        val name = ui.text("МОЙ ТВ", 26, true)
        name.letterSpacing = 0.12f
        val nlp = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START)
        nlp.leftMargin = ui.px(56)
        nlp.topMargin = ui.px(48)
        root.addView(name, nlp)

        // круглые кнопки справа сверху
        val round = LinearLayout(this)
        round.orientation = LinearLayout.HORIZONTAL
        round.addView(roundButton(R.drawable.ic_wifi) { wifi() }, roundParams(false))
        round.addView(roundButton(R.drawable.ic_settings) { systemSettings() }, roundParams(true))
        val rlp = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
        rlp.rightMargin = ui.px(56)
        rlp.topMargin = ui.px(40)
        root.addView(round, rlp)

        // приветствие
        greetView = ui.text("", 48, true)
        val glp = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START)
        glp.leftMargin = ui.px(56)
        glp.topMargin = ui.px(150)
        root.addView(greetView, glp)

        // дата и время справа
        val clock = LinearLayout(this)
        clock.orientation = LinearLayout.VERTICAL
        clock.gravity = Gravity.END
        dateView = ui.text("", 22, true)
        timeView = ui.text("", 96, true)
        clock.addView(dateView, LinearLayout.LayoutParams(-2, -2))
        clock.addView(timeView, LinearLayout.LayoutParams(-2, -2))
        val clp = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
        clp.rightMargin = ui.px(56)
        clp.topMargin = ui.px(130)
        root.addView(clock, clp)

        // ряд плиток
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL

        val tvTile = tile(R.drawable.ic_tv, "Телевизор", 26, 1.5f, true, { openTv() }, { pickApp("tv") })
        val ytTile = tile(R.drawable.ic_youtube, "YouTube", 22, 1f, false, { openYoutube() }, { pickApp("youtube") })
        val hdmiTile = tile(R.drawable.ic_hdmi, "HDMI", 22, 1f, false, { openHdmi() }, { pickApp("hdmi") })
        val wifiTile = tile(R.drawable.ic_wifi, "Wi-Fi", 22, 1f, false, { wifi() }, { pickApp("wifi") })
        val appsTile = tile(R.drawable.ic_apps, "Все приложения", 22, 1f, false,
            { startActivity(Intent(this, AllAppsActivity::class.java)) }, null)
        val setTile = tile(R.drawable.ic_settings, "Настройки", 22, 1f, false, { systemSettings() }, { pickApp("settings") })

        youtubeTile = ytTile.view
        hdmiLabel = hdmiTile.getChildAt(1) as TextView

        for (t in listOf(tvTile, ytTile, hdmiTile, wifiTile, appsTile, setTile)) {
            row.addView(t.first, t.second)
        }
        val rowLp = FrameLayout.LayoutParams(-1, ui.px(130), Gravity.BOTTOM)
        rowLp.leftMargin = ui.px(56)
        rowLp.rightMargin = ui.px(56)
        rowLp.bottomMargin = ui.px(48)
        root.addView(row, rowLp)

        setContentView(root)
        updateClock()
        youtubeTile.requestFocus()
    }

    // ---------- сборка элементов ----------

    private fun tile(
        icon: Int, label: String, size: Int, weight: Float, first: Boolean,
        onClick: () -> Unit, onLong: (() -> Unit)?
    ): TileHolder {
        val t = LinearLayout(this)
        t.orientation = LinearLayout.VERTICAL
        t.gravity = Gravity.BOTTOM or Gravity.START
        t.setPadding(ui.px(20), ui.px(18), ui.px(20), ui.px(18))
        t.background = ui.focusBg(ui.px(20))
        t.isFocusable = true
        t.isClickable = true
        t.setOnClickListener { onClick() }
        if (onLong != null) {
            t.setOnLongClickListener {
                onLong()
                true
            }
        }
        val iv = ImageView(this)
        iv.setImageResource(icon)
        t.addView(iv, LinearLayout.LayoutParams(ui.px(44), ui.px(44)))
        val tv = ui.text(label, size)
        tv.maxLines = 1
        val lp = LinearLayout.LayoutParams(-2, -2)
        lp.topMargin = ui.px(12)
        t.addView(tv, lp)

        val params = LinearLayout.LayoutParams(0, ui.px(130), weight)
        if (!first) params.leftMargin = ui.px(14)
        return TileHolder(t, params)
    }

    private fun roundButton(icon: Int, onClick: () -> Unit): View {
        val f = FrameLayout(this)
        f.background = ui.focusBg(ui.px(26), true)
        f.isFocusable = true
        f.isClickable = true
        f.setOnClickListener { onClick() }
        val iv = ImageView(this)
        iv.setImageResource(icon)
        f.addView(iv, FrameLayout.LayoutParams(ui.px(24), ui.px(24), Gravity.CENTER))
        return f
    }

    private fun roundParams(withMargin: Boolean): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(ui.px(52), ui.px(52))
        if (withMargin) lp.leftMargin = ui.px(14)
        return lp
    }

    // ---------- часы и приветствие ----------

    private fun updateClock() {
        val c = Calendar.getInstance()
        val h = c.get(Calendar.HOUR_OF_DAY)
        val m = c.get(Calendar.MINUTE)
        greetView.text = when {
            h in 5..10 -> "Доброе утро"
            h in 11..16 -> "Добрый день"
            h in 17..22 -> "Добрый вечер"
            else -> "Доброй ночи"
        }
        timeView.text = String.format(Locale.US, "%02d:%02d", h, m)
        dateView.text = days[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " +
            c.get(Calendar.DAY_OF_MONTH) + " " + months[c.get(Calendar.MONTH)]
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    // На главном экране кнопка «Назад» ничего не делает
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
    }

    // ---------- действия плиток ----------

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun launchPackage(pkg: String): Boolean {
        val i = packageManager.getLeanbackLaunchIntentForPackage(pkg)
            ?: packageManager.getLaunchIntentForPackage(pkg)
            ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Если для плитки выбрано своё приложение (долгое нажатие OK), открываем его
    private fun assigned(key: String): Boolean {
        val pkg = prefs.getString("app_$key", null) ?: return false
        return launchPackage(pkg)
    }

    private fun pickApp(key: String) {
        val apps = Apps.list(this)
        val names = ArrayList<String>()
        names.add("Стандартное действие плитки")
        names.add("Диагностика (показать входы и приложения ТВ)")
        for (a in apps) names.add(a.label)
        AlertDialog.Builder(this)
            .setTitle("Какое приложение открывать по этой плитке?")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == 0) {
                    prefs.edit().remove("app_$key").apply()
                    toast("Плитка снова работает по умолчанию")
                } else if (which == 1) {
                    startActivity(Intent(this, DiagActivity::class.java))
                } else {
                    prefs.edit().putString("app_$key", apps[which - 2].pkg).apply()
                    toast("Плитка открывает: " + apps[which - 2].label)
                }
            }
            .show()
    }

    private fun openTv() {
        if (assigned("tv")) return
        CvteTv.connect(this) { ok ->
            if (!ok) {
                toast("Служба ТВ не отвечает (" + CvteTv.lastError + "). Удерживайте OK на плитке и откройте диагностику.")
            } else {
                Thread {
                    val tuner = CvteTv.sources().firstOrNull { it.isTuner && !it.hidden }
                    runOnUiThread {
                        if (tuner == null) {
                            toast("Вход эфирного ТВ не найден. Удерживайте OK на плитке и откройте диагностику.")
                        } else {
                            startSource(tuner)
                        }
                    }
                }.start()
            }
        }
    }

    private fun startSource(src: Src) {
        val i = Intent(this, SourceActivity::class.java)
        i.putExtra("id", src.id)
        startActivity(i)
    }

    private fun openYoutube() {
        if (assigned("youtube")) return
        val candidates = listOf(
            "com.teamsmart.videomanager.tv",
            "com.liskovsoft.smarttubetv.beta",
            "com.google.android.youtube.tv",
            "com.google.android.youtube"
        )
        for (p in candidates) {
            if (launchPackage(p)) return
        }
        toast("YouTube не найден. Удерживайте OK на плитке и выберите приложение.")
    }

    private fun openHdmi() {
        if (assigned("hdmi")) return
        CvteTv.connect(this) { ok ->
            if (!ok) {
                openHdmiTif()
            } else {
                Thread {
                    val hdmi = CvteTv.sources().filter { it.isHdmi && !it.hidden }.sortedBy { it.name }
                    runOnUiThread {
                        if (hdmi.isEmpty()) {
                            openHdmiTif()
                        } else {
                            val names = hdmi.map { it.name }
                            AlertDialog.Builder(this)
                                .setTitle("Выберите вход")
                                .setItems(names.toTypedArray()) { _, which ->
                                    hdmiLabel.text = names[which]
                                    startSource(hdmi[which])
                                }
                                .show()
                        }
                    }
                }.start()
            }
        }
    }

    private fun openHdmiTif() {
        if (assigned("hdmi")) return
        val tim = getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        var inputs: List<TvInputInfo> = emptyList()
        try {
            val all = tim?.tvInputList ?: emptyList()
            inputs = all.filter { it.type == TvInputInfo.TYPE_HDMI }
            if (inputs.isEmpty()) inputs = all.filter { it.isPassthroughInput }
        } catch (e: Exception) {
            inputs = emptyList()
        }
        if (inputs.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Входы HDMI")
                .setMessage(
                    "Прошивка телевизора не отдала список входов. " +
                        "Удерживайте OK на плитке HDMI и выберите приложение, " +
                        "которое переключает источники, если оно есть на телевизоре."
                )
                .setPositiveButton("OK", null)
                .show()
            return
        }
        val sorted = inputs.sortedBy { (it.loadLabel(this) ?: it.id).toString() }
        val labels = sorted.map { (it.loadLabel(this) ?: it.id).toString() }
        AlertDialog.Builder(this)
            .setTitle("Выберите вход")
            .setItems(labels.toTypedArray()) { _, which ->
                try {
                    val i = Intent(this, TvActivity::class.java)
                    i.putExtra("input", sorted[which].id)
                    i.putExtra("label", labels[which])
                    startActivity(i)
                    hdmiLabel.text = labels[which]
                } catch (e: Exception) {
                    toast("Не удалось переключить на этот вход")
                }
            }
            .show()
    }

    private fun wifi() {
        if (assigned("wifi")) return
        val tries = ArrayList<Intent>()
        tries.add(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        tries.add(Intent("android.net.wifi.PICK_WIFI_NETWORK"))
        tries.add(Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.connectivity.NetworkActivity"))
        tries.add(Intent().setClassName("com.android.settings", "com.android.settings.wifi.WifiSettings"))
        tries.add(Intent(Settings.ACTION_WIFI_SETTINGS))
        for (i in tries) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                return
            } catch (e: Exception) {
            }
        }
        toast("Не удалось открыть настройки Wi-Fi")
    }

    private fun systemSettings() {
        if (assigned("settings")) return
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (e: Exception) {
            toast("Не удалось открыть настройки")
        }
    }
}

class TileHolder(val view: LinearLayout, val params: LinearLayout.LayoutParams) {
    val first: LinearLayout get() = view
    val second: LinearLayout.LayoutParams get() = params
    fun getChildAt(i: Int): View = view.getChildAt(i)
}
