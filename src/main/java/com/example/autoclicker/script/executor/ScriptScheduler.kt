package com.example.autoclicker.script.executor
import android.os.Handler
import android.os.Looper
import com.example.autoclicker.script.model.Script
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.script.model.RuntimeStrategy
import com.example.autoclicker.script.model.RunMode
import com.example.autoclicker.script.ui.floating.MarkDrawView
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 脚本调度器：核心职责 - 调度动作执行顺序、控制随机延迟、管理运行状态、执行停止策略
 */
class ScriptScheduler(
    private val script: Script,
    private val drawView: MarkDrawView,
    // 直接使用Script的runtimeStrategy（非空，无需默认值）
    private val runtimeStrategy: RuntimeStrategy = script.runtimeStrategy
) {
    // 线程安全的运行状态标记
    private val isRunning = AtomicBoolean(false)
    // 主线程Handler（用于延迟调度，避免子线程操作UI）
    private val mainHandler = Handler(Looper.getMainLooper())
    // 随机数生成器（避免重复创建）
    private val random = Random()
    // 当前执行的动作索引
    private var currentActionIndex = 0

    // 运行状态记录
    private var startTime: Long = 0L // 脚本启动时间（毫秒）
    private var totalExecuteCount: Int = 0 // 脚本总执行轮数（一轮所有动作为1次）

    /**
     * 启动脚本执行
     */
    fun start() {
        if (script.actions.isEmpty()) {
            // 无动作时直接返回
            return
        }
        // 重置运行状态
        isRunning.set(true)
        startTime = System.currentTimeMillis() // 记录启动时间
        totalExecuteCount = 0 // 重置执行次数
        currentActionIndex = 0 // 重置动作索引
        // 立即执行第一个动作
        executeNextAction()
    }

    /**
     * 停止脚本执行（安全停止，无残留任务）
     */
    fun stop() {
        isRunning.set(false)
        // 清空所有待执行的延迟任务
        mainHandler.removeCallbacksAndMessages(null)
        // 重置所有状态
        currentActionIndex = 0
        totalExecuteCount = 0
        startTime = 0L
        // 隐藏所有可视化反馈
        mainHandler.post {
            drawView.hideClickPoint()
        }
    }

    /**
     * 执行下一个动作（核心调度逻辑，包含停止策略检查）
     */
    private fun executeNextAction() {
        // 1. 前置检查：非运行中则终止
        if (!isRunning.get()) {
            return
        }

        // 2. 检查停止策略：触发则停止脚本
        if (checkStopCondition()) {
            stop()
            return
        }

        // 3. 获取当前要执行的动作
        val currentAction = script.actions[currentActionIndex]

        // 4. 执行动作（根据类型分发到对应执行器）
        when (currentAction) {
            is ScriptAction.Click -> ClickExecutor.execute(currentAction, drawView)
            is ScriptAction.Swipe -> SwipeExecutor.execute(currentAction, drawView)
        }

        // 5. 更新动作索引（循环执行）
        currentActionIndex = (currentActionIndex + 1) % script.actions.size

        // 6. 记录执行次数（完成一轮所有动作后，次数+1）
        if (currentActionIndex == 0) {
            totalExecuteCount++
        }

        // 7. 计算下一个动作的随机延迟
        val nextDelay = getRandomDelay(currentAction)

        // 8. 延迟执行下一个动作
        mainHandler.postDelayed({
            executeNextAction()
        }, nextDelay)
    }

    /**
     * 检查是否触发停止条件（核心：根据RuntimeStrategy判断）
     */
    private fun checkStopCondition(): Boolean {
        return when (val mode = runtimeStrategy.mode) {
            // 1. 永不停止：不触发停止
            is RunMode.NeverStop -> false

            // 2. 按时长停止：当前时间 - 启动时间 ≥ 设定时长
            is RunMode.ByDuration -> {
                val totalDuration = mode.toTotalMilliseconds()
                val elapsedTime = System.currentTimeMillis() - startTime
                elapsedTime >= totalDuration
            }

            // 3. 按次数停止：已执行轮数 ≥ 设定次数
            is RunMode.ByCount -> {
                totalExecuteCount >= mode.times
            }
        }
    }

    /**
     * 获取动作的随机延迟时间（严格控制在 [delayMinMs, delayMaxMs] 闭区间）
     */
    private fun getRandomDelay(action: ScriptAction): Long {
        // 统一处理，避免重复代码
        val min = when (action) {
            is ScriptAction.Click -> action.delayMinMs
            is ScriptAction.Swipe -> action.delayMinMs
        }
        val max = when (action) {
            is ScriptAction.Click -> action.delayMaxMs
            is ScriptAction.Swipe -> action.delayMaxMs
        }

        // 确保最小值非负（避免延迟为负数）
        val safeMin = min.coerceAtLeast(0L)
        // 确保最大值不小于最小值
        val safeMax = max.coerceAtLeast(safeMin)

        // 调用严格的随机数生成方法
        return nextLong(random, safeMin, safeMax)
    }

    /**
     * 核心优化：严格生成 [min, max] 闭区间内的随机数
     * 100% 保证在区间内，且非负，无越界
     */
    private fun nextLong(random: Random, min: Long, max: Long): Long {
        // 如果最小值等于最大值，直接返回（无随机必要）
        if (min == max) return min
        // 计算闭区间的元素个数
        val range = max - min + 1
        // 生成 [0, range) 的非负随机数，再映射到 [min, max]
        return min + Math.abs(random.nextLong() % range)
    }

    /**
     * 获取当前运行状态
     */
    fun isScriptRunning(): Boolean = isRunning.get()
}