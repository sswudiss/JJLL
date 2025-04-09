package com.example.jjll.common

// 定義 Repository 結果封裝，可以包含成功數據或錯誤信息
sealed class RepoResult<out T> {
    data class Success<T>(val data: T) : RepoResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : RepoResult<Nothing>()
    object Loading : RepoResult<Nothing>() // 可選：用於表示加載中
}
