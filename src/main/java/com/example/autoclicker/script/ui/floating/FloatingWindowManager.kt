package com.example.autoclicker.script.ui.floating

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.autoclicker.R
import com.example.autoclicker.script.executor.ScriptActionCalculator
import com.example.autoclicker.script.executor.ScriptScheduler
import com.example.autoclicker.script.model.Script
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.ui.dialog.bottomSheet.ActionEditFloatWindow
import kotlin.math.abs

class FloatingWindowManager(
    private val context: Context,
    private val script: Script
) {
    private var isAddingSwipe = false

    internal var windowManager: WindowManager? = null
    internal var floatingView: View? = null
    internal var layoutParams: WindowManager.LayoutParams? = null
    internal var layoutType: Int = 0

    private var lastX = 0
    private var lastY = 0
    private var isMoving = false
    private var screenRealWidth = 0
    private var screenRealHeight = 0
    private var floatingViewWidth = 0
    private var floatingViewHeight = 0

    internal var areaDrawOverlay: MarkDrawView? = null
    internal val markAreaList = mutableListOf<MarkAreaItem>()
    var overlayParams: WindowManager.LayoutParams? = null

    private val circleSizeDp = 36f
    private val circleSizePx by lazy {
        (context.resources.displayMetrics.density * circleSizeDp).toInt()
    }
    internal val halfCirclePx by lazy { circleSizePx / 2 }

    // ====================== 按你要求顺序排序 ======================
    private var startBtnFunction: StartBtnFunction? = null
    private var stopBtnFunction: StopBtnFunction? = null

    private var addClickBtnFunction: AddClickBtnFunction? = null
    private var removeBtnFunction: RemoveBtnFunction? = null
    private var addSwipeBtnFunction: AddSwipeBtnFunction? = null

    private var saveBtnFunction: SaveBtnFunction? = null
    private var settingBtnFunction: SettingBtnFunction? = null
    private var closeBtnFunction: CloseBtnFunction? = null

    private var scriptScheduler: ScriptScheduler? = null
    private var startStopBtn: ImageButton? = null

    private var isEditMode = false

    // ====================== 修复：延迟初始化，不提前findViewById ======================
    private var btnEdit: ImageButton? = null
    private var btnClean: ImageButton? = null

    init {
        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            getRealScreenSize()
            initLayoutParams()
            initAreaDrawOverlay()
            initFunctions()
            initView()
        } catch (e: Exception) {
            Log.e("FloatingWindow", "初始化失败", e)
        }
    }

    private fun setOverlayPenetrate(penetrate: Boolean) {
        if (overlayParams == null) return

        val baseFlags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        overlayParams?.flags = if (penetrate) {
            baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            baseFlags
        }

        if (areaDrawOverlay?.parent != null) {
            windowManager?.updateViewLayout(areaDrawOverlay, overlayParams)
        }
    }

    private fun toggleEditOffIfNeeded() {
        if (!isEditMode) {
            isEditMode = true
            btnEdit?.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, android.R.color.holo_red_light)
            )
            setOverlayPenetrate(false)
        }
    }

    fun startScript() {
        val serviceEnabled = com.example.autoclicker.service.AutoClickAccessibilityService.isAccessibilityServiceEnabled(context)
        if (!serviceEnabled) {
            Toast.makeText(context, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            com.example.autoclicker.service.AutoClickAccessibilityService.jumpToAccessibilitySetting(context)
            return
        }

        scriptScheduler?.stop()
        scriptScheduler = ScriptScheduler(script, areaDrawOverlay!!)
        scriptScheduler?.start()
        script.isRunning = true
        updateStartStopButton(true)
        startBtnFunction?.performStart(script)
    }

    fun stopScript() {
        scriptScheduler?.stop()
        scriptScheduler = null
        script.isRunning = false
        updateStartStopButton(false)
        stopBtnFunction?.performStop(script)
    }

    private fun updateStartStopButton(isRunning: Boolean) {
        startStopBtn?.let { btn ->
            if (isRunning) {
                btn.setImageResource(R.drawable.ic_stop)
                btn.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.holo_red_light)
                )
            } else {
                btn.setImageResource(R.drawable.ic_start)
                btn.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.holo_green_light)
                )
            }
        }
    }

    private fun getRealScreenSize() {
        val wm = context.getSystemService(WindowManager::class.java)
        val displayMetrics = context.resources.displayMetrics

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val bounds = metrics.bounds
            screenRealWidth = bounds.width()
            screenRealHeight = bounds.height()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val display = wm.defaultDisplay
            val realMetrics = android.util.DisplayMetrics()
            display.getRealMetrics(realMetrics)
            screenRealWidth = realMetrics.widthPixels
            screenRealHeight = realMetrics.heightPixels
        } else {
            screenRealWidth = displayMetrics.widthPixels
            screenRealHeight = displayMetrics.heightPixels
        }

        Log.d("FloatingWindow", "真实屏幕尺寸: ${screenRealWidth}x${screenRealHeight}")
        areaDrawOverlay?.setRealScreenSize(screenRealWidth, screenRealHeight)
    }

    private fun initLayoutParams() {
        layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = screenRealWidth / 4
            y = screenRealHeight / 4

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun initAreaDrawOverlay() {
        areaDrawOverlay = MarkDrawView(context)
        areaDrawOverlay?.setRealScreenSize(screenRealWidth, screenRealHeight)

        areaDrawOverlay?.onMarkClicked = { markItem, _ ->
            val dlg = ActionEditFloatWindow(
                context = context,
                script = script,
                markItem = markItem,
                onParamsReady = { idx, dMin, dMax, durMin, durMax, dUnit, durUnit ->
                    val ok = ScriptActionCalculator.processActionParams(
                        script, idx, dMin, dMax, durMin, durMax, dUnit, durUnit
                    )
                    if (ok) refreshDraw() else Toast.makeText(context, "参数错误", Toast.LENGTH_SHORT).show()
                }
            )
            dlg.show()
        }

        areaDrawOverlay?.onScreenTap = { x, y ->
            if (isAddingSwipe) {
                addSwipeBtnFunction?.onScreenClick(x.toInt(), y.toInt())
            }
        }

        areaDrawOverlay?.dragListener = object : MarkDrawView.OnMarkDragListener {
            override fun onDragStart(item: MarkAreaItem, isDragEnd: Boolean) {}
            override fun onDragMove(item: MarkAreaItem, newLeft: Int, newTop: Int, isDragEnd: Boolean) {
                if (isDragEnd) {
                    val w = item.swipeEndRect.width()
                    val h = item.swipeEndRect.height()
                    val l = newLeft.coerceIn(0, screenRealWidth - w)
                    val t = newTop.coerceIn(0, screenRealHeight - h)
                    item.swipeEndRect.set(l, t, l + w, t + h)
                    if (item.actionIndex in script.actions.indices) {
                        val a = script.actions[item.actionIndex]
                        if (a is ScriptAction.Swipe) {
                            a.endX = item.swipeEndRect.exactCenterX().toInt()
                            a.endY = item.swipeEndRect.exactCenterY().toInt()
                        }
                    }
                } else {
                    val w = item.rect.width()
                    val h = item.rect.height()
                    val l = newLeft.coerceIn(0, screenRealWidth - w)
                    val t = newTop.coerceIn(0, screenRealHeight - h)
                    item.rect.set(l, t, l + w, t + h)
                    if (item.actionIndex in script.actions.indices) {
                        when (val a = script.actions[item.actionIndex]) {
                            is ScriptAction.Click -> a.clickRect = Rect(item.rect)
                            is ScriptAction.Swipe -> {
                                a.startX = item.rect.exactCenterX().toInt()
                                a.startY = item.rect.exactCenterY().toInt()
                            }
                        }
                    }
                }
                refreshDraw()
            }
            override fun onDragEnd() {}
        }

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun showFullOverlay() {
        if (areaDrawOverlay?.parent == null && overlayParams != null) {
            areaDrawOverlay?.setRealScreenSize(screenRealWidth, screenRealHeight)
            windowManager?.addView(areaDrawOverlay, overlayParams)
        }
    }

    private fun hideFullOverlay() {
        areaDrawOverlay?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
        }
    }

    // ====================== 函数顺序严格按你要求 ======================
    private fun initFunctions() {
        // 1. 开始/停止
        startBtnFunction = StartBtnFunction(context, markAreaList) {}
        stopBtnFunction = StopBtnFunction(context, markAreaList)

        // 2. 添加点击
        addClickBtnFunction = AddClickBtnFunction(
            context = context,
            markDrawView = areaDrawOverlay!!,
            onComplete = { rect ->
                val click = ScriptAction.Click(rect, 100, 500)
                val idx = script.actions.size
                script.actions.add(click)
                markAreaList.add(
                    MarkAreaItem(
                        rect = rect,
                        actionType = MarkAreaItem.ActionType.CLICK,
                        actionNumber = idx + 1,
                        actionIndex = idx
                    )
                )
                refreshDraw()
                Handler(Looper.getMainLooper()).post {
                    show()
                    Toast.makeText(context, "点击已添加", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // 3. 移除
        removeBtnFunction = RemoveBtnFunction(
            script = script,
            markAreaList = markAreaList,
            refreshDraw = this::refreshDraw
        )

        // 4. 添加滑动
        addSwipeBtnFunction = AddSwipeBtnFunction(
            onSelecting = { _, sx, sy, ex, ey ->
                areaDrawOverlay?.previewStartX = sx
                areaDrawOverlay?.previewStartY = sy
                areaDrawOverlay?.previewEndX = ex
                areaDrawOverlay?.previewEndY = ey
                if (sx != null && sy != null && ex == null) {
                    areaDrawOverlay?.swipeTipText = "请点击终点位置"
                }
                refreshDraw()
            },
            onSelected = { sx, sy, ex, ey ->
                isAddingSwipe = false
                areaDrawOverlay?.resetSwipeMode()
                val r = halfCirclePx
                val sRect = Rect(sx - r, sy - r, sx + r, sy + r)
                val eRect = Rect(ex - r, ey - r, ex + r, ey + r)
                val swipe = ScriptAction.Swipe(
                    sx, sy, ex, ey, 200, 500, 0, 500
                )
                val idx = script.actions.size
                script.actions.add(swipe)
                markAreaList.add(
                    MarkAreaItem(
                        rect = sRect,
                        swipeEndRect = eRect,
                        actionType = MarkAreaItem.ActionType.SWIPE,
                        actionNumber = idx + 1,
                        actionIndex = idx
                    )
                )
                refreshDraw()
                show()
                Toast.makeText(context, "滑动已添加", Toast.LENGTH_SHORT).show()
            }
        )

        // 5. 保存
        saveBtnFunction = SaveBtnFunction(context, script, markAreaList,this::refreshDraw)

        // 6. 设置
        settingBtnFunction = SettingBtnFunction(context, script)

        // 7. 关闭
        closeBtnFunction = CloseBtnFunction(this)
    }

    private fun initView() {
        floatingView = LayoutInflater.from(context).inflate(R.layout.layout_floating_window, null)

        // ====================== 核心修复：在这里初始化，不再为空 ======================
        btnEdit = floatingView?.findViewById(R.id.btn_edit)
        btnClean = floatingView?.findViewById(R.id.btn_clean)
        startStopBtn = floatingView?.findViewById(R.id.btn_start)

        floatingView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                floatingViewWidth = floatingView?.width ?: 0
                floatingViewHeight = floatingView?.height ?: 0
                floatingView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    isMoving = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    if (abs(dx) > 5 || abs(dy) > 5) {
                        isMoving = true
                        val newX = (layoutParams?.x ?: 0) + dx
                        val newY = (layoutParams?.y ?: 0) + dy
                        layoutParams?.x = newX.coerceIn(0, screenRealWidth - floatingViewWidth)
                        layoutParams?.y = newY.coerceIn(0, screenRealHeight - floatingViewHeight)
                        windowManager?.updateViewLayout(floatingView, layoutParams)
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> isMoving
                else -> false
            }
        }

        floatingView?.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener { closeFloatingWindow() }
        floatingView?.findViewById<ImageButton>(R.id.btn_settings)?.setOnClickListener { showSettingsBottomSheet() }

        floatingView?.findViewById<ImageButton>(R.id.btn_add)?.setOnClickListener {
            toggleEditOffIfNeeded()
            showFullOverlay()
            addClickBtnFunction?.start()
        }

        floatingView?.findViewById<ImageButton>(R.id.btn_remove)?.setOnClickListener { removeLastAction() }

        btnEdit?.setOnClickListener {
            isEditMode = !isEditMode
            if (isEditMode) {
                btnEdit?.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.holo_red_light))
                setOverlayPenetrate(false)
            } else {
                btnEdit?.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, android.R.color.white))
                setOverlayPenetrate(true)
            }
        }

        btnClean?.setOnClickListener {
            clearSavedState()
        }

        floatingView?.findViewById<ImageButton>(R.id.btn_save)?.setOnClickListener { saveState() }

        startStopBtn?.setOnClickListener {
            if (!script.isSetting) {
                showSettingsBottomSheet()
            } else {
                if (script.isRunning) stopScript() else startScript()
            }
        }

        floatingView?.findViewById<ImageButton>(R.id.btn_swipe)?.setOnClickListener {
            toggleEditOffIfNeeded()
            isAddingSwipe = true
            areaDrawOverlay?.isInSwipeMode = true
            areaDrawOverlay?.swipeTipText = "请点击起点位置"
        }

        updateStartStopButton(script.isRunning)
    }

    fun refreshDraw() {
        areaDrawOverlay?.setRealScreenSize(screenRealWidth, screenRealHeight)
        areaDrawOverlay?.markList = markAreaList
        areaDrawOverlay?.invalidate()
    }

    fun showSettingsBottomSheet() { settingBtnFunction?.showSettingsBottomSheet() }
    fun closeFloatingWindow() { closeBtnFunction?.destroy() }
    fun saveState() { saveBtnFunction?.saveStateToLocal() }
    fun restoreState() {
        saveBtnFunction?.restoreStateFromLocal()
        showFullOverlay()
        refreshDraw()
        setOverlayPenetrate(true)
    }
    fun clearSavedState() { saveBtnFunction?.clearSavedState() }
    fun removeLastAction() { removeBtnFunction?.removeLastAction() }

    fun show() {
        if (floatingView != null && floatingView?.parent == null) {
            restoreState()
            areaDrawOverlay?.forceResetAllModes()
            windowManager?.addView(floatingView, layoutParams)
        }
    }

    fun dismiss() {
        Handler(Looper.getMainLooper()).post {
            if (floatingView != null && floatingView?.parent != null) {
                try {
                    windowManager?.removeView(floatingView)
                    Log.d("FloatingWindow", "悬浮窗已成功隐藏")
                } catch (e: Exception) {
                    Log.e("FloatingWindow", "Failed to dismiss floatingView", e)
                }
            }
        }
    }

    fun destroy() {
        try {
            stopScript()
            floatingView?.let { if (it.parent != null) windowManager?.removeViewImmediate(it) }
            areaDrawOverlay?.let { if (it.parent != null) windowManager?.removeViewImmediate(it) }

            floatingView = null
            areaDrawOverlay = null
            windowManager = null
            layoutParams = null
            overlayParams = null

            scriptScheduler = null
            startStopBtn = null
            btnEdit = null
            btnClean = null

            startBtnFunction = null
            stopBtnFunction = null
            addClickBtnFunction = null
            removeBtnFunction = null
            addSwipeBtnFunction = null
            saveBtnFunction = null
            settingBtnFunction = null
            closeBtnFunction = null

            markAreaList.clear()
            isEditMode = false
            isAddingSwipe = false

            Log.d("FloatingWindow", "✅ 悬浮窗已彻底销毁")
        } catch (e: Exception) {
            Log.e("FloatingWindow", "❌ 销毁失败", e)
        }
    }

    private fun MarkDrawView.setRealScreenSize(width: Int, height: Int) {}
}