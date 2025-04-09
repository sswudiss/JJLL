import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android") //
    //The Hilt Android Gradle plugin is applied but no com.google.dagger:hilt-compiler dependency was found.
    //版本號是否一致？ ksp("...:hilt-compiler:VERSION") 必須與 implementation("...:hilt-android:VERSION") 中的 VERSION 完全相同，
// 並且也必須與你項目根目錄 build.gradle.kts 文件中 plugins { id("com.google.dagger.hilt.android") version "VERSION" ... } 的 VERSION 完全相同。
    kotlin("plugin.serialization") version "2.1.20" //缺少的話注冊會失敗
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

        // --- 從 local.properties 讀取 Supabase 憑證 ---
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties") // 指向項目根目錄

        // 添加日誌方便調試 (這些日誌會在 Gradle Sync 時打印到 Build 輸出)
        println("Checking local.properties...")
        println("File path: ${localPropertiesFile.absolutePath}")
        println("File exists: ${localPropertiesFile.exists()}")

        if (localPropertiesFile.exists()) {
            try {
                localProperties.load(localPropertiesFile.inputStream())
                println("local.properties loaded successfully.")
                // 打印讀取到的原始值（注意 Key 的敏感性，只打印 URL 或 Key 的一部分）
                println("Raw value for 'supabase.url': ${localProperties.getProperty("supabase.url")}")
                println("Raw value for 'supabase.anon.key': ${localProperties.getProperty("supabase.anon.key")?.take(5)}...") // 只顯示前5位
            } catch (e: Exception) {
                println("Warning: Could not load local.properties: ${e.message}")
                // 可以在這裡拋出錯誤，如果憑證是必須的
                // throw GradleException("Failed to load local.properties", e)
            }
        } else {
            println("Warning: local.properties file not found. Using default/empty values.")
            // 如果 local.properties 是必需的，可以在這裡報錯停止構建
            // throw GradleException("local.properties not found. Please create it with Supabase credentials.")
        }

        // 從 localProperties 獲取值，如果為 null 則使用備用值
        //SUPABASE_URL和SUPABASE_ANON_KEY必須和根目錄下local.properties (全小寫)的一致
        val supabaseUrl = localProperties.getProperty("SUPABASE_URL", "URL_NOT_FOUND") // 使用明確的備用值
        val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY", "KEY_NOT_FOUND") // 使用明確的備用值

        // 打印最終用於生成 BuildConfig 的值
        println("Raw value for 'SUPABASE_URL': ${localProperties.getProperty("SUPABASE_URL")}")
        println("Raw value for 'SUPABASE_ANON_KEY': ${localProperties.getProperty("SUPABASE_ANON_KEY")?.take(5)}...")

        // **生成 BuildConfig 字段**
        // 確保第二個參數（常量名）和第三個參數（值的字符串形式）正確
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        // -----------------------------------------------------
    }
        buildTypes {
            release {
                isMinifyEnabled = false  // 你可以根據需要設置為 true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                // signingConfig signingConfigs.getByName("release") // 如果配置了簽名
            }
            debug {
                // isMinifyEnabled = false (默認)
                // applicationIdSuffix ".debug" // 可選
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11  //或更高
            targetCompatibility = JavaVersion.VERSION_11
        }
        kotlinOptions {
            jvmTarget = "11"   // 應與 compileOptions 兼容
        }
        buildFeatures {
            compose = true
            buildConfig = true // 👈 啟用 BuildConfig 生成
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
        implementation("com.google.dagger:hilt-android:2.56.1") // <-- 示例版本，確保與 compiler 和 plugin 一致

        // Hilt - 註解處理器 (使用 KSP!)
        ksp("com.google.dagger:hilt-compiler:2.56.1")
        // -------->  確保版本號與 implementation 和根 Gradle 中的 Hilt 插件版本完全一致 <--------
        // Hilt Navigation Compose - 提供 hiltViewModel() 函數
        implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

        // Jetpack Compose 的核心 Material 3 庫 (androidx.compose.material3:material3) 只包含了非常有限的一組基礎圖標。
        // 要使用 Icons.Filled 下面豐富的圖標集（如 AccountCircle, Search, MoreVert, Chat, Contacts, Person 等），
        // 如果你使用了Compose BOM (推荐)
        implementation(libs.androidx.material.icons.extended)

        // === 其他可能用到的庫 (例如 Coil for Image Loading) ===
        implementation(libs.coil.compose)

        // Ktor client - 確保這個依賴也在
        implementation(libs.ktor.ktor.client.okhttp) // 檢查下 Ktor 最新穩定版

        implementation(libs.kotlinx.serialization.json)
}