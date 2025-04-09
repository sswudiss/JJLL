package com.example.jjll.ui.navigation

//為了避免在代碼中硬編碼路由字符串（容易出錯），一個好的做法是定義路由常量。

// 使用 object 或 sealed class 來定義路由
object AppDestinations {
    const val LOGIN_ROUTE = "login"
    const val REGISTER_ROUTE = "register"
    const val MAIN_ROUTE = "main" // 主界面路由 (包括底部導航)
    // 後續可以添加聊天詳情頁等的路由
    const val CHAT_DETAIL_ROUTE = "chat_detail"
    const val CHAT_DETAIL_ID_ARG = "conversationId" // 聊天詳情頁需要的參數名
    val CHAT_DETAIL_ROUTE_WITH_ARGS = "$CHAT_DETAIL_ROUTE/{$CHAT_DETAIL_ID_ARG}" // 帶參數的路由模板

    // 可以在這裡為需要參數的路由定義創建導航路徑的輔助函數
    fun createChatDetailRoute(conversationId: String) = "$CHAT_DETAIL_ROUTE/$conversationId"

    // 其他可能的路由...
    // const val PROFILE_EDIT_ROUTE = "profile_edit"
}