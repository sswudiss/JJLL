// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20" // 👈 **確保添加此插件** 並使用你的Kotlin版本
    id("com.google.dagger.hilt.android") version "2.56.1" apply false
    id("com.google.devtools.ksp") version "2.1.20-1.0.32" apply false
    //實現推送通知
    id("com.google.gms.google-services") version "4.4.2" apply false
}


