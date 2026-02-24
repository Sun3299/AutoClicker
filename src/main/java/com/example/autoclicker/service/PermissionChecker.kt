package com.example.autoclicker.service

import android.content.Context

object PermissionChecker {

    /**
     * 检查所有必要权限（无障碍+悬浮窗）
     * @param context 上下文
     * @param onAllGranted 所有权限通过后的回调
     */
    fun checkAllPermissions(context: Context, onAllGranted: () -> Unit) {
        // 1. 检查无障碍服务
        if (!AutoClickAccessibilityService.isAccessibilityServiceEnabled(context)) {
            AutoClickAccessibilityService.jumpToAccessibilitySetting(context)
            return
        }

        // 2. 检查悬浮窗权限
        if (!AutoClickAccessibilityService.isOverlayPermissionGranted(context)) {
            AutoClickAccessibilityService.jumpToOverlaySetting(context)
            return
        }

        // 3. 所有权限通过
        onAllGranted()
    }
}