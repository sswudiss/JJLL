package com.example.jjll.di

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
            supabaseUrl = "https://egqabugxkpnhvkeuarys.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVncWFidWd4a3BuaHZrZXVhcnlzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDM4NDYzNTcsImV4cCI6MjA1OTQyMjM1N30.xtF_0nR2hDXrqVWOOb1JJ_D3oRRzlk9aMB8SzboUG6Y"
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