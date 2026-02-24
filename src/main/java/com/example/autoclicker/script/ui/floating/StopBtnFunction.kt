package com.example.autoclicker.script.ui.floating

import android.content.Context
import com.example.autoclicker.script.model.Script

class StopBtnFunction(private val context: Context, private val markAreaList: MutableList<MarkAreaItem>) {
    fun performStop(script: Script) {
        script.isRunning = false
    }
}