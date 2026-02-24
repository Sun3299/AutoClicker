package com.example.autoclicker.script.ui.floating


import android.content.Context
import com.example.autoclicker.script.model.Script

class StartBtnFunction(
    private val context: Context,
    private val markAreaList: MutableList<MarkAreaItem>,
    private val onStart: () -> Unit
) {
    fun performStart(script: Script) {
        script.isRunning = true
        onStart.invoke()
    }
}