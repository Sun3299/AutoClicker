package com.example.autoclicker.script.model

/**
 * 时间单位密封类（独立结构体，封装毫秒/秒/分的展示名和换算逻辑）
 * @property displayName UI展示用的单位名称（如“毫秒”“秒”）
 */
sealed class TimeUnit(val displayName: String) {
    // 毫秒单位
    object Millisecond : TimeUnit("毫秒") {
        override fun toMs(value: Long): Long = value * 1L
    }

    // 秒单位
    object Second : TimeUnit("秒") {
        override fun toMs(value: Long): Long = value * 1000L
    }

    // 分单位
    object Minute : TimeUnit("分") {
        override fun toMs(value: Long): Long = value * 60 * 1000L
    }

    // 抽象方法：将数值转换为毫秒（核心换算逻辑）
    abstract fun toMs(value: Long): Long

    // 伴生对象：提供工具方法（根据展示名反向查找单位）
    companion object {
        /**
         * 根据UI上的单位文本（如“秒”）获取对应的TimeUnit实例
         * @param displayName UI展示的单位文本
         * @return 匹配的TimeUnit，默认返回Millisecond
         */
        fun fromDisplayName(displayName: String): TimeUnit {
            return when (displayName.trim()) {
                "秒" -> Second
                "分" -> Minute
                else -> Millisecond // 默认毫秒
            }
        }

        /**
         * 获取所有支持的单位展示名（用于Spinner初始化选项）
         */
        fun getAllDisplayNames(): List<String> {
            return listOf(Millisecond.displayName, Second.displayName, Minute.displayName)
        }
    }
}

/**
 * 脚本设置数据类（仅两套单位：Action间隔/ Duration时长）
 * @param nextIntervalMin 下个动作间隔最小值（原始数值）
 * @param nextIntervalMax 下个动作间隔最大值（原始数值）
 * @param actionUnit Action间隔的单位（第一套：对应第一个Spinner）
 * @param durationIntervalMin 滑动时长最小值（原始数值）
 * @param durationIntervalMax 滑动时长最大值（原始数值）
 * @param durationUnit Duration时长的单位（第二套：对应第二个Spinner）
 */
data class ScriptSettings (
    // ========== 第一套单位：Action间隔（min/max共用一个单位） ==========
    val nextIntervalMin: Long = 50,
    val nextIntervalMax: Long = 50,
    val actionUnit: TimeUnit = TimeUnit.Millisecond, // 间隔的统一单位

    // ========== 第二套单位：Duration时长（min/max共用一个单位） ==========
    val durationIntervalMin: Long = 50,
    val durationIntervalMax: Long = 50,
    val durationUnit: TimeUnit = TimeUnit.Millisecond // 时长的统一单位
) {
    // ==================== Action间隔换算（基于第一套单位） ====================
    /** 获取下个动作间隔最小值（毫秒） */
    fun getNextIntervalMinMs(): Long = actionUnit.toMs(nextIntervalMin)

    /** 获取下个动作间隔最大值（毫秒） */
    fun getNextIntervalMaxMs(): Long = actionUnit.toMs(nextIntervalMax)

    // ==================== Duration时长换算（基于第二套单位） ====================
    /** 获取滑动时长最小值（毫秒） */
    fun getDurationIntervalMinMs(): Long = durationUnit.toMs(durationIntervalMin)

    /** 获取滑动时长最大值（毫秒） */
    fun getDurationIntervalMaxMs(): Long = durationUnit.toMs(durationIntervalMax)

    // 校验数值合法性（非负 + 最小值≤最大值）
    fun isLegal(): Boolean {
        return nextIntervalMin >= 0 &&
                nextIntervalMax >= 0 &&
                durationIntervalMin >= 0 &&
                durationIntervalMax >= 0 &&
                nextIntervalMin <= nextIntervalMax &&
                durationIntervalMin <= durationIntervalMax
    }
}