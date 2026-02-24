package com.example.autoclicker.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.autoclicker.R
import com.example.autoclicker.script.adapter.ScriptListAdapter
import com.example.autoclicker.script.model.Script
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * 脚本列表页面
 * 职责：展示脚本列表、处理脚本CRUD、悬浮窗启停
 */
class ScriptListActivity : AppCompatActivity(),
    ScriptListAdapter.OnItemActionListener {

    private lateinit var scriptListAdapter: ScriptListAdapter
    private val scriptList = mutableListOf<Script>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initSampleScripts()
        initRecyclerView()
        initClickEvents()
    }
    private fun initSampleScripts() {
        val scriptNames = listOf(
            "Boss直聘", "美团抢券", "支付宝下滑", "抖音自动点赞", "微信自动回复"
        )
        scriptNames.forEachIndexed { index, name ->
            val script = Script(id = (index + 1).toString(), name = name)
            // 替换：用新的 initScriptContext 替代旧的 initFloatingWindow
            script.initScriptContext(this)
            scriptList.add(script)
        }
    }
    private fun initRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_script_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        scriptListAdapter = ScriptListAdapter(scriptList, this)
        recyclerView.adapter = scriptListAdapter
    }

    private fun initClickEvents() {
        // 新增脚本
        findViewById<FloatingActionButton>(R.id.btn_add).setOnClickListener {
            showAddScriptDialog()
        }
        // 全局设置
        findViewById<FloatingActionButton>(R.id.btn_settings).setOnClickListener {
            showGlobalSettingsDialog()
        }
    }


    // 新建脚本弹窗
    private fun showAddScriptDialog() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("新建脚本")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    val newScript = Script(
                        id = System.currentTimeMillis().toString(),
                        name = name
                    )
                    // 替换：初始化脚本上下文（不再直接初始化悬浮窗）
                    newScript.initScriptContext(this)
                    scriptListAdapter.addScript(newScript)
                    Toast.makeText(this, "脚本【$name】创建成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "脚本名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }


    // 显示全局设置
    private fun showGlobalSettingsDialog() {
        Toast.makeText(this, "测试", Toast.LENGTH_SHORT).show()
    }

    // 启动脚本：显示该脚本的独立悬浮窗
    override fun onStartClick(script: Script) {
        // 同一时间只显示一个悬浮窗
        scriptList.forEach {
            if (it.id != script.id && it.isFloatingWindowShow) {
                it.getFloatingWindowManager()?.closeFloatingWindow()
            }
        }
        // 调用新的 showFloatingWindow 方法（交由 Session 管理）
        script.showFloatingWindow()
        Toast.makeText(this, "已启动【${script.name}】专属悬浮窗", Toast.LENGTH_SHORT).show()
    }

    // 重命名脚本
    override fun onRenameClick(script: Script) {
        val editText = EditText(this).apply { setText(script.name) }
        AlertDialog.Builder(this)
            .setTitle("重命名脚本")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != script.name) {
                    scriptListAdapter.updateScriptName(script, newName)
                    Toast.makeText(this, "脚本已重命名为【$newName】", Toast.LENGTH_SHORT).show()
                } else if (newName.isEmpty()) {
                    Toast.makeText(this, "脚本名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 删除脚本
    override fun onDeleteClick(script: Script) {
        AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除脚本【${script.name}】吗？删除后不可恢复")
            .setPositiveButton("删除") { _, _ ->
                if (script.isFloatingWindowShow) {
                    script.getFloatingWindowManager()?.closeFloatingWindow()
                }
                // 清空脚本资源，避免内存泄漏
                script.clear()
                // 从列表移除
                scriptListAdapter.removeScript(script)
                Toast.makeText(this, "脚本【${script.name}】已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 页面销毁时：清空所有脚本资源，避免内存泄漏
    override fun onDestroy() {
        super.onDestroy()
        scriptList.forEach { it.clear() }
        scriptList.clear()
    }
}