package com.example.autoclicker.script.model

/**
 * 运行时策略（描述自动点击器的停止规则）
 * @property mode 具体的运行模式
 */
data class RuntimeStrategy(
    val mode: RunMode = RunMode.NeverStop
) {
    /**
     * 转换为UI友好的显示文本
     */
    fun toDisplayText(): String = when (mode) {
        is RunMode.NeverStop -> "永不停止"
        is RunMode.ByDuration -> "按时长停止：${mode.hour}小时${mode.minute}分钟${mode.second}秒"
        is RunMode.ByCount -> "按次数停止：${mode.times}次"
    }

    /**
     * 静态构建工具
     */
    companion object {
        /**
         * 从UI选择结果构建运行策略
         * @param isNeverStop 是否选择"永不停止"
         * @param isByDuration 是否选择"按时长停止"（否则为按次数）
         * @param hour 时长-小时（默认0）
         * @param minute 时长-分钟（默认0）
         * @param second 时长-秒（默认0）
         * @param count 次数（默认10）
         * @return 构建好的运行策略
         */
        fun fromUiSelection(
            isNeverStop: Boolean,
            isByDuration: Boolean,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            count: Int = 10
        ): RuntimeStrategy {
            val runMode = when {
                isNeverStop -> RunMode.NeverStop
                isByDuration -> RunMode.ByDuration(hour, minute, second)
                else -> RunMode.ByCount(count)
            }
            return RuntimeStrategy(runMode)
        }
    }
}

/**
 * 运行模式密封类（定义所有支持的停止规则类型）
 */
sealed class RunMode {
    /** 永不停止模式 */
    object NeverStop : RunMode()

    /**
     * 按时长停止模式
     * @param hour 小时数（默认0）
     * @param minute 分钟数（默认0）
     * @param second 秒数（默认0）
     */
    data class ByDuration(
        val hour: Int = 0,
        val minute: Int = 0,
        val second: Int = 0
    ) : RunMode() {
        /**
         * 转换为总毫秒数（便于执行器计算结束时间）
         * @return 时长对应的总毫秒数
         */
        fun toTotalMilliseconds(): Long =
            (hour * 3600L + minute * 60L + second) * 1000L
    }

    /**
     * 按次数停止模式
     * @param times 运行次数（默认10）
     */
    data class ByCount(val times: Int = 10) : RunMode()
}