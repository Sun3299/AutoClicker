package com.example.autoclicker.script.model

import android.graphics.Rect

sealed class ScriptAction(
    open val timestamp: Long = System.currentTimeMillis(),
    open val markInfo: MarkInfo? = null
) {

    data class Click(
        var clickRect: Rect,
        // --- 用于执行的毫秒值（计算后） ---
        var delayMinMs: Long = 0,
        var delayMaxMs: Long = 500,
        // --- 用于UI展示的原始值和单位（用户输入） ---
        var rawDelayMin: Long = 0,
        var rawDelayMax: Long = 500,
        var rawDelayUnit: TimeUnit = TimeUnit.Millisecond,

        override val timestamp: Long = System.currentTimeMillis(),
        override val markInfo: MarkInfo? = null
    ) : ScriptAction(timestamp, markInfo) {
        constructor(
            x: Int, y: Int,
            delayMinMs: Long = 0, delayMaxMs: Long = 500,
            timestamp: Long = System.currentTimeMillis(),
            markInfo: MarkInfo? = null
        ) : this(
            clickRect = Rect(x, y, x, y),
            delayMinMs = delayMinMs, delayMaxMs = delayMaxMs,
            timestamp = timestamp, markInfo = markInfo
        )
    }

    data class Swipe(
        var startX: Int,
        var startY: Int,
        var endX: Int,
        var endY: Int,
        // --- 用于执行的毫秒值（计算后） ---
        var durationMinMs: Long = 200,
        var durationMaxMs: Long = 500,
        var delayMinMs: Long = 0,
        var delayMaxMs: Long = 500,
        // --- 用于UI展示的原始值和单位（用户输入） ---
        var rawDelayMin: Long = 0,
        var rawDelayMax: Long = 500,
        var rawDelayUnit: TimeUnit = TimeUnit.Millisecond,
        var rawDurationMin: Long = 200,
        var rawDurationMax: Long = 500,
        var rawDurationUnit: TimeUnit = TimeUnit.Millisecond,

        override val timestamp: Long = System.currentTimeMillis(),
        override val markInfo: MarkInfo? = null
    ) : ScriptAction(timestamp, markInfo)
}

