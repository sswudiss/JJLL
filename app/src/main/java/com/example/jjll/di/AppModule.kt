package com.example.jjll.di // 確保包名正確


import com.example.jjll.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import javax.inject.Singleton

/*
* 現在我們來設置 Hilt，讓它可以提供 (Provide) 一個單例的 (Singleton) SupabaseClient 實例給我們 App 的其他部分使用。
* */

@Module
@InstallIn(SingletonComponent::class) // 表示這個 Module 提供的依賴在 Application 生命周期內是單例
object AppModule {

    @OptIn(SupabaseInternal::class)
    @Provides
    @Singleton // 標記提供的 SupabaseClient 是單例
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            // 使用 BuildConfig 安全地獲取 URL 和 Key
            //import com.example.jjll.BuildConfig
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // 安裝你需要的功能模塊
            install(Auth)
            install(Postgrest)
            install(Realtime) {
                // Realtime 的可選配置 (例如重連間隔等)
                // reconnectionInterval = 10.seconds
            }
            install(Storage)

            // 配置 Ktor client engine (CIO 通常是不錯的選擇 for Android)
            // ktorEngine = CIO.create() // 可選, Supabase-kt 會提供默認引擎
        }
    }

    // 我們可以直接從 SupabaseClient 中獲取 Auth, Postgrest 等實例
    // 但如果你想單獨注入它們，可以這樣提供：
    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth = client.auth

    @Provides
    @Singleton
    fun provideSupabaseDatabase(client: SupabaseClient): Postgrest = client.postgrest // <-- 確保這個提供函數存在

    @Provides
    @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime {
        // 注意: 你需要先連接 Realtime 才能使用
        // client.realtime.connect() // 不建議在這裡連接，應該在需要時管理連接狀態
        return client.realtime
    }

    @Provides
    @Singleton
    fun provideSupabaseStorage(client: SupabaseClient): Storage {
        return client.storage
    }
}