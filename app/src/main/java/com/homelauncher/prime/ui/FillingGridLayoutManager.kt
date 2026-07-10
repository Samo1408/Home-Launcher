package com.homelauncher.prime.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * GridLayoutManager that forces every child cell to fill an exact slice of the
 * page so that `cols x rows` cells exactly cover the available area, no matter
 * how big or small the icons inside are.
 */
class FillingGridLayoutManager(
    ctx: Context,
    cols: Int,
    private val rows: Int
) : GridLayoutManager(ctx, cols) {

    init { isItemPrefetchEnabled = false }

    override fun canScrollVertically(): Boolean = false
    override fun canScrollHorizontally(): Boolean = false

    private fun apply(lp: RecyclerView.LayoutParams): RecyclerView.LayoutParams {
        val w = (width - paddingLeft - paddingRight)
        val h = (height - paddingTop - paddingBottom)
        if (w > 0) lp.width = w / spanCount else lp.width = ViewGroup.LayoutParams.MATCH_PARENT
        if (h > 0 && rows > 0) lp.height = h / rows else lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
        return lp
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams =
        apply(super.generateDefaultLayoutParams() as RecyclerView.LayoutParams)

    override fun generateLayoutParams(c: Context, attrs: AttributeSet): RecyclerView.LayoutParams =
        apply(super.generateLayoutParams(c, attrs) as RecyclerView.LayoutParams)

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): RecyclerView.LayoutParams =
        apply(super.generateLayoutParams(lp) as RecyclerView.LayoutParams)
}
