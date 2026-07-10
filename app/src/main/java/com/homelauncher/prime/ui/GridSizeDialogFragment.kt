package com.homelauncher.prime.ui

import android.os.Bundle
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.homelauncher.prime.R

class GridSizeDialogFragment : PreferenceDialogFragmentCompat() {

    private lateinit var pickerCols: NumberPicker
    private lateinit var pickerRows: NumberPicker
    private lateinit var preview: TextView

    companion object {
        private const val MIN = 3
        private const val MAX = 12

        fun newInstance(key: String): GridSizeDialogFragment {
            val f = GridSizeDialogFragment()
            f.arguments = Bundle().apply { putString(ARG_KEY, key) }
            return f
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        pickerCols = view.findViewById(R.id.pickerCols)
        pickerRows = view.findViewById(R.id.pickerRows)
        preview = view.findViewById(R.id.gridPreviewLabel)

        val pref = preference as GridSizeDialogPreference
        val (c, r) = GridSizeDialogPreference.parse(pref.gridValue)

        // السحب لفوق => الرقم يزيد، لتحت => يقل (سلوك NumberPicker الافتراضي)
        setupPicker(pickerCols, c)
        setupPicker(pickerRows, r)

        updatePreview()
        pickerCols.setOnValueChangedListener { _, _, _ -> updatePreview() }
        pickerRows.setOnValueChangedListener { _, _, _ -> updatePreview() }
    }

    private fun setupPicker(p: NumberPicker, value: Int) {
        p.minValue = MIN
        p.maxValue = MAX
        p.wrapSelectorWheel = false
        p.value = value.coerceIn(MIN, MAX)
    }

    private fun updatePreview() {
        preview.text = "${pickerCols.value} × ${pickerRows.value}"
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (!positiveResult) return
        val pref = preference as GridSizeDialogPreference
        val v = "${pickerCols.value}x${pickerRows.value}"
        if (pref.callChangeListener(v)) {
            pref.gridValue = v
        }
    }
}
