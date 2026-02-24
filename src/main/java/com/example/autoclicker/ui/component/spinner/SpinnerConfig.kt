package com.example.autoclicker.ui.component.spinner

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.example.autoclicker.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * 下拉框工具类：构造方法参数和UiDesign中调用的完全匹配
 * 包含：context、triggerLayout、valueTv、ivSpinnerArrow、defaultText、options
 */
class SpinnerConfig(
    private val context: Context,
    val triggerView: View,
    val valueTextView: TextView,
    val arrowImageView: ImageView, // 这个参数必须和调用时一致
    val defaultText: String,
    val options: List<String>
) {
    private var popupWindow: PopupWindow? = null
    private var isExpanded = false // 标记下拉框是否展开

    // 初始化：设置默认值 + 绑定触发控件的点击事件
    fun init() {
        valueTextView.text = defaultText
        triggerView.apply {
            setOnClickListener {
                if (isExpanded) dismiss() else showPopup()
            }
        }
    }

    // 显示下拉框：加载你定义的xml模板（layout_popup_container + item_popup_option）
    fun showPopup() {
        isExpanded = true
        arrowImageView.rotation = 180f // 箭头向上旋转

        // 1. 加载下拉框容器（你定义的layout_popup_container.xml）
        val popupContainer = LayoutInflater.from(context)
            .inflate(R.layout.layout_popup_container, null) as LinearLayout

        // 2. 循环加载单个选项（你定义的item_popup_option.xml），添加到容器
        for (optionText in options) {
            val singleOption = LayoutInflater.from(context)
                .inflate(R.layout.item_popup_option, null) as TextView
            singleOption.text = optionText
            // 选项点击：更新显示值 + 关闭下拉框
            singleOption.setOnClickListener {
                valueTextView.text = optionText
                dismiss()
            }
            popupContainer.addView(singleOption)
        }

        // 3. 创建PopupWindow，显示在触发控件下方
        popupWindow = PopupWindow(
            popupContainer, // 下拉框内容（你的自定义样式）
            triggerView.width, // 宽度和触发控件一致
            LinearLayout.LayoutParams.WRAP_CONTENT // 高度自适应选项
        ).apply {
            isOutsideTouchable = true // 点击外部关闭下拉框
            isFocusable = true // 解决部分机型点击外部不关闭的问题

            // 核心修改：监听PopupWindow关闭事件，确保箭头复原
            setOnDismissListener {
                resetArrow() // 无论哪种方式关闭，都复原箭头
            }

            showAsDropDown(triggerView, 0, 0, Gravity.START) // 对齐触发控件
        }
    }

    // 关闭下拉框：箭头复位 + 销毁PopupWindow
    fun dismiss() {
        isExpanded = false
        resetArrow() // 复用复位方法
        popupWindow?.dismiss()
    }

    // 单独封装箭头复位逻辑（统一管理）
    private fun resetArrow() {
        arrowImageView.rotation = 0f // 箭头恢复向下
        isExpanded = false // 同步标记状态
    }

    // 对外提供：获取当前选中的下拉值
    fun getCurrentValue(): String = valueTextView.text.toString()
}