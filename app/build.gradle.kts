import java.util.Properties

//---避免泄露 Supabase 項目 URL 和 service_role_key---
//加載 local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
//---避免泄露 Supabase 項目 URL 和 service_role_key---

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android") //
    //The Hilt Android Gradle plugin is applied but no com.google.dagger:hilt-compiler dependency was found.
    //版本號是否一致？ ksp("...:hilt-compiler:VERSION") 必須與 implementation("...:hilt-android:VERSION") 中的 VERSION 完全相同，
// 並且也必須與你項目根目錄 build.gradle.kts 文件中 plugins { id("com.google.dagger.hilt.android") version "VERSION" ... } 的 VERSION 完全相同。
    kotlin("plugin.serialization") version "2.1.20" //序列化插件，缺少的話注冊會失敗
//實現推送通知
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.jjll"
    compileSdk = 35  //<-- 確保與你的環境匹配 (例如 33, 34 或更高)

    defaultConfig {
        applicationId = "com.example.jjll"
        minSdk = 31  // <-- 你設定的最低 SDK 版本
        targetSdk = 35 // <-- 確保與 compileSdk 匹配或稍低
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // 👈 啟用 BuildConfig 生成
    }

    buildTypes {
        release {
            isMinifyEnabled = false  // 你可以根據需要設置為 true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            //---避免泄露 Supabase 項目 URL 和 service_role_key---
            //為 release 版本創建 BuildConfig 字段
            buildConfigField(
                "String",  //變量的類型
                "SUPABASE_URL", //變量的名稱
                //非常重要！ 值的內容必須是一個字符串字面量，這意味著它需要被包含在轉義的引號 \"...\" 中
                "\"${localProperties.getProperty("SUPABASE_URL")}\""
            )
            buildConfigField(
                "String",
                "SUPABASE_ANON_KEY",
                "\"${localProperties.getProperty("SUPABASE_ANON_KEY")}\""
            )
            //---避免泄露 Supabase 項目 URL 和 service_role_key---
        }
        debug {
            //---避免泄露 Supabase 項目 URL 和 service_role_key---
            //為 debug 版本創建 BuildConfig 字段
            buildConfigField(
                "String",
                "SUPABASE_URL",
                "\"${localProperties.getProperty("SUPABASE_URL")}\""
            )
            buildConfigField(
                "String",
                "SUPABASE_ANON_KEY",
                "\"${localProperties.getProperty("SUPABASE_ANON_KEY")}\""
            )
            //---避免泄露 Supabase 項目 URL 和 service_role_key---
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11  //或更高
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"   // 應與 compileOptions 兼容
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // === Navigation Compose ===
    implementation(libs.androidx.navigation.compose)

    // === ViewModel Compose (用於狀態管理) ===
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // === Supabase Kotlin SDK ===
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:supabase-kt")
    implementation("io.ktor:ktor-client-okhttp:3.1.2")

    // === Hilt (依賴注入 - 先添加，後續使用) ===
    // Hilt - 核心庫
    implementation(libs.hilt.android) // <-- 示例版本，確保與 compiler 和 plugin 一致
    // Hilt - 註解處理器 (使用 KSP!)
    ksp(libs.hilt.compiler)
    // -------->  確保版本號與 implementation 和根 Gradle 中的 Hilt 插件版本完全一致 <--------
    // Hilt Navigation Compose - 提供 hiltViewModel() 函數
    implementation(libs.androidx.hilt.navigation.compose)

    // Jetpack Compose 的核心 Material 3 庫 (androidx.compose.material3:material3) 只包含了非常有限的一組基礎圖標。
    // 要使用 Icons.Filled 下面豐富的圖標集（如 AccountCircle, Search, MoreVert, Chat, Contacts, Person 等），
    // 如果你使用了Compose BOM (推荐)
    implementation(libs.androidx.material.icons.extended)

    // Coil for Compose (異步加載網絡圖片)
    implementation(libs.coil.compose)

    // Ktor client - 確保這個依賴也在
    implementation(libs.ktor.ktor.client.okhttp) // 檢查下 Ktor 最新穩定版

    //序列化
    implementation(libs.kotlinx.serialization.json)

    //用於獲取當前的 ISO 8601 格式的時間字符串
    implementation(libs.kotlinx.datetime) // 檢查最新版本

    // Accompanist Swipe to Refresh (下拉刷新)
    implementation(libs.accompanist.swiperefresh)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    // Add the dependency for the Cloud Messaging library
    implementation(libs.firebase.messaging.ktx)

}