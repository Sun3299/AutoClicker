package com.example.autoclicker.script.model

import android.content.Context
import java.lang.ref.WeakReference
import com.example.autoclicker.script.ui.floating.FloatingSessionManager
import com.example.autoclicker.script.ui.floating.FloatingWindowManager

/**
 * 脚本数据模型（仅负责数据存储，移除所有动作添加/标记管理方法）
 * 适配：点击/滑动拆分 + 调度器 + ScriptAction密封类
 */
data class Script(
    val id: String, // 脚本唯一ID
    var name: String, // 脚本名称
    // 动作日志：存储执行过的点击/滑动动作（适配ScriptAction密封类）
    val actions: MutableList<ScriptAction> = mutableListOf(),
    // 运行策略（随机间隔 + 三种运行模式）
    var runtimeStrategy: RuntimeStrategy = RuntimeStrategy(
        mode = RunMode.NeverStop
    ),
    // 正确写法：使用默认构造函数初始化（ScriptSettings有默认值）
    var settings: ScriptSettings = ScriptSettings(),
    var isSetting: Boolean=false,
    // 悬浮窗状态
    var isFloatingWindowShow: Boolean = false,
    // 保留原有字段（兼容历史逻辑）
    var defaultDelayMin: Long? = 500,
    var defaultDelayMax: Long? = 500,
    // 脚本运行状态（调度器/执行器统一管理）
    var isRunning: Boolean = false
) {
    // ========== Context弱引用（避免内存泄漏） ==========
    private var contextRef: WeakReference<Context>? = null

    /**
     * 初始化脚本上下文（建议传入ApplicationContext）
     */
    fun initScriptContext(context: Context) {
        val appContext = context.applicationContext
        this.contextRef = WeakReference(appContext)
    }

    /**
     * 获取上下文（空安全）
     */
    private fun getContext(): Context? {
        return contextRef?.get()
    }

    // ========== 悬浮窗管理（复用原有逻辑） ==========
    /**
     * 显示脚本专属悬浮窗
     */
    fun showFloatingWindow() {
        val context = getContext() ?: return
        FloatingSessionManager.getInstance(context).switchToScript(this)
        isFloatingWindowShow = true
    }

    /**
     * 获取悬浮窗管理器
     */
    fun getFloatingWindowManager(): FloatingWindowManager? {
        val context = getContext() ?: throw IllegalStateException("Script上下文未初始化，请先调用initScriptContext()")
        return FloatingSessionManager.getInstance(context).getFloatingWindowByScript(this)
    }

    // ========== 资源清空（防止内存泄漏） ==========
    /**
     * 清空脚本资源（页面销毁/脚本删除时调用）
     */
    fun clear() {
        // 1. 关闭悬浮窗
        if (isFloatingWindowShow) {
            getFloatingWindowManager()?.closeFloatingWindow()
        }
        // 2. 清空弱引用
        contextRef?.clear()
        contextRef = null
        // 3. 清空所有集合
        actions.clear()

        // 4. 重置状态
        isFloatingWindowShow = false
        isRunning = false
        // 5. 重置运行策略为默认值
        runtimeStrategy = RuntimeStrategy()
    }
}

