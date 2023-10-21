package chat.revolt.activities

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import chat.revolt.BuildConfig
import chat.revolt.api.settings.GlobalState
import chat.revolt.ndk.NativeLibraries
import chat.revolt.screens.SplashScreen
import chat.revolt.screens.about.AboutScreen
import chat.revolt.screens.about.AttributionScreen
import chat.revolt.screens.chat.ChatRouterScreen
import chat.revolt.screens.chat.dialogs.FeedbackDialog
import chat.revolt.screens.login.LoginGreetingScreen
import chat.revolt.screens.login.LoginScreen
import chat.revolt.screens.login.MfaScreen
import chat.revolt.screens.register.OnboardingScreen
import chat.revolt.screens.register.RegisterDetailsScreen
import chat.revolt.screens.register.RegisterGreetingScreen
import chat.revolt.screens.register.RegisterVerifyScreen
import chat.revolt.screens.settings.AppearanceSettingsScreen
import chat.revolt.screens.settings.ChangelogsSettingsScreen
import chat.revolt.screens.settings.ClosedBetaUpdaterScreen
import chat.revolt.screens.settings.DebugSettingsScreen
import chat.revolt.screens.settings.SettingsScreen
import chat.revolt.ui.theme.RevoltTheme
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.android.core.SentryAndroid

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AppEntrypoint(windowSizeClass)
        }
    }

    companion object {
        init {
            NativeLibraries.init()
        }
    }
}

val RevoltTweenInt: FiniteAnimationSpec<IntOffset> = tween(400, easing = EaseInOutExpo)
val RevoltTweenFloat: FiniteAnimationSpec<Float> = tween(400, easing = EaseInOutExpo)
val RevoltTweenDp: FiniteAnimationSpec<Dp> = tween(400, easing = EaseInOutExpo)
val RevoltTweenColour: FiniteAnimationSpec<Color> = tween(400, easing = EaseInOutExpo)

@Composable
fun AppEntrypoint(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()

    RevoltTheme(
        requestedTheme = GlobalState.theme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = "splash",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = RevoltTweenInt
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = RevoltTweenInt
                    ) + fadeOut(animationSpec = RevoltTweenFloat)
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = RevoltTweenInt
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = RevoltTweenInt
                    )
                }
            ) {
                composable("splash") { SplashScreen(navController) }

                composable("login/greeting") { LoginGreetingScreen(navController) }
                composable("login/login") { LoginScreen(navController) }
                composable("login/mfa/{mfaTicket}/{allowedAuthTypes}") { backStackEntry ->
                    val mfaTicket = backStackEntry.arguments?.getString("mfaTicket") ?: ""
                    val allowedAuthTypes =
                        backStackEntry.arguments?.getString("allowedAuthTypes") ?: ""

                    MfaScreen(navController, allowedAuthTypes, mfaTicket)
                }

                composable("register/greeting") { RegisterGreetingScreen(navController) }
                composable("register/details") { RegisterDetailsScreen(navController) }
                composable("register/verify/{email}") { backStackEntry ->
                    val email = backStackEntry.arguments?.getString("email") ?: ""

                    RegisterVerifyScreen(navController, email)
                }
                composable("register/onboarding") { OnboardingScreen(navController) }

                composable("chat") { ChatRouterScreen(navController, windowSizeClass) }

                composable("settings") { SettingsScreen(navController) }
                composable("settings/appearance") { AppearanceSettingsScreen(navController) }
                composable("settings/debug") { DebugSettingsScreen(navController) }
                composable("settings/updater") { ClosedBetaUpdaterScreen(navController) }
                composable("settings/changelogs") { ChangelogsSettingsScreen(navController) }
                dialog("settings/feedback") { FeedbackDialog(navController) }

                composable("about") { AboutScreen(navController) }
                composable("about/oss") { AttributionScreen(navController) }
            }
        }
    }
}
