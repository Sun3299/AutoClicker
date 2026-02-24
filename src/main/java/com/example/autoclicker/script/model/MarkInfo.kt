package com.example.autoclicker.script.model

import android.view.View
import android.view.WindowManager

/**
 * 标记项信息模型（支撑绘图和View管理）
 * 存储标记View、布局参数、序号、关联的区域矩形
 */
data class MarkInfo(
    val markView: View? = null,          // 标记View（绘图/交互用）
    val layoutParams: WindowManager.LayoutParams? = null, // 标记布局参数
    val position: Int = 0,               // 标记序号（绘图显示用）
    val rect: android.graphics.Rect,     // 标记对应的区域矩形（绘图核心）
    val markSize: Int = 80               // 标记View尺寸（默认80px）
)