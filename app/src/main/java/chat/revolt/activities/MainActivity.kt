package chat.revolt.activities

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.RevoltApplication
import chat.revolt.api.HitRateLimitException
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.api.routes.onboard.needsOnboarding
import chat.revolt.api.settings.GlobalState
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.ndk.NativeLibraries
import chat.revolt.persistence.KVStorage
import chat.revolt.screens.DefaultDestinationScreen
import chat.revolt.screens.SplashScreen
import chat.revolt.screens.about.AboutScreen
import chat.revolt.screens.about.AttributionScreen
import chat.revolt.screens.chat.ChatRouterScreen
import chat.revolt.screens.create.CreateGroupScreen
import chat.revolt.screens.labs.LabsRootScreen
import chat.revolt.screens.login.LoginGreetingScreen
import chat.revolt.screens.login.LoginScreen
import chat.revolt.screens.login.MfaScreen
import chat.revolt.screens.register.OnboardingScreen
import chat.revolt.screens.register.RegisterDetailsScreen
import chat.revolt.screens.register.RegisterGreetingScreen
import chat.revolt.screens.register.RegisterVerifyScreen
import chat.revolt.screens.services.DiscoverScreen
import chat.revolt.screens.settings.AppearanceSettingsScreen
import chat.revolt.screens.settings.ChangelogsSettingsScreen
import chat.revolt.screens.settings.ChatSettingsScreen
import chat.revolt.screens.settings.DebugSettingsScreen
import chat.revolt.screens.settings.ProfileSettingsScreen
import chat.revolt.screens.settings.SessionSettingsScreen
import chat.revolt.screens.settings.SettingsScreen
import chat.revolt.screens.settings.channel.ChannelSettingsHome
import chat.revolt.screens.settings.channel.ChannelSettingsOverview
import chat.revolt.screens.settings.channel.ChannelSettingsPermissions
import chat.revolt.ui.theme.RevoltTheme
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.request.get
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MainActivityViewModel @Inject constructor(
    private val kvStorage: KVStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val nextDestination = MutableStateFlow<String?>(null)
    var isConnected = MutableStateFlow(false)
    val isReady = MutableStateFlow(false)

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> true
            else -> false
        }
    }

    private suspend fun canReachRevolt(): Boolean {
        val res = RevoltHttp.get("/")
        return res.status.value == 200
    }

    private suspend fun startWithDestination(destination: String) {
        nextDestination.emit(destination)
        isReady.emit(true)
    }

    private suspend fun startWithoutDestination() {
        isReady.emit(true)
    }

    fun checkLoggedInState() {
        viewModelScope.launch {
            Log.d("MainActivity", "Checking logged in state")

            isConnected.emit(hasInternetConnection())

            Log.d("MainActivity", "Checking if we can reach Revolt")

            if (!isConnected.value) return@launch startWithoutDestination()

            Log.d("MainActivity", "We can reach Revolt, checking if we're logged in")

            val token = kvStorage.get("sessionToken")
                ?: return@launch startWithDestination("login/greeting")
            val id = kvStorage.get("sessionId") ?: ""

            Log.d(
                "MainActivity",
                "We have a session token, checking if it's valid and if we can still reach Revolt"
            )

            val canReachRevolt = canReachRevolt()
            val valid = try {
                RevoltAPI.checkSessionToken(token)
            } catch (e: Throwable) {
                false
            }

            if (canReachRevolt && !valid) {
                Log.d("MainActivity", "Session token is invalid, clearing session")
                Toast.makeText(
                    context,
                    context.getString(R.string.token_invalid_toast),
                    Toast.LENGTH_SHORT
                ).show()
                kvStorage.remove("sessionToken")
                kvStorage.remove("sessionId")
                startWithDestination("login/greeting")
            } else {
                try {
                    Log.d("MainActivity", "Session token is valid, checking onboarding state")
                    val onboard = needsOnboarding(token)
                    if (onboard) {
                        Log.d("MainActivity", "Onboarding state is incomplete, starting onboarding")
                        startWithDestination("register/onboarding")
                        return@launch
                    }
                } catch (e: HitRateLimitException) {
                    Log.e("MainActivity", "Rate limited while checking onboarding state", e)
                    Toast.makeText(
                        context,
                        context.getString(R.string.rate_limit_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch startWithoutDestination()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to check onboarding state, clearing session", e)
                    kvStorage.remove("sessionToken")
                    kvStorage.remove("sessionId")
                    startWithDestination("login/greeting")
                }

                try {
                    Log.d("MainActivity", "Onboarding state is complete, logging in")
                    RevoltAPI.loginAs(token)
                    RevoltAPI.setSessionId(id)
                    startWithDestination("chat")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to login, clearing session", e)
                    kvStorage.remove("sessionToken")
                    kvStorage.remove("sessionId")
                    startWithDestination("login/greeting")
                }
            }
        }
    }

    fun updateNextDestination(destination: String) {
        viewModelScope.launch {
            nextDestination.emit(null)
            nextDestination.emit(destination)
        }
    }

    init {
        Log.d("MainActivity", "Starting up")
        checkLoggedInState()
    }
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()

    // Fix for SDK >=31, where core-splashscreen accidentally removes dynamic colours
    // See the other one in DefaultDestinationScreen.kt
    override fun onResume() {
        super.onResume()
        DynamicColors.applyToActivityIfAvailable(this)
        DynamicColors.applyToActivitiesIfAvailable(RevoltApplication.instance)
        window.statusBarColor = Color.Transparent.toArgb()
    }

    // Same as above for configuration changes (rotation, dark mode, etc.)
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        DynamicColors.applyToActivityIfAvailable(this)
        DynamicColors.applyToActivitiesIfAvailable(RevoltApplication.instance)
        window.statusBarColor = Color.Transparent.toArgb()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
        }

        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !viewModel.isReady.value
            }
        }

        RevoltAPI.hydrateFromPersistentCache()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            AppEntrypoint(
                windowSizeClass,
                viewModel.nextDestination.collectAsState().value,
                viewModel.isConnected.collectAsState().value,
                viewModel::checkLoggedInState,
                viewModel::updateNextDestination
            )
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

