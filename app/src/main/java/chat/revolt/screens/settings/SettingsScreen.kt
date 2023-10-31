package chat.revolt.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.settings.GlobalState
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.generic.SheetClickable
import chat.revolt.components.screens.settings.SelfUserOverview
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage
) : ViewModel() {
    fun logout() {
        runBlocking {
            kvStorage.remove("sessionToken")
            GlobalState.reset()
            RevoltAPI.logout()
        }
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsScreenViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        PageHeader(
            text = stringResource(id = R.string.settings),
            showBackButton = true,
            onBackButtonClicked = {
                navController.popBackStack()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SelfUserOverview()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_category_account),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp, start = 10.dp, top = 20.dp)
                )

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            painter = painterResource(R.drawable.ic_card_account_details_24dp),
                            contentDescription = stringResource(id = R.string.settings_profile),
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(
                            text = stringResource(id = R.string.settings_profile),
                            style = textStyle
                        )
                    },
                    modifier = Modifier.testTag("settings_view_profile")
                ) {
                    navController.navigate("settings/profile")
                }

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            painter = painterResource(R.drawable.ic_tablet_cellphone_24dp),
                            contentDescription = stringResource(id = R.string.settings_sessions),
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(
                            text = stringResource(id = R.string.settings_sessions),
                            style = textStyle
                        )
                    },
                    modifier = Modifier.testTag("settings_view_sessions")
                ) {
                    navController.navigate("settings/sessions")
                }

                Text(
                    text = stringResource(id = R.string.settings_category_general),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp, start = 10.dp, top = 20.dp)
                )

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            painter = painterResource(id = R.drawable.ic_palette_24dp),
                            contentDescription =
                            stringResource(id = R.string.settings_appearance),
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(
                            text = stringResource(id = R.string.settings_appearance),
                            style = textStyle
                        )
                    },
                    modifier = Modifier.testTag("settings_view_appearance")
                ) {
                    navController.navigate("settings/appearance")
                }

                Text(
                    text = stringResource(id = R.string.settings_category_miscellaneous),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp, start = 10.dp, top = 20.dp)
                )

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(id = R.string.about),
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(text = stringResource(id = R.string.about), style = textStyle)
                    },
                    modifier = Modifier.testTag("settings_view_about")
                ) {
                    navController.navigate("about")
                }

                if (BuildConfig.DEBUG) {
                    SheetClickable(
                        icon = { modifier ->
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Debug",
                                modifier = modifier
                            )
                        },
                        label = { textStyle ->
                            Text(text = "Debug", style = textStyle)
                        },
                        modifier = Modifier.testTag("settings_view_debug")
                    ) {
                        navController.navigate("settings/debug")
                    }
                }

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Closed Beta Updater",
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(text = "Closed Beta Updater", style = textStyle)
                    },
                    modifier = Modifier.testTag("settings_view_updater")
                ) {
                    navController.navigate("settings/updater")
                }

                Text(
                    text = stringResource(
                        id = R.string.settings_category_last,
                        BuildConfig.VERSION_NAME
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp, start = 10.dp, top = 20.dp)
                )

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(id = R.string.settings_changelogs),
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(
                            text = stringResource(id = R.string.settings_changelogs),
                            style = textStyle
                        )
                    },
                    modifier = Modifier.testTag("settings_view_changelogs")
                ) {
                    navController.navigate("settings/changelogs")
                }

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = stringResource(id = R.string.settings_feedback),
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(
                            text = stringResource(id = R.string.settings_feedback),
                            style = textStyle
                        )
                    },
                    modifier = Modifier.testTag("settings_view_feedback")
                ) {
                    navController.navigate("settings/feedback")
                }

                SheetClickable(
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.logout),
                            modifier = modifier
                        )
                    },
                    label = { textStyle ->
                        Text(text = stringResource(id = R.string.logout), style = textStyle)
                    },
                    dangerous = true,
                    modifier = Modifier.testTag("settings_view_logout")
                ) {
                    viewModel.logout()
                    navController.navigate("login/greeting") {
                        popUpTo("chat") {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }
}
