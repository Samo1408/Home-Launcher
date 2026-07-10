package com.homelauncher.prime.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.DialogPreference

class GridSizeDialogPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : DialogPreference(context, attrs) {

    var gridValue: String = "5x5"
        set(v) {
            field = v
            persistString(v)
            summary = formatSummary(v)
            notifyChanged()
        }

    override fun onSetInitialValue(defaultValue: Any?) {
        val def = (defaultValue as? String) ?: "5x5"
        gridValue = getPersistedString(def)
    }

    override fun getSummary(): CharSequence = formatSummary(gridValue)

    private fun formatSummary(v: String): String {
        val (c, r) = parse(v)
        return "$c × $r"
    }

    companion object {
        fun parse(v: String): Pair<Int, Int> {
            val p = v.lowercase().split("x")
            val c = p.getOrNull(0)?.toIntOrNull() ?: 5
            val r = p.getOrNull(1)?.toIntOrNull() ?: 5
            return c to r
        }
    }
}
