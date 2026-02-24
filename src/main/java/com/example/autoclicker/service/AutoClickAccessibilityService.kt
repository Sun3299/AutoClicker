package com.example.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * 极简版权限检查工具：仅包含
 * 1. 无障碍服务状态检查 + 跳转设置
 * 2. 悬浮窗权限状态检查 + 跳转设置
 */
class AutoClickAccessibilityService : AccessibilityService() {
    private val TAG = "PermissionCheck"

    // 单例实例（仅用于服务生命周期标记，非必需）
    companion object {
        var instance: AutoClickAccessibilityService? = null

        // ====================== 无障碍服务相关 ======================
        /**
         * 检查无障碍服务是否开启
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val am = context.getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val targetServiceName = "${AutoClickAccessibilityService::class.java.name}"
            enabledServices.forEach { serviceInfo ->
                val serviceId = serviceInfo.id ?: return@forEach
                if (serviceId.replace("/","") == targetServiceName) {
                    return true
                }
            }
            return false
        }

        /**
         * 跳转到无障碍服务设置页面
         */
        fun jumpToAccessibilitySetting(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        // ====================== 悬浮窗权限相关 ======================
        /**
         * 检查悬浮窗权限是否开启（Android 6.0+需要）
         */
        fun isOverlayPermissionGranted(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Settings.canDrawOverlays(context)
            }
            // 6.0以下默认返回true（无需动态申请）
            return true
        }

        /**
         * 跳转到悬浮窗权限设置页面
         */
        fun jumpToOverlaySetting(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        }
    }

    // 无障碍服务必须的生命周期方法（仅空实现，保留规范）
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "无障碍服务已连接")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "无障碍服务已销毁")
    }

    // 必须重写的空实现（无障碍服务规范要求）
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }
}