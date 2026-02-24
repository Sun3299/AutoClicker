package com.example.autoclicker.script.ui.floating

class CloseBtnFunction(
    private val floatingWindowManager: FloatingWindowManager
) {
    fun destroy(){
        floatingWindowManager.destroy()
    }
}