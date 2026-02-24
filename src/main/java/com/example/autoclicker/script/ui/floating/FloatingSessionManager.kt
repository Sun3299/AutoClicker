package com.example.autoclicker.script.ui.floating

import android.content.Context
import com.example.autoclicker.script.model.Script

class FloatingSessionManager private constructor(private val appContext: Context) {
    // 存储 脚本ID → 悬浮窗管理器 映射（单例独占模式：集合中最多一个元素）
    private val activeFloatingWindows = mutableMapOf<Script, FloatingWindowManager>()

    companion object {
        @Volatile
        private var INSTANCE: FloatingSessionManager? = null

        /**
         * 获取单例实例（强制使用ApplicationContext，避免Activity泄漏）
         */
        fun getInstance(context: Context): FloatingSessionManager {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                INSTANCE ?: FloatingSessionManager(appContext).also { INSTANCE = it }
            }
        }

        /**
         * 清空单例（应用退出时调用，避免内存泄漏）
         */
        fun clearInstance() {
            INSTANCE?.apply {
                activeFloatingWindows.values.forEach { it.destroy() }
                activeFloatingWindows.clear()
            }
            INSTANCE = null
        }
    }

    /**
     * 切换到指定脚本的悬浮窗（单例独占：先销毁其他，再创建/复用当前）
     * @param scriptId 脚本唯一标识
     */
    fun switchToScript(script: Script) {
        // 🔥 先把旧的全部销毁，再建新的！顺序不能乱
        val oldWindows = ArrayList(activeFloatingWindows.values)
        oldWindows.forEach { it.destroy() } // 立刻同步销毁
        activeFloatingWindows.clear()

        // 现在再创建新的
        val floatingWindow = FloatingWindowManager(appContext, script)
        activeFloatingWindows[script] = floatingWindow

        if (!script.isSetting) {
            floatingWindow.showSettingsBottomSheet()
        }

        try {
            floatingWindow.show()
//            floatingWindow.restoreState()
        } catch (e: Exception) {
            e.printStackTrace()
            activeFloatingWindows.remove(script)
            floatingWindow.dismiss()
        }
    }

    /**
     * 获取当前活跃的悬浮窗管理器实例（单例独占模式下最多一个）
     */
    fun getActiveFloatingWindow(): FloatingWindowManager? {
        return activeFloatingWindows.values.firstOrNull()
    }

    /**
     * 根据脚本ID获取对应的悬浮窗管理器实例
     * @param scriptId 脚本唯一标识
     */
    fun getFloatingWindowByScript(script: Script): FloatingWindowManager? {
        return activeFloatingWindows[script]
    }

    /**
     * 销毁指定脚本的悬浮窗会话（核心联动CloseBtnFunction的逻辑）
     * @param scriptId 脚本唯一标识
     */
    fun destroySession(script: Script) {
        // 1. 从映射中取出实例（空安全处理）
        val floatingWindowManager = activeFloatingWindows.remove(script) ?: return

        try {
            // 2. 核心：调用FloatingWindowManager的destroy()，间接触发CloseBtnFunction的关闭逻辑
            floatingWindowManager.destroy()
        } catch (e: Exception) {
            // 捕获异常，避免销毁失败导致流程中断
            e.printStackTrace()
        } finally {
            // 3. 兜底：确保资源释放（即使destroy抛异常）
            floatingWindowManager.dismiss()
        }
    }

    /**
     * 销毁所有悬浮窗会话（比如应用退后台时调用）
     */
    fun destroyAllSessions() {
        activeFloatingWindows.values.forEach {
            try {
                it.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
                it.dismiss()
            }
        }
        activeFloatingWindows.clear()
    }
}