package com.mytv.launcher

import android.content.Context
import android.content.Intent

data class AppItem(val label: String, val pkg: String, val cls: String)

object Apps {
    // Все приложения, которые можно запустить (обычные и для Android TV), без самого лаунчера
    fun list(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val out = LinkedHashMap<String, AppItem>()
        val cats = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (cat in cats) {
            val i = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(i, 0)) {
                val ai = ri.activityInfo ?: continue
                if (ai.packageName == ctx.packageName) continue
                if (!out.containsKey(ai.packageName)) {
                    out[ai.packageName] = AppItem(ri.loadLabel(pm).toString(), ai.packageName, ai.name)
                }
            }
        }
        return out.values.sortedBy { it.label.lowercase() }
    }

    fun launch(ctx: Context, item: AppItem): Boolean {
        return try {
            val i = Intent(Intent.ACTION_MAIN)
            i.setClassName(item.pkg, item.cls)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ctx.startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }
}
