package com.example.autoclicker.ui.dialog.bottomSheet

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.autoclicker.databinding.BottomSheetActionBinding
import com.example.autoclicker.script.model.Script
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.script.model.TimeUnit
import com.example.autoclicker.script.ui.floating.MarkAreaItem
import com.example.autoclicker.ui.component.spinner.SpinnerConfig
import com.google.android.material.R

/**
 * 动作编辑弹窗：仅负责读取/显示用户输入的原始值+单位，不做任何转换
 * 所有动作（点击/滑动）共用一个弹窗，不隐藏任何控件
 */
class ActionEditFloatWindow(
    private val context: Context,
    private val script: Script,
    private val markItem: MarkAreaItem, // 同编号的圆/矩形共用
    private val onParamsReady: (
        // 仅传递用户输入的原始参数，不做任何处理
        actionIndex: Int,
        rawDelayMin: Long,
        rawDelayMax: Long,
        rawDurationMin: Long,
        rawDurationMax: Long,
        rawDelayUnit: TimeUnit,
        rawDurationUnit: TimeUnit
    ) -> Unit // 回调仅通知原始参数就绪
) {
    // 视图绑定（所有动作共用同一个布局，不隐藏任何控件）
    private val binding = BottomSheetActionBinding.inflate(LayoutInflater.from(context))
    private val bottomSheetDialog: BottomSheetDialog by lazy { createBottomSheetDialog() }

    /**
     * 创建弹窗（所有动作共用样式，不隐藏任何控件）
     */
    private fun createBottomSheetDialog(): BottomSheetDialog {
        return BottomSheetDialog(context).apply {
            setContentView(binding.root)

            // 1. 设置标题：动作 n（同编号的圆/矩形共用）
            binding.tvActionId.text = "动作 ${markItem.actionNumber}"

            // 2. 初始化UI数据（仅读取原始值和原始单位，不做任何转换）
            initActionData()

            // 3. 初始化下拉框（所有动作都显示，不隐藏）
            initSpinners()

            // 4. 绑定按钮事件（仅传递原始参数，不做任何计算/修改）
            bindButtonEvents()

            // 5. 复用全局样式配置
            configBottomSheetStyle(this)
            setupBottomSheetBehavior(this)
        }
    }

    /**
     * 初始化UI数据：仅读取原始值和原始单位，完全保留用户上次输入的内容
     */
    private fun initActionData() {
        val scriptAction = script.actions.getOrNull(markItem.actionIndex) ?: return

        when (scriptAction) {
            is ScriptAction.Click -> {
                // 读取点击动作的原始延迟值
                binding.etIntervalMin.setText(scriptAction.rawDelayMin.toString())
                binding.etIntervalMax.setText(scriptAction.rawDelayMax.toString())
                // 读取点击动作的原始延迟单位
                binding.tvIntervalUnit.text = scriptAction.rawDelayUnit.displayName

                // 点击动作时长默认显示0，单位默认毫秒（不隐藏控件）
                binding.etDurationMin.setText("0")
                binding.etDurationMax.setText("0")
                binding.tvDurationUnit.text = TimeUnit.Millisecond.displayName
            }

            is ScriptAction.Swipe -> {
                // 读取滑动动作的原始延迟值
                binding.etIntervalMin.setText(scriptAction.rawDelayMin.toString())
                binding.etIntervalMax.setText(scriptAction.rawDelayMax.toString())
                // 读取滑动动作的原始延迟单位
                binding.tvIntervalUnit.text = scriptAction.rawDelayUnit.displayName

                // 读取滑动动作的原始时长值
                binding.etDurationMin.setText(scriptAction.rawDurationMin.toString())
                binding.etDurationMax.setText(scriptAction.rawDurationMax.toString())
                // 读取滑动动作的原始时长单位
                binding.tvDurationUnit.text = scriptAction.rawDurationUnit.displayName
            }
        }
    }

    /**
     * 初始化下拉框：所有动作都显示，不隐藏任何下拉框
     */
    private fun initSpinners() {
        // 间隔单位下拉框（所有动作都显示）
        val intervalSpinner = SpinnerConfig(
            triggerView = binding.llIntervalUnit,
            valueTextView = binding.tvIntervalUnit,
            arrowImageView = binding.ivIntervalArrow,
            defaultText = binding.tvIntervalUnit.text.toString(), // 用已保存的原始单位作为默认值
            options = TimeUnit.getAllDisplayNames(),
            context = context
        )

        // 时长单位下拉框（所有动作都显示，不隐藏）
        val durationSpinner = SpinnerConfig(
            triggerView = binding.llDurationUnit,
            valueTextView = binding.tvDurationUnit,
            arrowImageView = binding.ivDurationArrow,
            defaultText = binding.tvDurationUnit.text.toString(), // 用已保存的原始单位作为默认值
            options = TimeUnit.getAllDisplayNames(),
            context = context
        )

        intervalSpinner.init()
        durationSpinner.init()
    }

    /**
     * 绑定按钮事件：仅读取原始输入并传递，不做任何计算/转换/修改
     */
    private fun bindButtonEvents() {
        // 取消按钮：仅关闭弹窗，无任何逻辑
        binding.btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // 确认按钮：仅读取原始输入值+单位，传递给外部，不做任何处理
        binding.btnConfirm.setOnClickListener {
            try {
                // 1. 读取原始输入值（仅做非空判断，默认0，不做任何转换）
                val rawDelayMin = binding.etIntervalMin.text.toString().toLongOrNull() ?: 0
                val rawDelayMax = binding.etIntervalMax.text.toString().toLongOrNull() ?: 0
                val rawDurationMin = binding.etDurationMin.text.toString().toLongOrNull() ?: 0
                val rawDurationMax = binding.etDurationMax.text.toString().toLongOrNull() ?: 0

                // 2. 读取原始单位（仅转换类型，不做任何计算）
                val rawDelayUnit = TimeUnit.fromDisplayName(binding.tvIntervalUnit.text.toString())
                val rawDurationUnit = TimeUnit.fromDisplayName(binding.tvDurationUnit.text.toString())

                // 3. 传递原始参数给外部回调，无任何修改
                onParamsReady.invoke(
                    markItem.actionIndex,
                    rawDelayMin,
                    rawDelayMax,
                    rawDurationMin,
                    rawDurationMax,
                    rawDelayUnit,
                    rawDurationUnit
                )

                // 4. 提示+关闭弹窗
                Toast.makeText(context, "动作 ${markItem.actionNumber} 参数已提交", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()

            } catch (e: Exception) {
                Toast.makeText(context, "参数读取失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 复用全局BottomSheet样式配置
     */
    private fun configBottomSheetStyle(dialog: BottomSheetDialog) {
        dialog.window?.apply {
            setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            })
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            decorView.setPadding(0, 0, 0, 0)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    /**
     * 配置BottomSheetBehavior，保证弹窗展开样式统一
     */
    private fun setupBottomSheetBehavior(dialog: BottomSheetDialog) {
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            dialog.findViewById<View>(R.id.design_bottom_sheet)
                ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.setOnShowListener {
            dialog.findViewById<View>(R.id.design_bottom_sheet)?.let { bottomSheetView ->
                (bottomSheetView.layoutParams as CoordinatorLayout.LayoutParams).apply {
                    height = CoordinatorLayout.LayoutParams.WRAP_CONTENT
                    width = CoordinatorLayout.LayoutParams.MATCH_PARENT
                }.also { bottomSheetView.layoutParams = it }
                dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    /**
     * 显示弹窗（同编号的圆/矩形共用同一个弹窗）
     */
    fun show() {
        if (!bottomSheetDialog.isShowing) {
            bottomSheetDialog.show()
        }
    }

    /**
     * 关闭弹窗
     */
    fun dismiss() {
        if (bottomSheetDialog.isShowing) {
            bottomSheetDialog.dismiss()
        }
    }
}