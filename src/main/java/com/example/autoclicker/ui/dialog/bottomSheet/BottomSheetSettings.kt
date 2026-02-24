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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.autoclicker.databinding.BottomSheetSettingsBinding
import com.example.autoclicker.databinding.BottomSheetStopSettingsBinding
import com.example.autoclicker.script.model.Script
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.script.model.ScriptSettings
import com.example.autoclicker.script.model.TimeUnit // 导入TimeUnit密封类
import com.example.autoclicker.ui.component.spinner.SpinnerConfig
import com.example.autoclicker.ui.dialog.StopSettingsManager
import com.example.autoclicker.ui.dialog.OnStopSettingConfirmListener
import com.google.android.material.R

class BottomSheetSettings private constructor(
    private val context: Context,
    private val script: Script // 绑定当前操作的Script
) {

    private lateinit var stopSettingsManager: StopSettingsManager

    private val bottomSheetDialog: BottomSheetDialog by lazy { createBottomSheetDialog() }
    private val stopSettingsDialog: BottomSheetDialog by lazy { createStopSettingsDialog() }

    private val binding = BottomSheetSettingsBinding.inflate(LayoutInflater.from(context))

    // 创建弹窗：初始化UI + 绑定点击事件
    private fun createBottomSheetDialog(): BottomSheetDialog {
        return BottomSheetDialog(context).apply {
            setContentView(binding.root)
            //初始化name
            binding.scriptName.text=script.name
            // 下拉框初始化（改用TimeUnit的工具方法，统一管理选项）
            val spinnerConfigs = listOf(
                SpinnerConfig(
                    triggerView = binding.customSpinnerTrigger,
                    valueTextView = binding.tvSpinnerValue,
                    arrowImageView = binding.ivSpinnerArrow,
                    defaultText = TimeUnit.Millisecond.displayName, // 用密封类的默认值
                    options = TimeUnit.getAllDisplayNames(), // 用密封类的选项列表（毫秒/秒/分）
                    context = this.context
                ),
                SpinnerConfig(
                    triggerView = binding.customSpinnerTrigger1,
                    valueTextView = binding.tvSpinnerValue1,
                    arrowImageView = binding.ivSpinnerArrow1,
                    defaultText = TimeUnit.Millisecond.displayName,
                    options = TimeUnit.getAllDisplayNames(),
                    context = this.context
                )
            )
            spinnerConfigs.forEach { it.init() }

            // 确认按钮：读取UI值 → 构造ScriptSettings → 同步到已有ScriptAction/更新默认值
            binding.btnConfirm.setOnClickListener {
                script.isSetting = true
                // 安全调用StopSettingsManager
                if (::stopSettingsManager.isInitialized) {
                    stopSettingsManager.setRuntimeStrategy()
                }

                // 1. 读取UI输入（处理空值/非数字，避免崩溃）
                val nextIntervalMin = binding.etIntervalMin.text.toString().toLongOrNull() ?: 500
                val nextIntervalMax = binding.etIntervalMax.text.toString().toLongOrNull() ?: 500
                val durationIntervalMin = binding.etDurationMin.text.toString().toLongOrNull() ?: 500
                val durationIntervalMax = binding.etDurationMax.text.toString().toLongOrNull() ?: 500

                // 2. 读取Spinner文本 → 转为TimeUnit密封类实例（核心修复：类型匹配）
                val actionUnit = TimeUnit.fromDisplayName(binding.tvSpinnerValue.text.toString())
                val durationUnit = TimeUnit.fromDisplayName(binding.tvSpinnerValue1.text.toString())

                // 3. 构造ScriptSettings实例（修复字段赋值错误）
                val scriptSettings = ScriptSettings(
                    nextIntervalMin = nextIntervalMin,
                    nextIntervalMax = nextIntervalMax,
                    actionUnit = actionUnit, // 传入TimeUnit实例，而非String
                    durationIntervalMin = durationIntervalMin,
                    durationIntervalMax = durationIntervalMax, // 修复：改为durationIntervalMax
                    durationUnit = durationUnit // 传入TimeUnit实例，而非String
                )

                // 4. 将ScriptSettings赋值给Script（持久化存储）
                script.settings = scriptSettings


                dismiss() // 关闭弹窗
            }

            // 取消按钮 + 停止设置弹窗触发
            binding.btnCancel.setOnClickListener { dismiss() }
            binding.customSpinnerTrigger2.setOnClickListener { showStopSettingsDialog() }

            // 弹窗样式配置（原有逻辑不变）
            configBottomSheetStyle(this)
            setupBottomSheetBehavior(this)
        }
    }

    // 直接更新所有已有ScriptAction的配置（参数改为换算后的毫秒值）


    // 停止设置弹窗（原有逻辑不变）
    private fun createStopSettingsDialog(): BottomSheetDialog {
        return BottomSheetDialog(context).apply {
            val stopBinding = BottomSheetStopSettingsBinding.inflate(LayoutInflater.from(context))
            setContentView(stopBinding.root)
            stopSettingsManager=StopSettingsManager(
                context = context,
                dialog = this,
                binding = stopBinding,
                script = script,
                onConfirmListener = object : OnStopSettingConfirmListener {
                    override fun onConfirm(stopText: String) {
                        binding.tvSpinnerValue2.text = stopText
                    }
                }
            )
            stopSettingsManager.init()
            configBottomSheetStyle(this)
            setupBottomSheetBehavior(this)
        }
    }

    // 弹窗样式配置（原有逻辑不变）
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

    private fun configBottomSheetStyle(dialog: BottomSheetDialog) {
        dialog.window?.apply {
            setType(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            })
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            decorView.setPadding(0, 0, 0, 0)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    // 弹窗显示/隐藏方法
    private fun showStopSettingsDialog() = if (!stopSettingsDialog.isShowing) stopSettingsDialog.show() else Unit
    fun show() = if (!bottomSheetDialog.isShowing) bottomSheetDialog.show() else Unit
    fun dismiss() {
        if (bottomSheetDialog.isShowing) bottomSheetDialog.dismiss()
        if (stopSettingsDialog.isShowing) stopSettingsDialog.dismiss()
    }

    // 获取实例（每个Script对应一个弹窗）
    companion object {
        private val scriptDialogMap = mutableMapOf<String, BottomSheetSettings>()

        fun getInstanceForScript(context: Context, script: Script): BottomSheetSettings {
            return scriptDialogMap[script.id] ?: BottomSheetSettings(context.applicationContext, script).also {
                scriptDialogMap[script.id] = it
            }
        }

        fun removeInstance(scriptId: String) {
            scriptDialogMap.remove(scriptId)?.dismiss()
        }
    }
}