package com.smybs0.deepseekchat

import android.app.Application
import com.smybs0.deepseeklib.DeepseekConfig

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DeepseekConfig.init(this, "user deepseek api key")
    }
}