package com.example.autoclicker.script.executor

import com.example.autoclicker.script.model.Script
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.script.model.TimeUnit

/**
 * 独立计算类：负责所有ScriptAction的参数验证、原始值保存、执行值计算
 * 与UI/弹窗完全解耦
 */
class ScriptActionCalculator private constructor() {

    companion object {
        // 单例（全局唯一，专注计算逻辑）
        val instance: ScriptActionCalculator by lazy { ScriptActionCalculator() }

        /**
         * 核心方法：处理动作参数（验证→保存原始值→计算执行值）
         */
        fun processActionParams(
            script: Script,
            actionIndex: Int,
            rawDelayMin: Long,
            rawDelayMax: Long,
            rawDurationMin: Long,
            rawDurationMax: Long,
            rawDelayUnit: TimeUnit,
            rawDurationUnit: TimeUnit
        ): Boolean {
            // 1. 参数验证（独立逻辑）
            if (!validateParams(rawDelayMin, rawDelayMax, rawDurationMin, rawDurationMax)) {
                return false
            }

            // 2. 保存原始值 + 计算执行值（毫秒）
            updateScriptAction(
                script = script,
                actionIndex = actionIndex,
                rawDelayMin = rawDelayMin,
                rawDelayMax = rawDelayMax,
                rawDurationMin = rawDurationMin,
                rawDurationMax = rawDurationMax,
                rawDelayUnit = rawDelayUnit,
                rawDurationUnit = rawDurationUnit
            )

            return true
        }

        /**
         * 参数验证：确保数值合法
         */
        private fun validateParams(
            rawDelayMin: Long,
            rawDelayMax: Long,
            rawDurationMin: Long,
            rawDurationMax: Long
        ): Boolean {
            // 延迟值验证：≥0 且 最小值≤最大值
            if (rawDelayMin < 0 || rawDelayMax < 0 || rawDelayMin > rawDelayMax) {
                return false
            }
            // 时长值验证：≥0 且 最小值≤最大值（点击动作时长默认0，不影响）
            if (rawDurationMin < 0 || rawDurationMax < 0 || rawDurationMin > rawDurationMax) {
                return false
            }
            return true
        }

        /**
         * 核心逻辑：1.保存用户输入的原始值+单位  2.计算用于执行的毫秒值
         */
        private fun updateScriptAction(
            script: Script,
            actionIndex: Int,
            rawDelayMin: Long,
            rawDelayMax: Long,
            rawDurationMin: Long,
            rawDurationMax: Long,
            rawDelayUnit: TimeUnit,
            rawDurationUnit: TimeUnit
        ) {
            val scriptAction = script.actions.getOrNull(actionIndex) ?: return

            when (scriptAction) {
                is ScriptAction.Click -> {
                    // ========== 1. 保存用户输入的原始值+单位（用于弹窗展示） ==========
                    scriptAction.rawDelayMin = rawDelayMin
                    scriptAction.rawDelayMax = rawDelayMax
                    scriptAction.rawDelayUnit = rawDelayUnit

                    // ========== 2. 计算用于执行的毫秒值（仅用于运行，不影响UI） ==========
                    scriptAction.delayMinMs = convertToMs(rawDelayMin, rawDelayUnit)
                    scriptAction.delayMaxMs = convertToMs(rawDelayMax, rawDelayUnit)
                }

                is ScriptAction.Swipe -> {
                    // ========== 1. 保存用户输入的原始值+单位（用于弹窗展示） ==========
                    scriptAction.rawDelayMin = rawDelayMin
                    scriptAction.rawDelayMax = rawDelayMax
                    scriptAction.rawDelayUnit = rawDelayUnit
                    scriptAction.rawDurationMin = rawDurationMin
                    scriptAction.rawDurationMax = rawDurationMax
                    scriptAction.rawDurationUnit = rawDurationUnit

                    // ========== 2. 计算用于执行的毫秒值（仅用于运行，不影响UI） ==========
                    scriptAction.delayMinMs = convertToMs(rawDelayMin, rawDelayUnit)
                    scriptAction.delayMaxMs = convertToMs(rawDelayMax, rawDelayUnit)
                    scriptAction.durationMinMs = convertToMs(rawDurationMin, rawDurationUnit)
                    scriptAction.durationMaxMs = convertToMs(rawDurationMax, rawDurationUnit)
                }
            }
        }

        /**
         * 单位转换：将原始值+单位 转为 毫秒（仅用于执行，不影响UI展示）
         */
        private fun convertToMs(value: Long, unit: TimeUnit): Long {
            return when (unit) {
                is TimeUnit.Millisecond -> value
                is TimeUnit.Second -> value * 1000
                is TimeUnit.Minute -> value * 60 * 1000
            }
        }
    }
}