package com.example.render_native_demo

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val statusView = TextView(this).apply {
            gravity = Gravity.CENTER
            text = runtimeBaseline()
        }
        setContentView(statusView)
    }

    companion object {
        init {
            System.loadLibrary("render_native_demo")
        }

        @JvmStatic
        external fun runtimeBaseline(): String
    }
}
