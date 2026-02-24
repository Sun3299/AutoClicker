package com.example.autoclicker.script.scheduler

import com.example.autoclicker.script.model.Script
import com.example.autoclicker.script.model.ScriptAction

/**
 * 动作任务（统一封装点击/滑动任务）
 * 用于调度器按顺序执行
 */
sealed class ActionTask(
    open val taskId: String, // 任务唯一ID
    open val delayBeforeExecute: Long = 0 // 执行前延迟（毫秒）
) {
    // 点击任务：关联点击区域索引
    data class ClickTask(
        override val taskId: String,
        val areaIndex: Int, // 对应script.clickAreas的索引
        override val delayBeforeExecute: Long = 0
    ) : ActionTask(taskId, delayBeforeExecute)

    // 滑动任务：关联滑动区域索引
    data class SwipeTask(
        override val taskId: String,
        val areaIndex: Int, // 对应script.swipeAreas的索引
        override val delayBeforeExecute: Long = 0
    ) : ActionTask(taskId, delayBeforeExecute)
}