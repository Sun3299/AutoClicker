package com.example.autoclicker.ui.dialog

import android.content.Context
import android.util.Log
import android.widget.RadioButton
import com.example.autoclicker.databinding.BottomSheetStopSettingsBinding
import com.example.autoclicker.script.model.RuntimeStrategy
import com.example.autoclicker.script.model.Script
import com.google.android.material.bottomsheet.BottomSheetDialog

// 1. 定义回调接口：用于传递选中的停止规则文本
interface OnStopSettingConfirmListener {
    fun onConfirm(stopText: String)
}

// 改造后：不再自行创建Dialog，接收外部传入的Dialog和Binding
class StopSettingsManager(
    private val context: Context,
    private val dialog: BottomSheetDialog, // 外部传入的Dialog
    private val binding: BottomSheetStopSettingsBinding,
    private val script: Script,
    // 新增：回调参数，接收选中的文本
    private val onConfirmListener: OnStopSettingConfirmListener
) {
    private val TAG = "StopSettingsManager"

    // 初始化逻辑（替代原有的show方法，只处理业务逻辑）
    fun init() {
        Log.d(TAG, "初始化停止设置弹窗逻辑")
        // 绑定RadioButton互斥+输入框状态
        initRadioButtonLogic()
        // 绑定完成按钮逻辑
        initConfirmButton()
    }

    // RadioButton互斥+输入框状态控制（核心逻辑不变）
    private fun initRadioButtonLogic() {
        val radioButtons = listOf(
            binding.rbNeverStop,
            binding.rbStopAfterDuration,
            binding.rbStopAfterRepeat
        )

        radioButtons.forEach { radioButton ->
            radioButton.setOnClickListener {
                Log.d(TAG, "点击RadioButton：${radioButton.text}")
                // 互斥逻辑：取消其他RadioButton选中
                radioButtons.forEach { rb ->
                    if (rb != radioButton) {
                        rb.isChecked = false
                        Log.d(TAG, "取消选中：${rb.text}")
                    }
                }
                // 更新输入框启用状态
                updateInputEnabledState(radioButton)
            }
        }

        // 初始化：默认选中“永不停止”，禁用所有输入框
        updateInputEnabledState(binding.rbNeverStop)
    }

    // 更新输入框启用/禁用状态
    private fun  updateInputEnabledState(selectedRadioButton: RadioButton) {
        val isTimeEnabled = selectedRadioButton == binding.rbStopAfterDuration
        val isCountEnabled = selectedRadioButton == binding.rbStopAfterRepeat

        binding.etHour.isEnabled = isTimeEnabled
        binding.etMinute.isEnabled = isTimeEnabled
        binding.etSecond.isEnabled = isTimeEnabled
        binding.etRepeatCount.isEnabled = isCountEnabled

        Log.d(TAG, "输入框状态更新：时长=$isTimeEnabled，次数=$isCountEnabled")
    }

    // 完成按钮逻辑（点击后关闭弹窗 + 触发回调）
    private fun initConfirmButton() {
        binding.btnConfirm.setOnClickListener {
            // 获取用户选择的结果
            val selectedResult = getSelectedResult()
            Log.d(TAG, "确认选择：$selectedResult")

            // 新增：触发回调，把选中的文本传递给外部
            onConfirmListener.onConfirm(selectedResult)

            // 关闭外部传入的Dialog
            dialog.dismiss()
        }
    }

    // 可选：封装获取选中结果的方法（供外部调用）
    fun getSelectedResult(): String {
        return when {
            binding.rbNeverStop.isChecked -> "永不停止"
            binding.rbStopAfterDuration.isChecked -> {
                val hour = binding.etHour.text.toString().toIntOrNull() ?: 0
                val minute = binding.etMinute.text.toString().toIntOrNull() ?: 0
                val second = binding.etSecond.text.toString().toIntOrNull() ?: 0
                "按时长停止：${hour}小时${minute}分钟${second}秒"
            }
            else -> {
                val count = binding.etRepeatCount.text.toString().toIntOrNull() ?: 10
                "按次数停止：${count}次"
            }
        }
    }
    fun setRuntimeStrategy() {
        when {
            // 1. 选择“永不停止”
            binding.rbNeverStop.isChecked -> {
                script.runtimeStrategy = RuntimeStrategy.fromUiSelection(
                    isNeverStop = true,
                    isByDuration = false
                )
            }

            // 2. 选择“按时长停止”
            binding.rbStopAfterDuration.isChecked -> {
                // 读取输入并做非负校验（避免负数传入）
                val hour = binding.etHour.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
                val minute = binding.etMinute.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0
                val second = binding.etSecond.text.toString().toIntOrNull()?.coerceAtLeast(0) ?: 0

                script.runtimeStrategy = RuntimeStrategy.fromUiSelection(
                    isNeverStop = false,
                    isByDuration = true,
                    hour = hour,
                    minute = minute,
                    second = second
                )
            }

            // 3. 默认选择“按次数停止”
            else -> {
                // 读取输入并确保次数至少为1（避免0/负数）
                val count = binding.etRepeatCount.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 10

                script.runtimeStrategy = RuntimeStrategy.fromUiSelection(
                    isNeverStop = false,
                    isByDuration = false,
                    count = count
                )
            }
        }
    }
}