package com.mytv.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

data class Src(val id: Int, val name: String, val category: String, val status: String) {
    val isHdmi: Boolean get() = category.contains("HDMI", true)
    val isTuner: Boolean
        get() = listOf("DTMB", "DVBT", "DVBC", "DVBS", "ATV", "ATSC", "_TV").any { category.contains(it, true) }
    val isApp: Boolean get() = category.contains("APP", true)
    val hidden: Boolean get() = status.contains("HIDE", true)
}

// Связь со службой ТВ прошивки CVTE (та же, через которую работает родной лаунчер).
// Классы com.cvte.tv.api.* лежат в системной библиотеке телевизора, поэтому всё делается через рефлексию.
object CvteTv {
    private const val PKG = "com.cvte.tv.api."
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var api: Any? = null
    @Volatile private var loader: ClassLoader? = null
    @Volatile var lastError: String = ""

    @Volatile private var initDone = false

    private fun sig(m: java.lang.reflect.Method): String =
        m.name + "(" + m.parameterTypes.joinToString(",") { it.simpleName } + ")" +
            (if (java.lang.reflect.Modifier.isStatic(m.modifiers)) " static" else "")

    // Описание классов SDK из системной библиотеки (для диагностики)
    fun describe(cl: ClassLoader): String {
        val sb = StringBuilder()
        for (n in listOf("TvApiSDKManager", "TvApiApplication", "TvServiceConnection", "TvServiceConnectListener")) {
            try {
                val c = Class.forName(PKG + n, true, cl)
                sb.append(n).append(" : ").append(c.superclass?.simpleName ?: "-").append("\n")
                for (m in c.declaredMethods) sb.append("   ").append(sig(m)).append("\n")
            } catch (e: Throwable) {
                sb.append(n).append(" : ").append((e.cause ?: e).toString()).append("\n")
            }
        }
        return sb.toString()
    }

    fun connect(ctx: Context, cb: (Boolean) -> Unit) {
        if (api != null) {
            cb(true)
            return
        }
        val cl = ctx.classLoader
        val app = ctx.applicationContext
        loader = cl
        var done = false
        val finish = { ok: Boolean ->
            if (!done) {
                done = true
                main.post { cb(ok) }
            }
        }
        try {
            val mgrCls = Class.forName(PKG + "TvApiSDKManager", true, cl)
            val inst = mgrCls.getMethod("getInstance").invoke(null)
            if (!initDone) {
                // SDK требует init() до первого обращения (у родного лаунчера это делает его Application)
                val all = mgrCls.methods.toList() + mgrCls.declaredMethods.toList()
                val im = all.firstOrNull {
                    it.name.equals("init", true) && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0].isAssignableFrom(app.javaClass)
                }
                if (im != null) {
                    im.isAccessible = true
                    im.invoke(if (java.lang.reflect.Modifier.isStatic(im.modifiers)) null else inst, app)
                    initDone = true
                } else {
                    lastError = "в TvApiSDKManager нет подходящего init: " +
                        mgrCls.declaredMethods.joinToString(", ") { sig(it) }
                }
            }
            val connCls = Class.forName(PKG + "TvServiceConnection", true, cl)
            val handler = InvocationHandler { p, m, args ->
                when (m.name) {
                    "onServiceConnected" -> {
                        if (args != null && args.isNotEmpty()) {
                            api = args[0]
                            finish(true)
                        }
                        null
                    }
                    "hashCode" -> System.identityHashCode(p)
                    "equals" -> (args != null && args.isNotEmpty() && p === args[0])
                    "toString" -> "CvteTvConnection"
                    else -> null
                }
            }
            val proxy = Proxy.newProxyInstance(cl, arrayOf(connCls), handler)
            mgrCls.getMethod("getTvApi", connCls).invoke(inst, proxy)
            main.postDelayed({
                if (!done) lastError = "Служба ТВ не ответила за 4 секунды"
                finish(false)
            }, 4000)
        } catch (e: Throwable) {
            lastError = (e.cause ?: e).toString()
            finish(false)
        }
    }

    private fun call(target: Any?, iface: String, name: String, vararg a: Any?): Any? {
        val cl = loader ?: return null
        val c = Class.forName(PKG + "aidl." + iface, true, cl)
        val m = c.methods.first { it.name == name && it.parameterTypes.size == a.size }
        return m.invoke(target, *a)
    }

    private fun field(o: Any, n: String): Any? {
        return try {
            o.javaClass.getField(n).get(o)
        } catch (e: Exception) {
            val f = o.javaClass.getDeclaredField(n)
            f.isAccessible = true
            f.get(o)
        }
    }

    fun sources(): List<Src> {
        val a = api ?: return emptyList()
        return try {
            val si = call(a, "ITvApiManager", "getTVApiSystemInputSource")
            val list = call(si, "ITVApiSystemInputSourceAidl", "eventSystemInputSourceGetList") as? List<*>
                ?: return emptyList()
            list.mapNotNull { e ->
                if (e == null) null
                else Src(
                    (field(e, "id") as? Int) ?: -1,
                    (field(e, "name") ?: "").toString(),
                    (field(e, "category") ?: "").toString(),
                    (field(e, "status") ?: "").toString()
                )
            }
        } catch (e: Throwable) {
            lastError = (e.cause ?: e).toString()
            emptyList()
        }
    }

    fun setSource(id: Int): Boolean {
        val a = api ?: return false
        return try {
            val si = call(a, "ITvApiManager", "getTVApiSystemInputSource")
            (call(si, "ITVApiSystemInputSourceAidl", "eventSystemInputSourceSetInputSource", id) as? Boolean) ?: false
        } catch (e: Throwable) {
            lastError = (e.cause ?: e).toString()
            false
        }
    }

    // Развернуть картинку на весь экран и снять «глушение» экрана
    fun showFull() {
        val a = api ?: return
        try {
            val sw = call(a, "ITvApiManager", "getTVApiScreenWindow")
            try { call(sw, "ITVApiScreenWindowAidl", "eventScreenWindowMute", false) } catch (e: Throwable) { }
            try { call(sw, "ITVApiScreenWindowAidl", "eventScreenWindowSetFull") } catch (e: Throwable) { }
        } catch (e: Throwable) {
            lastError = (e.cause ?: e).toString()
        }
    }
}
