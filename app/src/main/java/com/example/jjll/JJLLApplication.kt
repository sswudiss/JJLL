package com.example.jjll

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

//Hilt 需要一個繼承自 Application 的類，並用 @HiltAndroidApp 註解。

@HiltAndroidApp // 👈 Hilt 必需的註解
class JJLLApplication  : Application() {
    // 目前這個類可以是空的，Hilt 會處理初始化
    // 未來可以在這裡做一些全局初始化工作 (如果需要的話)
    override fun onCreate() {
        super.onCreate()
        // 例如：全局日誌庫初始化等
    }
}