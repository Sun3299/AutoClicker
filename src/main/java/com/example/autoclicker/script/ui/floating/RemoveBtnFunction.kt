package com.example.autoclicker.script.ui.floating
import android.util.Log
import com.example.autoclicker.script.model.Script

class RemoveBtnFunction(
    private val script: Script,
    private val markAreaList: MutableList<MarkAreaItem>,
    private val refreshDraw: () -> Unit
) {
    fun removeLastAction() {
        if (markAreaList.isEmpty() || script.actions.isEmpty()) {
            Log.d("RemoveBtn", "没有可删除的动作")
            return
        }

        // 1. 删除最后一个界面标记
        markAreaList.removeAt(markAreaList.size - 1)

        // 2. 删除脚本里对应的最后一个动作（用 removeAt 替代 removeLast）
        if (script.actions.isNotEmpty()) {
            script.actions.removeAt(script.actions.size - 1)
        }

        // 3. 刷新界面
        refreshDraw()

        Log.d("RemoveBtn", "已删除最后一个动作 & 界面标记")
    }
}