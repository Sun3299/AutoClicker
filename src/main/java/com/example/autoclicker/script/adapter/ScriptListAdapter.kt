package com.example.autoclicker.script.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.autoclicker.R
import com.example.autoclicker.script.model.Script

/**
 * 脚本列表适配器
 * 职责：展示脚本item、分发点击事件
 */
class ScriptListAdapter(
    private var scriptList: MutableList<Script>,
    private val listener: OnItemActionListener
) : RecyclerView.Adapter<ScriptListAdapter.ScriptViewHolder>() {

    // 回调接口：让Activity处理点击事件
    interface OnItemActionListener {
        fun onStartClick(script: Script)
        fun onRenameClick(script: Script)
        fun onDeleteClick(script: Script)
    }

    // ViewHolder：缓存item控件
    class ScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvScriptName: TextView = itemView.findViewById(R.id.tv_script_name)
        val ivStart: ImageView = itemView.findViewById(R.id.iv_start)
        val ivMore: ImageView = itemView.findViewById(R.id.iv_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script, parent, false)
        return ScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val script = scriptList[position]
        holder.tvScriptName.text = script.name

        // 启动按钮点击
        holder.ivStart.setOnClickListener {
            listener.onStartClick(script)
        }

        // 更多按钮点击（弹出菜单）
        holder.ivMore.setOnClickListener {
            showMoreMenu(it, script)
        }
    }

    // 显示更多操作菜单
    private fun showMoreMenu(view: View, script: Script) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.menu_script_more, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename -> {
                    listener.onRenameClick(script)
                    true
                }
                R.id.action_delete -> {
                    listener.onDeleteClick(script)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun getItemCount() = scriptList.size

    // 添加新脚本
    fun addScript(script: Script) {
        scriptList.add(script)
        notifyItemInserted(scriptList.size - 1)
    }

    // 删除脚本
    fun removeScript(script: Script) {
        val position = scriptList.indexOfFirst { it.id == script.id }
        if (position != -1) {
            scriptList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // 更新脚本名称
    fun updateScriptName(script: Script, newName: String) {
        val position = scriptList.indexOfFirst { it.id == script.id }
        if (position != -1) {
            scriptList[position] = script.copy(name = newName)
            notifyItemChanged(position)
        }
    }
}