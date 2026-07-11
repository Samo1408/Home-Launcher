package com.homelauncher.prime.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.homelauncher.prime.R
import com.homelauncher.prime.data.AppItem
import com.homelauncher.prime.data.AppRepository
import com.homelauncher.prime.util.DesktopStore
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var desktopPager: ViewPager2
    private lateinit var desktopPageIndicator: TextView
    private lateinit var drawerPager: ViewPager2
    private lateinit var pageIndicator: TextView
    private lateinit var widgetClock: TextView
    private lateinit var widgetDate: TextView
    private lateinit var widgetExtra: TextView
    private lateinit var drawer: View
    private lateinit var drawerHeader: View
    private lateinit var openDrawer: View
    private lateinit var closeDrawer: ImageButton
    private lateinit var sortDrawer: ImageButton
    private lateinit var refreshDrawer: ImageButton
    private lateinit var settingsBtn: ImageButton
    private lateinit var tabPersonal: View
    private lateinit var tabWork: View
    private lateinit var rootView: View
    private lateinit var homeContent: View

    private var apps: List<AppItem> = emptyList()
    private var currentWorkTab = false
    private var dragProgress = 0f
    private var dragStartProgress = 0f
    private var dragStartY = 0f
    private var dragStartX = 0f
    private var dragActivated = false
    private var dragCandidate = 0
    private var dragAnimator: ValueAnimator? = null
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    private fun prefs() = PreferenceManager.getDefaultSharedPreferences(this)
    private fun openMode() = prefs().getString("drawer_open_mode", "swipe") ?: "swipe"
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AppRepository.initialize(this)
        findViews()
        setupListeners()
        lifecycleScope.launch {
            AppRepository.appsFlow.collect { list ->
                if (list.isNotEmpty()) { apps = list; renderDesktop(); renderDrawer(currentWorkTab) }
            }
        }
        if (AppRepository.cachedApps.isNotEmpty()) {
            apps = AppRepository.cachedApps; renderDesktop(); renderDrawer(currentWorkTab)
        }
    }

    private fun findViews() {
        rootView = findViewById(R.id.root)
        homeContent = findViewById(R.id.homeContent)
        desktopPager = findViewById(R.id.desktopPager)
        desktopPageIndicator = findViewById(R.id.desktopPageIndicator)
        drawerPager = findViewById(R.id.drawerPager)
        pageIndicator = findViewById(R.id.pageIndicator)
        widgetClock = findViewById(R.id.widgetClock)
        widgetDate = findViewById(R.id.widgetDate)
        widgetExtra = findViewById(R.id.widgetExtra)
        drawer = findViewById(R.id.drawer)
        drawerHeader = findViewById(R.id.drawerHeader)
        openDrawer = findViewById(R.id.openDrawer)
        closeDrawer = findViewById(R.id.closeDrawer)
        sortDrawer = findViewById(R.id.sortDrawer)
        refreshDrawer = findViewById(R.id.refreshDrawer)
        settingsBtn = findViewById(R.id.settingsBtn)
        tabPersonal = findViewById(R.id.tabPersonal)
        tabWork = findViewById(R.id.tabWork)
    }

    private fun setupListeners() {
        openDrawer.setOnClickListener { showDrawer(true) }
        closeDrawer.setOnClickListener { showDrawer(false) }
        refreshDrawer.setOnClickListener { lifecycleScope.launch { AppRepository.refresh(this@HomeActivity) } }
        settingsBtn.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        tabPersonal.setOnClickListener { renderDrawer(false) }
        tabWork.setOnClickListener { renderDrawer(true) }
    }

    private fun renderDesktop() {
        val ids = DesktopStore.getShortcutIds(this)
        val entries = apps.filter { ids.contains(it.id) }.map { DesktopAdapter.Entry.Shortcut(it) }
        desktopPager.adapter = DesktopAdapter(entries, { a, v -> AppGridAdapter.launch(this, a, v) }, { _, _ -> })
    }

    private fun renderDrawer(work: Boolean) {
        currentWorkTab = work
        tabPersonal.alpha = if (work) 0.5f else 1f
        tabWork.alpha = if (work) 1f else 0.5f
        val filtered = if (!work) apps.filter { !it.isWork } else apps.filter { it.isWork }
        drawerPager.adapter = DrawerCellAdapter(filtered)
    }

    private fun showDrawer(show: Boolean) {
        if (show) {
            if (drawer.visibility != View.VISIBLE) { drawer.visibility = View.VISIBLE; applyDragProgress(0f); drawer.post { animateDrag(1f) } }
        } else animateDrag(0f)
    }

    private fun applyDragProgress(p: Float) {
        dragProgress = p.coerceIn(0f, 1f)
        drawer.translationY = (1f - dragProgress) * drawer.height.toFloat()
    }

    private fun animateDrag(target: Float) {
        dragAnimator?.cancel()
        if (dragProgress == target) { applyDragProgress(target); if (target == 0f) drawer.visibility = View.GONE; return }
        if (target > 0f && drawer.visibility != View.VISIBLE) drawer.visibility = View.VISIBLE
        ValueAnimator.ofFloat(dragProgress, target).apply {
            duration = 260; interpolator = DecelerateInterpolator(1.0f)
            addUpdateListener { applyDragProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { if (target <= 0f) drawer.visibility = View.GONE }
            })
        }.also { dragAnimator = it; start() }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        try { if (drawer.visibility != View.VISIBLE && openMode() == "swipe") handleDragTouch(ev) } catch (_: Throwable) {}
        return super.dispatchTouchEvent(ev)
    }

    private fun handleDragTouch(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartY = ev.y; dragStartX = ev.x; dragStartProgress = dragProgress
                dragActivated = false; dragCandidate = if (drawer.visibility == View.VISIBLE) 2 else 1
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragCandidate == 0) return
                val dy = ev.y - dragStartY
                if (!dragActivated) {
                    val needed = dp(16)
                    if ((dragCandidate == 1 && dy < -needed) || (dragCandidate == 2 && dy > needed)) {
                        dragActivated = true; dragAnimator?.cancel()
                        if (drawer.visibility != View.VISIBLE) { drawer.visibility = View.VISIBLE; applyDragProgress(0f) }
                    }
                }
                if (dragActivated) applyDragProgress((dragStartProgress + (-dy) / drawer.height.toFloat()).coerceIn(0f, 1f))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragActivated) {
                    val open = if (dragCandidate == 1) dragProgress > 0.3f else dragProgress > 0.7f
                    animateDrag(if (open) 1f else 0f); dragActivated = false; dragCandidate = 0
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawer.visibility == View.VISIBLE) showDrawer(false) else super.onBackPressed()
    }
}
