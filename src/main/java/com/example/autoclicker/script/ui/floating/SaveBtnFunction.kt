package com.example.autoclicker.script.ui.floating

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.autoclicker.script.model.Script
import com.example.autoclicker.script.model.ScriptAction
import com.example.autoclicker.script.model.ScriptSettings
import com.example.autoclicker.script.model.TimeUnit

/**
 * 保存按钮功能类（纯SP实现，精准适配TimeUnit密封类 + ScriptSettings）
 */
class SaveBtnFunction(
    private val context: Context,
    private val script: Script,
    private val markAreaList: MutableList<MarkAreaItem>,
    private val refreshDraw: () -> Unit
) {
    // SP文件名 + 脚本唯一标识前缀
    private val PREF_NAME = "FloatingWindow_Script_State"
    private fun getScriptPrefix(): String = "script_${script.id}_"

    /**
     * 保存脚本状态到SP（完整适配TimeUnit + ScriptSettings）
     */
    fun saveStateToLocal() {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()

        try {
            // ========== 1. 保存Script基础属性 ==========
            editor.putString("${getScriptPrefix()}name", script.name)
            editor.putBoolean("${getScriptPrefix()}isSetting", script.isSetting)
            editor.putBoolean("${getScriptPrefix()}isFloatingWindowShow", script.isFloatingWindowShow)
            editor.putBoolean("${getScriptPrefix()}isRunning", script.isRunning)
            // 保存默认延迟（空安全）
            script.defaultDelayMin?.let { editor.putLong("${getScriptPrefix()}defaultDelayMin", it) }
            script.defaultDelayMax?.let { editor.putLong("${getScriptPrefix()}defaultDelayMax", it) }

            // ========== 2. 保存ScriptSettings（核心：适配两套TimeUnit） ==========
            val settingsPrefix = "${getScriptPrefix()}settings_"
            // 2.1 保存Action间隔（第一套单位）
            editor.putLong("${settingsPrefix}nextIntervalMin", script.settings.nextIntervalMin)
            editor.putLong("${settingsPrefix}nextIntervalMax", script.settings.nextIntervalMax)
            editor.putString("${settingsPrefix}actionUnit", script.settings.actionUnit.displayName) // 存展示名

            // 2.2 保存Duration时长（第二套单位）
            editor.putLong("${settingsPrefix}durationIntervalMin", script.settings.durationIntervalMin)
            editor.putLong("${settingsPrefix}durationIntervalMax", script.settings.durationIntervalMax)
            editor.putString("${settingsPrefix}durationUnit", script.settings.durationUnit.displayName) // 存展示名

            // ========== 3. 保存ScriptAction列表（适配TimeUnit密封类） ==========
            val actionCount = script.actions.size
            editor.putInt("${getScriptPrefix()}action_count", actionCount)

            for (i in 0 until actionCount) {
                val action = script.actions[i]
                val actionPrefix = "${getScriptPrefix()}action_${i}_"

                // 基础字段
                editor.putLong("${actionPrefix}timestamp", action.timestamp)
                editor.putBoolean("${actionPrefix}hasMarkInfo", action.markInfo != null)

                // 区分动作类型
                editor.putString("${actionPrefix}type", if (action is ScriptAction.Click) "CLICK" else "SWIPE")

                when (action) {
                    is ScriptAction.Click -> {
                        // 保存Click坐标
                        editor.putInt("${actionPrefix}click_left", action.clickRect.left)
                        editor.putInt("${actionPrefix}click_top", action.clickRect.top)
                        editor.putInt("${actionPrefix}click_right", action.clickRect.right)
                        editor.putInt("${actionPrefix}click_bottom", action.clickRect.bottom)

                        // 保存执行用毫秒值
                        editor.putLong("${actionPrefix}delayMinMs", action.delayMinMs)
                        editor.putLong("${actionPrefix}delayMaxMs", action.delayMaxMs)

                        // 保存原始值 + TimeUnit展示名（核心适配）
                        editor.putLong("${actionPrefix}rawDelayMin", action.rawDelayMin)
                        editor.putLong("${actionPrefix}rawDelayMax", action.rawDelayMax)
                        editor.putString("${actionPrefix}rawDelayUnit", action.rawDelayUnit.displayName)
                    }

                    is ScriptAction.Swipe -> {
                        // 保存Swipe坐标
                        editor.putInt("${actionPrefix}startX", action.startX)
                        editor.putInt("${actionPrefix}startY", action.startY)
                        editor.putInt("${actionPrefix}endX", action.endX)
                        editor.putInt("${actionPrefix}endY", action.endY)

                        // 保存执行用毫秒值
                        editor.putLong("${actionPrefix}durationMinMs", action.durationMinMs)
                        editor.putLong("${actionPrefix}durationMaxMs", action.durationMaxMs)
                        editor.putLong("${actionPrefix}delayMinMs", action.delayMinMs)
                        editor.putLong("${actionPrefix}delayMaxMs", action.delayMaxMs)

                        // 保存原始值 + TimeUnit展示名（核心适配）
                        editor.putLong("${actionPrefix}rawDelayMin", action.rawDelayMin)
                        editor.putLong("${actionPrefix}rawDelayMax", action.rawDelayMax)
                        editor.putString("${actionPrefix}rawDelayUnit", action.rawDelayUnit.displayName)

                        editor.putLong("${actionPrefix}rawDurationMin", action.rawDurationMin)
                        editor.putLong("${actionPrefix}rawDurationMax", action.rawDurationMax)
                        editor.putString("${actionPrefix}rawDurationUnit", action.rawDurationUnit.displayName)
                    }
                }
            }

            // ========== 4. 保存MarkAreaItem列表 ==========
            val markCount = markAreaList.size
            editor.putInt("${getScriptPrefix()}mark_count", markCount)

            for (i in 0 until markCount) {
                val mark = markAreaList[i]
                val markPrefix = "${getScriptPrefix()}mark_${i}_"

                editor.putString("${markPrefix}actionType", mark.actionType.name)
                editor.putInt("${markPrefix}actionNumber", mark.actionNumber)
                editor.putInt("${markPrefix}actionIndex", mark.actionIndex)

                // 保存标记矩形
                editor.putInt("${markPrefix}rect_left", mark.rect.left)
                editor.putInt("${markPrefix}rect_top", mark.rect.top)
                editor.putInt("${markPrefix}rect_right", mark.rect.right)
                editor.putInt("${markPrefix}rect_bottom", mark.rect.bottom)

                // 保存滑动终点矩形
                editor.putInt("${markPrefix}swipe_left", mark.swipeEndRect.left)
                editor.putInt("${markPrefix}swipe_top", mark.swipeEndRect.top)
                editor.putInt("${markPrefix}swipe_right", mark.swipeEndRect.right)
                editor.putInt("${markPrefix}swipe_bottom", mark.swipeEndRect.bottom)
            }

            // 提交保存
            editor.apply()
            Toast.makeText(context, "脚本「${script.name}」状态保存成功", Toast.LENGTH_SHORT).show()
            Log.d("SaveBtnFunction", "✅ 脚本${script.id}（${script.name}）已完整保存")
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("SaveBtnFunction", "❌ 保存脚本${script.id}失败", e)
        }
    }

    /**
     * 从SP恢复脚本状态（核心：通过displayName恢复TimeUnit密封类）
     */
    fun restoreStateFromLocal() {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        try {
            // 1. 清空旧数据
            script.actions.clear()
            markAreaList.clear()

            // 2. 恢复脚本基础信息
            sp.getString("${getScriptPrefix()}name", script.name)?.let { script.name = it }
            script.isSetting = sp.getBoolean("${getScriptPrefix()}isSetting", false)
            script.isFloatingWindowShow = sp.getBoolean("${getScriptPrefix()}isFloatingWindowShow", false)
            script.isRunning = sp.getBoolean("${getScriptPrefix()}isRunning", false)

            script.defaultDelayMin = if (sp.contains("${getScriptPrefix()}defaultDelayMin")) {
                sp.getLong("${getScriptPrefix()}defaultDelayMin", 500)
            } else null
            script.defaultDelayMax = if (sp.contains("${getScriptPrefix()}defaultDelayMax")) {
                sp.getLong("${getScriptPrefix()}defaultDelayMax", 500)
            } else null

            // 3. 恢复 ScriptSettings
            val settingsPrefix = "${getScriptPrefix()}settings_"
            val nextIntervalMin = sp.getLong("${settingsPrefix}nextIntervalMin", 50)
            val nextIntervalMax = sp.getLong("${settingsPrefix}nextIntervalMax", 50)
            val actionUnit = TimeUnit.fromDisplayName(sp.getString("${settingsPrefix}actionUnit", "毫秒") ?: "毫秒")

            val durationIntervalMin = sp.getLong("${settingsPrefix}durationIntervalMin", 50)
            val durationIntervalMax = sp.getLong("${settingsPrefix}durationIntervalMax", 50)
            val durationUnit = TimeUnit.fromDisplayName(sp.getString("${settingsPrefix}durationUnit", "毫秒") ?: "毫秒")

            script.settings = ScriptSettings(
                nextIntervalMin = nextIntervalMin,
                nextIntervalMax = nextIntervalMax,
                actionUnit = actionUnit,
                durationIntervalMin = durationIntervalMin,
                durationIntervalMax = durationIntervalMax,
                durationUnit = durationUnit
            )

            // 4. 恢复动作列表
            val actionCount = sp.getInt("${getScriptPrefix()}action_count", 0)
            for (i in 0 until actionCount) {
                val prefix = "${getScriptPrefix()}action_${i}_"
                val type = sp.getString(prefix + "type", "CLICK")
                val timestamp = sp.getLong(prefix + "timestamp", System.currentTimeMillis())

                val action = if (type == "CLICK") {
                    val rect = Rect(
                        sp.getInt(prefix + "click_left", 0),
                        sp.getInt(prefix + "click_top", 0),
                        sp.getInt(prefix + "click_right", 0),
                        sp.getInt(prefix + "click_bottom", 0)
                    )
                    ScriptAction.Click(
                        clickRect = rect,
                        delayMinMs = sp.getLong(prefix + "delayMinMs", 0),
                        delayMaxMs = sp.getLong(prefix + "delayMaxMs", 500),
                        rawDelayMin = sp.getLong(prefix + "rawDelayMin", 0),
                        rawDelayMax = sp.getLong(prefix + "rawDelayMax", 500),
                        rawDelayUnit = TimeUnit.fromDisplayName(sp.getString(prefix + "rawDelayUnit", "毫秒") ?: "毫秒"),
                        timestamp = timestamp
                    )
                } else {
                    ScriptAction.Swipe(
                        startX = sp.getInt(prefix + "startX", 0),
                        startY = sp.getInt(prefix + "startY", 0),
                        endX = sp.getInt(prefix + "endX", 0),
                        endY = sp.getInt(prefix + "endY", 0),
                        durationMinMs = sp.getLong(prefix + "durationMinMs", 200),
                        durationMaxMs = sp.getLong(prefix + "durationMaxMs", 500),
                        delayMinMs = sp.getLong(prefix + "delayMinMs", 0),
                        delayMaxMs = sp.getLong(prefix + "delayMaxMs", 500),
                        rawDelayMin = sp.getLong(prefix + "rawDelayMin", 0),
                        rawDelayMax = sp.getLong(prefix + "rawDelayMax", 500),
                        rawDelayUnit = TimeUnit.fromDisplayName(sp.getString(prefix + "rawDelayUnit", "毫秒") ?: "毫秒"),
                        rawDurationMin = sp.getLong(prefix + "rawDurationMin", 200),
                        rawDurationMax = sp.getLong(prefix + "rawDurationMax", 500),
                        rawDurationUnit = TimeUnit.fromDisplayName(sp.getString(prefix + "rawDurationUnit", "毫秒") ?: "毫秒"),
                        timestamp = timestamp
                    )
                }
                script.actions.add(action)
            }

            // 5. 恢复标记
            val markCount = sp.getInt("${getScriptPrefix()}mark_count", 0)
            for (i in 0 until markCount) {
                val prefix = "${getScriptPrefix()}mark_${i}_"
                val mark = MarkAreaItem(
                    rect = Rect(
                        sp.getInt(prefix + "rect_left", 0),
                        sp.getInt(prefix + "rect_top", 0),
                        sp.getInt(prefix + "rect_right", 0),
                        sp.getInt(prefix + "rect_bottom", 0)
                    ),
                    swipeEndRect = Rect(
                        sp.getInt(prefix + "swipe_left", 0),
                        sp.getInt(prefix + "swipe_top", 0),
                        sp.getInt(prefix + "swipe_right", 0),
                        sp.getInt(prefix + "swipe_bottom", 0)
                    ),
                    actionType = try {
                        MarkAreaItem.ActionType.valueOf(sp.getString(prefix + "actionType", "CLICK")!!)
                    } catch (e: Exception) {
                        MarkAreaItem.ActionType.CLICK
                    },
                    actionNumber = sp.getInt(prefix + "actionNumber", i + 1),
                    actionIndex = i
                )
                markAreaList.add(mark)
            }

            // 6. 同步序号
            markAreaList.forEachIndexed { i, mark ->
                mark.actionIndex = i
                mark.actionNumber = i + 1
            }

            val floatWindow = FloatingSessionManager.getInstance(context).getActiveFloatingWindow()
            floatWindow?.let {
                it.areaDrawOverlay?.let { it1 -> it1.visibility = View.VISIBLE }
                it.refreshDraw()
            }

            Toast.makeText(context, "恢复成功，矩形已显示", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "恢复失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 清空本地保存的脚本状态（完整清理ScriptSettings/TimeUnit相关字段）
     */
    fun clearSavedState() {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()

        try {
            // 1. 删除Script基础属性
            editor.remove("${getScriptPrefix()}name")
            editor.remove("${getScriptPrefix()}isSetting")
            editor.remove("${getScriptPrefix()}isFloatingWindowShow")
            editor.remove("${getScriptPrefix()}isRunning")
            editor.remove("${getScriptPrefix()}defaultDelayMin")
            editor.remove("${getScriptPrefix()}defaultDelayMax")

            // 2. 删除ScriptSettings
            val settingsPrefix = "${getScriptPrefix()}settings_"
            editor.remove("${settingsPrefix}nextIntervalMin")
            editor.remove("${settingsPrefix}nextIntervalMax")
            editor.remove("${settingsPrefix}actionUnit")
            editor.remove("${settingsPrefix}durationIntervalMin")
            editor.remove("${settingsPrefix}durationIntervalMax")
            editor.remove("${settingsPrefix}durationUnit")

            // 3. 删除ScriptAction列表
            val actionCount = sp.getInt("${getScriptPrefix()}action_count", 0)
            for (i in 0 until actionCount) {
                val actionPrefix = "${getScriptPrefix()}action_${i}_"
                editor.remove("${actionPrefix}timestamp")
                editor.remove("${actionPrefix}hasMarkInfo")
                editor.remove("${actionPrefix}type")

                // Click/Swipe通用字段
                editor.remove("${actionPrefix}delayMinMs")
                editor.remove("${actionPrefix}delayMaxMs")
                editor.remove("${actionPrefix}rawDelayMin")
                editor.remove("${actionPrefix}rawDelayMax")
                editor.remove("${actionPrefix}rawDelayUnit")

                // Click专属
                editor.remove("${actionPrefix}click_left")
                editor.remove("${actionPrefix}click_top")
                editor.remove("${actionPrefix}click_right")
                editor.remove("${actionPrefix}click_bottom")

                // Swipe专属
                editor.remove("${actionPrefix}startX")
                editor.remove("${actionPrefix}startY")
                editor.remove("${actionPrefix}endX")
                editor.remove("${actionPrefix}endY")
                editor.remove("${actionPrefix}durationMinMs")
                editor.remove("${actionPrefix}durationMaxMs")
                editor.remove("${actionPrefix}rawDurationMin")
                editor.remove("${actionPrefix}rawDurationMax")
                editor.remove("${actionPrefix}rawDurationUnit")
            }
            editor.remove("${getScriptPrefix()}action_count")

            // 4. 删除MarkAreaItem列表
            val markCount = sp.getInt("${getScriptPrefix()}mark_count", 0)
            for (i in 0 until markCount) {
                val markPrefix = "${getScriptPrefix()}mark_${i}_"
                editor.remove("${markPrefix}actionType")
                editor.remove("${markPrefix}actionNumber")
                editor.remove("${markPrefix}actionIndex")
                editor.remove("${markPrefix}rect_left")
                editor.remove("${markPrefix}rect_top")
                editor.remove("${markPrefix}rect_right")
                editor.remove("${markPrefix}rect_bottom")
                editor.remove("${markPrefix}swipe_left")
                editor.remove("${markPrefix}swipe_top")
                editor.remove("${markPrefix}swipe_right")
                editor.remove("${markPrefix}swipe_bottom")
            }
            editor.remove("${getScriptPrefix()}mark_count")

            // 提交删除
            editor.apply()

            // 5. 清空内存数据
            script.actions.clear()
            markAreaList.clear()
            script.isSetting = false
            script.isFloatingWindowShow = false
            script.isRunning = false
            script.defaultDelayMin = null
            script.defaultDelayMax = null
            script.settings = ScriptSettings() // 重置为默认设置
            refreshDraw()

            Toast.makeText(context, "脚本「${script.name}」状态已清空", Toast.LENGTH_SHORT).show()
            Log.d("SaveBtnFunction", "✅ 脚本${script.id}（${script.name}）SP状态已清空")
        } catch (e: Exception) {
            Toast.makeText(context, "清空失败：${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("SaveBtnFunction", "❌ 清空脚本${script.id}失败", e)
        }
    }
}