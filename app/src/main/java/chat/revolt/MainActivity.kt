package chat.revolt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.navigation.compose.dialog
import chat.revolt.api.settings.GlobalState
import chat.revolt.screens.SplashScreen
import chat.revolt.screens.about.AboutScreen
import chat.revolt.screens.about.AttributionScreen
import chat.revolt.screens.about.PlaceholderScreen
import chat.revolt.screens.chat.ChatRouterScreen
import chat.revolt.screens.chat.dialogs.FeedbackDialog
import chat.revolt.screens.login.GreeterScreen
import chat.revolt.screens.login.LoginScreen
import chat.revolt.screens.login.MfaScreen
import chat.revolt.screens.settings.AppearanceSettingsScreen
import chat.revolt.screens.settings.SettingsScreen
import chat.revolt.ui.theme.RevoltTheme
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.android.core.SentryAndroid

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
        }

        setContent {
            AppEntrypoint()
        }
    }
}

val RevoltTweenInt: FiniteAnimationSpec<IntOffset> = tween(400, easing = EaseInOutExpo)
val RevoltTweenIntSize: FiniteAnimationSpec<IntSize> = tween(400, easing = EaseInOutExpo)
val RevoltTweenFloat: FiniteAnimationSpec<Float> = tween(400, easing = EaseInOutExpo)
val RevoltTweenDp: FiniteAnimationSpec<Dp> = tween(400, easing = EaseInOutExpo)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppEntrypoint() {
    val navController = rememberAnimatedNavController()

    RevoltTheme(
        requestedTheme = GlobalState.theme,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedNavHost(
                navController = navController,
                startDestination = "splash",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentScope.SlideDirection.Left,
                        animationSpec = RevoltTweenInt
                    ) + fadeIn(animationSpec = RevoltTweenFloat)
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentScope.SlideDirection.Left,
                        animationSpec = RevoltTweenInt
                    ) + fadeOut(animationSpec = RevoltTweenFloat)
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentScope.SlideDirection.Right,
                        animationSpec = RevoltTweenInt
                    ) + fadeIn(animationSpec = RevoltTweenFloat)
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentScope.SlideDirection.Right,
                        animationSpec = RevoltTweenInt
                    ) + fadeOut(animationSpec = RevoltTweenFloat)
                }
            ) {
                composable("splash") { SplashScreen(navController) }

                composable("login/greeting") { GreeterScreen(navController) }
                composable("login/login") { LoginScreen(navController) }
                composable("login/mfa/{mfaTicket}/{allowedAuthTypes}") { backStackEntry ->
                    val mfaTicket = backStackEntry.arguments?.getString("mfaTicket") ?: ""
                    val allowedAuthTypes =
                        backStackEntry.arguments?.getString("allowedAuthTypes") ?: ""

                    MfaScreen(navController, allowedAuthTypes, mfaTicket)
                }

                composable("chat") { ChatRouterScreen(navController) }

                composable("settings") { SettingsScreen(navController) }
                composable("settings/appearance") { AppearanceSettingsScreen(navController) }
                dialog("settings/feedback") { FeedbackDialog(navController) }

                composable("about") { AboutScreen(navController) }
                composable("about/oss") { AttributionScreen(navController) }
                composable("about/placeholder") { PlaceholderScreen(navController) }
            }
        }
    }
}
