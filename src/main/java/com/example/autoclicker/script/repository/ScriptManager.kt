//// 路径：com/example/autoclicker/script/repository/ScriptManager.kt
//package com.example.autoclicker.script.repository
//
//import com.example.autoclicker.script.model.ScriptDataDefine
//
//// 脚本数据唯一管家：处理增删改查，全局单例
//object ScriptManager {
//    private val scriptList = mutableListOf<ScriptDataDefine>()
//
//    // 初始化示例数据
//    fun initSampleData() {
//        val scriptNames = listOf("Boss直聘", "美团抢券", "支付宝下滑", "抖音自动点赞", "微信自动回复")
//        scriptList.clear()
//        scriptNames.forEachIndexed { index, name ->
//            scriptList.add(ScriptDataDefine((index + 1).toString(), name))
//        }
//    }
//
//    // 获取所有脚本
//    fun getScripts(): List<ScriptDataDefine> = scriptList.toList()
//
//    // 新增脚本
//    fun addScript(script: ScriptDataDefine) {
//        scriptList.add(script)
//    }
//
//    // 删除脚本
//    fun removeScript(script: ScriptDataDefine) {
//        scriptList.remove(script)
//    }
//
//    // 重命名脚本
//    fun renameScript(script: ScriptDataDefine, newName: String) {
//        val index = scriptList.indexOfFirst { it.id == script.id }
//        if (index != -1) {
//            scriptList[index] = scriptList[index].copy(name = newName)
//        }
//    }
//}