val NavTweenInt: FiniteAnimationSpec<IntOffset> = tween(350, easing = EaseInOutExpo)
val NavTweenFloat: FiniteAnimationSpec<Float> = tween(350, easing = EaseInOutExpo)

@Composable
fun AppEntrypoint(
    windowSizeClass: WindowSizeClass,
    nextDestination: String?,
    isConnected: Boolean,
    onRetryConnection: () -> Unit,
    onUpdateNextDestination: (String) -> Unit = {}
) {
    val navController = rememberNavController()

    RevoltTheme(
        requestedTheme = GlobalState.theme,
        colourOverrides = SyncedSettings.android.colourOverrides
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = "default",
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = NavTweenInt,
                        initialOffset = { it / 3 }
                    ) + fadeIn(animationSpec = NavTweenFloat)
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = NavTweenInt,
                        targetOffset = { it / 3 }
                    ) + fadeOut(animationSpec = NavTweenFloat)
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = NavTweenInt,
                        initialOffset = { it / 3 }
                    ) + fadeIn(animationSpec = NavTweenFloat)
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = NavTweenInt,
                        targetOffset = { it / 2 }
                    ) + fadeOut(animationSpec = NavTweenFloat)
                }
            ) {
                composable("default") {
                    DefaultDestinationScreen(
                        navController,
                        nextDestination,
                        isConnected,
                        onRetryConnection
                    )
                }
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
                composable("register/onboarding") {
                    OnboardingScreen(
                        navController,
                        onOnboardingComplete = {
                            onUpdateNextDestination("chat")
                            navController.popBackStack(
                                navController.graph.startDestinationRoute!!,
                                inclusive = true
                            )
                            navController.navigate("default")
                        }
                    )
                }

                composable("chat") {
                    ChatRouterScreen(
                        navController,
                        windowSizeClass,
                        onNullifiedUser = {
                            onRetryConnection()
                            navController.popBackStack(
                                navController.graph.startDestinationRoute!!,
                                inclusive = true
                            )
                            navController.navigate("default")
                        }
                    )
                }

                composable("create/group") { CreateGroupScreen(navController) }

                composable("discover") { DiscoverScreen(navController) }

                composable("settings") { SettingsScreen(navController) }
                composable("settings/profile") { ProfileSettingsScreen(navController) }
                composable("settings/sessions") { SessionSettingsScreen(navController) }
                composable("settings/appearance") { AppearanceSettingsScreen(navController) }
                composable("settings/chat") { ChatSettingsScreen(navController) }
                composable("settings/debug") { DebugSettingsScreen(navController) }
                composable("settings/changelogs") { ChangelogsSettingsScreen(navController) }

                composable("settings/channel/{channelId}") { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    ChannelSettingsHome(navController, channelId)
                }
                composable("settings/channel/{channelId}/overview") { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    ChannelSettingsOverview(navController, channelId)
                }
                composable("settings/channel/{channelId}/permissions") { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
                    ChannelSettingsPermissions(navController, channelId)
                }

                composable("about") { AboutScreen(navController) }
                composable("about/oss") { AttributionScreen(navController) }

                composable("labs") { LabsRootScreen(navController) }
            }
        }
    }
}
