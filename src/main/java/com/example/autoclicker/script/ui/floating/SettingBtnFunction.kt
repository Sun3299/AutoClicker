package com.example.autoclicker.script.ui.floating

import android.content.Context
import com.example.autoclicker.script.model.Script
import com.example.autoclicker.ui.dialog.bottomSheet.BottomSheetSettings

class SettingBtnFunction(private val context: Context, private val script: Script) {
    fun showSettingsBottomSheet() {
        val settingsDialog = BottomSheetSettings.getInstanceForScript(context, script)
        settingsDialog.show()
        // 补充你的设置弹窗逻辑
    }
}