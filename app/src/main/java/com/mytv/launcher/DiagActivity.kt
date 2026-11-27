package com.mytv.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.tv.TvInputManager
import android.os.Bundle
import android.util.TypedValue
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

// Список входов ТВ и «телевизионных» приложений прошивки, чтобы понять, как включать ТВ и HDMI.
class DiagActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = Ui(this)
        val pm = packageManager
        val sb = StringBuilder()

        sb.append("== ВХОДЫ ТВ ==\n")
        try {
            val tim = getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            val list = tim?.tvInputList ?: emptyList()
            for (i in list) {
                sb.append(i.id).append("\n  тип=").append(i.type)
                    .append(" passthrough=").append(i.isPassthroughInput)
                    .append(" имя=").append(i.loadLabel(this)).append("\n")
            }
        } catch (e: Exception) {
            sb.append("ошибка: ").append(e).append("\n")
        }

        sb.append("\n== ЛАУНЧЕРЫ (HOME) ==\n")
        try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            for (ri in pm.queryIntentActivities(home, 0)) {
                sb.append(ri.activityInfo.packageName).append(" / ").append(ri.activityInfo.name).append("\n")
            }
        } catch (e: Exception) {
            sb.append("ошибка: ").append(e).append("\n")
        }

        sb.append("\n== КОПИИ APK ЛАУНЧЕРОВ ==\n")
        try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val dirs = getExternalFilesDirs(null)
            for (ri in pm.queryIntentActivities(home, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName) continue
                val src = File(pm.getApplicationInfo(pkg, 0).sourceDir)
                for (d in dirs) {
                    if (d == null) continue
                    try {
                        val dst = File(d, pkg + ".apk")
                        src.copyTo(dst, true)
                        sb.append(dst.absolutePath).append("  ").append(dst.length() / 1024).append(" КБ\n")
                    } catch (e: Exception) {
                        sb.append("не скопировалось в ").append(d.absolutePath).append(": ").append(e.message).append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            sb.append("ошибка: ").append(e).append("\n")
        }

        sb.append("\n== БИБЛИОТЕКИ CVTE (копии на флешку) ==\n")
        try {
            val dirs = getExternalFilesDirs(null)
            val files = File("/system/framework").listFiles() ?: emptyArray()
            for (f in files) {
                if (!f.name.contains("cvte", true)) continue
                sb.append(f.name).append("  ").append(f.length() / 1024).append(" КБ\n")
                for (d in dirs) {
                    if (d == null) continue
                    try {
                        f.copyTo(File(d, f.name), true)
                    } catch (e: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            sb.append("ошибка: ").append(e).append("\n")
        }

        sb.append("\n== СЛУЖБЫ com.cvte.tv.api.impl ==\n")
        try {
            val spi = pm.getPackageInfo("com.cvte.tv.api.impl", PackageManager.GET_SERVICES)
            for (sv in spi.services ?: emptyArray()) {
                sb.append(sv.name).append(if (sv.exported) "  (e)" else "").append("\n")
            }
        } catch (e: Exception) {
            sb.append("ошибка: ").append(e).append("\n")
        }

        sb.append("\n== API: КЛАССЫ ==\n").append(CvteTv.describe(classLoader))

        sb.append("\n== ПАКЕТЫ ТВ ==\n")
        val re = Regex("(?i)(tv|source|input|hdmi|live|dtv|atv|dvb|tuner|channel|nebula|mstar|mtk|amlogic|allwinner|rockchip|hisi|realtek)")
        var n = 0
        try {
            for (pi in pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)) {
                val pkg = pi.packageName
                val label = pi.applicationInfo?.loadLabel(pm)?.toString() ?: ""
                if (!re.containsMatchIn(pkg) && !re.containsMatchIn(label)) continue
                if (n >= 40) break
                n++
                sb.append(pkg).append("  [").append(label).append("]\n")
                val acts = pi.activities ?: continue
                for (a in acts.take(8)) {
                    sb.append("    ").append(a.name.removePrefix(pkg))
                    if (a.exported) sb.append("  (e)")
                    sb.append("\n")
                }
            }
        } catch (e: Exception) {
            sb.append("ошибка: ").append(e).append("\n")
        }

        val tv = TextView(this)
        tv.text = sb.toString()
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17 * ui.scale)
        tv.typeface = Typeface.MONOSPACE
        tv.setPadding(ui.px(40), ui.px(30), ui.px(40), ui.px(30))

        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.BLACK)
        sv.isFocusable = true
        sv.addView(tv)
        setContentView(sv)
        sv.requestFocus()

        CvteTv.connect(this) { ok ->
            if (!ok) {
                tv.append("\n== CVTE API ==\nне подключилось: " + CvteTv.lastError + "\n")
            } else {
                Thread {
                    val list = CvteTv.sources()
                    val t = StringBuilder("\n== CVTE API: ИСТОЧНИКИ ==\n")
                    for (x in list) {
                        t.append(x.id).append("  ").append(x.name).append("  ").append(x.category)
                            .append("  ").append(x.status).append("\n")
                    }
                    if (list.isEmpty()) t.append("пусто: ").append(CvteTv.lastError).append("\n")
                    runOnUiThread { tv.append(t.toString()) }
                }.start()
            }
        }

        // Старая версия лаунчера (другое имя пакета) мешает выбору главного экрана — предлагаем удалить
        var oldInstalled = false
        try {
            pm.getPackageInfo("com.mytv.launcher", 0)
            oldInstalled = true
        } catch (e: Exception) {
        }
        if (oldInstalled) {
            AlertDialog.Builder(this)
                .setTitle("Найдена старая версия «Мой ТВ»")
                .setMessage("Удалить её? Новая версия «Мой ТВ 2» останется.")
                .setPositiveButton("Удалить") { _, _ ->
                    try {
                        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:com.mytv.launcher")))
                    } catch (e: Exception) {
                    }
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }
}
