package com.example.jjll.di // 替換為你的包名

// ... 其他 imports ...
import com.example.jjll.data.repository.ChatRepository
import com.example.jjll.data.repository.ChatRepositoryImpl
import com.example.jjll.data.repository.FriendRepository
import com.example.jjll.data.repository.FriendRepositoryImpl
import com.example.jjll.data.repository.ProfileRepository
import com.example.jjll.data.repository.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent // 或 ViewModelComponent，取決於範圍
import javax.inject.Singleton


// RepositoryModule.kt
// ...
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindFriendRepository(impl: FriendRepositoryImpl): FriendRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}