package com.homelauncher.prime.ui

import android.app.admin.DevicePolicyManager
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.net.TrafficStats
import android.os.Handler
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Locale

import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.homelauncher.prime.R
import com.homelauncher.prime.data.AppItem
import com.homelauncher.prime.data.AppRepository
import com.homelauncher.prime.util.DesktopStore
import com.homelauncher.prime.util.IconCache
import com.homelauncher.prime.util.IntentUtil
import com.homelauncher.prime.util.RootUtil
import com.homelauncher.prime.ui.DrawerCellAdapter.SelectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class HomeActivity : AppCompatActivity() {

    private lateinit var desktopPager: ViewPager2
    private lateinit var desktopPageIndicator: TextView
    private lateinit var drawerPager: ViewPager2
    private lateinit var drawerVertical: RecyclerView
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
    private var dragProgress: Float = 0f
    private var dragStartProgress: Float = 0f
    private var dragStartY: Float = 0f
    private var dragStartX: Float = 0f
    private var dragActivated: Boolean = false
    private var dragCandidate: Int = 0
    private var dragAnimator: ValueAnimator? = null
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    private fun prefs() = PreferenceManager.getDefaultSharedPreferences(this)
    private fun openMode(): String = prefs().getString("drawer_open_mode", "swipe") ?: "swipe"
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        AppRepository.registerPackageListener(this)
        rootView = findViewById(R.id.root)
        homeContent = findViewById(R.id.homeContent)
        desktopPager = findViewById(R.id.desktopPager)
        desktopPageIndicator = findViewById(R.id.desktopPageIndicator)
        drawerPager = findViewById(R.id.drawerPager)
        drawerVertical = findViewById(R.id.drawerVertical)
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

        openDrawer.setOnClickListener { showDrawer(true) }
        closeDrawer.setOnClickListener { showDrawer(false) }
        sortDrawer.setOnClickListener { /* TODO: sort menu */ }
        refreshDrawer.setOnClickListener { AppRepository.invalidate(); loadApps() }
        settingsBtn.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        tabPersonal.setOnClickListener { renderDrawer(false) }
        tabWork.setOnClickListener { renderDrawer(true) }

        AppRepository.cached()?.let { apps = it; renderDesktop(); renderDrawer(currentWorkTab) }
        if (apps.isEmpty() || AppRepository.dirty) loadApps()
    }

    private fun loadApps() {
        CoroutineScope(Dispatchers.IO).aunch {
            val list = AppRepository.loadAll(this@HomeActivity)
            withContext(Dispatchers.Main) {
                if (list.size != apps.size || list.hashCode() != apps.hashCode()) {
                    apps = list
                    renderDesktop()
                    renderDrawer(currentWorkTab)
                }
            }
        }
    }

    private fun renderDesktop() {
        val shortcutIds = DesktopStore.getShortcutIds(this)
        val entries = apps.filter { shortcutIds.contains(it.id) }.map { DesktopAdapter.Entry.Shortcut(it) }
        desktopPager.adapter = DesktopAdapter(entries, { a, v -> launchApp(a, v) }, { a, v -> /* long click */ })
    }

    private fun renderDrawer(work: Boolean) {
        currentWorkTab = work
        tabPersonal.alpha = if (work) 0.5f else 1f
        tabWork.alpha = if (work) 1f else 0.5f
        val filtered = if (!work) apps.filter { !it.isWork } else apps.filter { it.isWork }
        drawerPager.adapter = DrawerCellAdapter(filtered)
    }

    private fun launchApp(item: AppItem, source: View) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(item.packageName)
            if (launchIntent != null) startActivity(launchIntent)
        } catch (_: Throwable) {}
    }

    private fun showDrawer(show: Boolean) {
        if (show) {
            if (drawer.visibility != View.VISIBLE) {
                drawer.visibility = View.VISIBLE
                applyDragProgress(0f)
                drawer.post { animateDrag(1f) }
            }
        } else {
            animateDrag(0f)
        }
    }

    private fun applyDragProgress(p: Float) {
        val pp = p.coerceIn(0f, 1f)
        dragProgress = pp
        val h = drawer.height.toFloat()
        drawer.translationY = (1f - pp) * h
    }

    private fun animateDrag(target: Float) {
        dragAnimator?.cancel()
        val from = dragProgress
        if (from == target) { applyDragProgress(target); if (target == 0f) drawer.visibility = View.GONE; return }
        if (target > 0f && drawer.visibility != View.VISIBLE) drawer.visibility = View.VISIBLE
        val anim = ValueAnimator.ofFloat(from, target)
        anim.duration = 260
        anim.interpolator = DecelerateInterpolator(1.0f)
        anim.addUpdateListener { applyDragProgress(it.animatedValue as Float) }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                if (target <= 0f) drawer.visibility = View.GONE
            }
        })
        dragAnimator = anim
        anim.start()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        try {
            if (drawer.visibility != View.VISIBLE) {
                if (openMode() == "swipe") handleDragTouch(ev)
            }
        } catch (_: Throwable) {}
        return super.dispatchTouchEvent(ev)
    }

    private fun handleDragTouch(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartY = ev.y
                dragStartX = ev.x
                dragStartProgress = dragProgress
                dragActivated = false
                dragCandidate = if (drawer.visibility == View.VISIBLE) 2 else 1
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragCandidate == 0) return
                val dy = ev.y - dragStartY
                if (!dragActivated) {
                    val needed = dw(16)
                    if ((dragCandidate == 1 && dy < -needed) || (dragCandidate == 2 && dy > needed)) {
                        dragActivated = true
                        dragAnimator?.cancel()
                        if (drawer.visibility != View.VISIBLE) {
                            drawer.visibility = View.VISIBLE
                            applyDragProgress(0f)
                        }
                    }
                }
                if (dragActivated) {
                    val p = (dragStartProgress + (-dy) / drawer.height.toFloat()).coerceIn(0f, 1f)
                    applyDragProgress(p)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragActivated) {
                    val open = if (dragCandidate == 1) dragProgress > 0.3f else dragProgress > 0.7F
                    animateDrag(if (open) 1f else 0f)
                    dragActivated = false
                    dragCandidate = 0
                }
            }
        }
    }

    private fun lockNow() {
        if (prefs().getString("lock_method", "root") == "root" && RootUtil.lockScreen()) return
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(IntentUtil.adminComponent(this))) dpm.lockNow()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawer.visibility == View.VISIBLE) showDrawer(false) else super.onBackPressed()
    }
}
