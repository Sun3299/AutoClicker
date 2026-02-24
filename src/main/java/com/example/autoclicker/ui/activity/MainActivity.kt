package com.example.autoclicker.ui.activity


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.autoclicker.service.PermissionChecker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 无布局，仅做权限检查入口
    }

    override fun onResume() {
        super.onResume()
        PermissionChecker.checkAllPermissions(this) {
            // 权限通过，跳转到脚本列表页
            val intent = Intent(this, ScriptListActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}