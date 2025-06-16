package com.example.jjll.di


import com.example.jjll.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            // 從 BuildConfig 中安全地引用變量
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY

        ) {
            // 安裝 Supabase 插件
            install(Auth) { // <--- 修改: 使用 Auth 而不是 GoTrue
                // Auth (原 GoTrue) 的特定配置 (可選)
                // e.g., autoLoadFromStorage = true (默認)
                // e.g., autoSaveToStorage = true (默認)
                // e.g., scheme = "app"
                // e.g., host = "login"
                // 如果使用 OAuth deep linking，可能需要配置 scheme 和 host
            }
            install(Postgrest) {
                // Postgrest 的特定配置 (可選)
            }
            install(Realtime) {
                // Realtime 的特定配置 (可選)
            }
            install(Storage) {
                // Storage 的特定配置 (可選)
            }
        }
    }
}