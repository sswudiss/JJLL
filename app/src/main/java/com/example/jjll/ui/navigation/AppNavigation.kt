package com.example.jjll.ui.navigation // 建議將導航相關文件放在這個包下

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jjll.ui.auth.LoginScreen
import com.example.jjll.ui.auth.SignUpScreen
import com.example.jjll.ui.chat.ChatScreen // 假設的聊天屏幕文件路徑
import com.example.jjll.ui.home.HomeScreen
import com.example.jjll.ui.search.SearchUsersScreen // 假設的搜索屏幕文件路徑
import com.example.jjll.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Defines all the navigation routes in the application in a type-safe way.
 */
object Screen {
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val SEARCH_USERS = "search_users"

    // Route for chat detail screen, which requires a chatId argument.
    private const val CHAT_DETAIL_ROUTE = "chat_detail"
    const val CHAT_ID_ARG = "chatId" // Argument name
    val CHAT_DETAIL = "$CHAT_DETAIL_ROUTE/{$CHAT_ID_ARG}"

    /**
     * Helper function to create the full route for navigating to a specific chat.
     * e.g., createChatDetailRoute("some-chat-uuid") -> "chat_detail/some-chat-uuid"
     */
    fun createChatDetailRoute(chatId: String) = "$CHAT_DETAIL_ROUTE/$chatId"
}

/**
 * The main navigation component for the application.
 * It sets up the NavHost and defines all composable screens.
 * It also handles determining the initial screen based on authentication state.
 */
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    // State to hold the determined start destination. Initially null while checking auth status.
    var startDestination by remember { mutableStateOf<String?>(null) }

    // This effect runs once when AppNavigation enters the composition.
    // It launches a coroutine to check the initial authentication status.
    LaunchedEffect(key1 = Unit) {
        coroutineScope.launch {
            val isLoggedIn = authViewModel.getInitialAuthStatus()
            startDestination = if (isLoggedIn) Screen.HOME else Screen.LOGIN
        }
    }

    // While startDestination is being determined, show a loading indicator.
    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        // Once the start destination is known, set up the NavHost.
        NavHost(
            navController = navController,
            startDestination = startDestination!! // We know it's not null here.
        ) {
            // Authentication Flow
            composable(route = Screen.LOGIN) {
                LoginScreen(navController = navController)
            }
            composable(route = Screen.SIGN_UP) {
                SignUpScreen(navController = navController)
            }

            // Main App Flow
            composable(route = Screen.HOME) {
                HomeScreen(navController = navController)
            }

            composable(route = Screen.SEARCH_USERS) {
                SearchUsersScreen(navController = navController)
            }

            // Chat Detail Screen with Argument
            composable(
                route = Screen.CHAT_DETAIL,
                arguments = listOf(
                    // Define the 'chatId' argument and specify its type as String.
                    navArgument(Screen.CHAT_ID_ARG) {
                        type = NavType.StringType
                        // You can also specify nullable = false, or a defaultValue if needed.
                    }
                )
            ) { backStackEntry ->
                // The ChatScreen will use a Hilt ViewModel, which can retrieve
                // the 'chatId' argument from the SavedStateHandle automatically.
                ChatScreen(navController = navController)
            }
        }
    }
}