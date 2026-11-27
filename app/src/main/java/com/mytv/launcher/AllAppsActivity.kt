package com.mytv.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class AllAppsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = Ui(this)
        val apps = Apps.list(this)
        val icons = HashMap<String, Drawable?>()

        val root = FrameLayout(this)
        root.addView(ui.background(), FrameLayout.LayoutParams(-1, -1))
        val dim = View(this)
        dim.setBackgroundColor(Color.argb(190, 0, 0, 0))
        root.addView(dim, FrameLayout.LayoutParams(-1, -1))

        val title = ui.text("Все приложения", 36, true)
        val tlp = FrameLayout.LayoutParams(-2, -2)
        tlp.leftMargin = ui.px(56)
        tlp.topMargin = ui.px(40)
        root.addView(title, tlp)

        val grid = GridView(this)
        grid.numColumns = 6
        grid.horizontalSpacing = ui.px(14)
        grid.verticalSpacing = ui.px(14)
        grid.stretchMode = GridView.STRETCH_COLUMN_WIDTH
        grid.selector = ui.selector(ui.px(20))
        grid.clipToPadding = false
        grid.setPadding(ui.px(56), ui.px(8), ui.px(56), ui.px(40))
        grid.setBackgroundColor(Color.TRANSPARENT)
        grid.cacheColorHint = Color.TRANSPARENT

        grid.adapter = object : BaseAdapter() {
            override fun getCount(): Int = apps.size
            override fun getItem(position: Int): Any = apps[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val app = apps[position]
                val cell = LinearLayout(this@AllAppsActivity)
                cell.orientation = LinearLayout.VERTICAL
                cell.gravity = Gravity.CENTER
                cell.setPadding(ui.px(10), ui.px(10), ui.px(10), ui.px(10))
                cell.layoutParams = AbsListView.LayoutParams(-1, ui.px(150))

                val iv = ImageView(this@AllAppsActivity)
                val icon = icons.getOrPut(app.pkg) {
                    try {
                        packageManager.getApplicationIcon(app.pkg)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (icon != null) iv.setImageDrawable(icon)
                cell.addView(iv, LinearLayout.LayoutParams(ui.px(72), ui.px(72)))

                val tv = ui.text(app.label, 18)
                tv.gravity = Gravity.CENTER
                tv.maxLines = 2
                tv.ellipsize = TextUtils.TruncateAt.END
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.topMargin = ui.px(8)
                cell.addView(tv, lp)
                return cell
            }
        }

        grid.setOnItemClickListener { _, _, position, _ ->
            if (!Apps.launch(this, apps[position])) {
                Toast.makeText(this, "Не удалось открыть приложение", Toast.LENGTH_LONG).show()
            }
        }

        val glp = FrameLayout.LayoutParams(-1, -1)
        glp.topMargin = ui.px(110)
        root.addView(grid, glp)

        setContentView(root)
        grid.requestFocus()
    }
}
