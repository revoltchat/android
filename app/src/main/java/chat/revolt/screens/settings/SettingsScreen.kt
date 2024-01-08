package chat.revolt.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.api.settings.GlobalState
import chat.revolt.api.settings.LabsAccessControlVariates
import chat.revolt.components.generic.ListHeader
import chat.revolt.components.generic.PageHeader
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
                    .padding(vertical = 10.dp)
            ) {
                ListHeader {
                    Text(stringResource(R.string.settings_category_account))
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_profile)
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_card_account_details_24dp),
                            contentDescription = stringResource(id = R.string.settings_profile)
                        )
                    },
                    modifier = Modifier
                        .testTag("settings_view_profile")
                        .clickable {
                            navController.navigate("settings/profile")
                        }
                )

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_sessions)
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_tablet_cellphone_24dp),
                            contentDescription = stringResource(id = R.string.settings_sessions)
                        )
                    },
                    modifier = Modifier
                        .testTag("settings_view_sessions")
                        .clickable {
                            navController.navigate("settings/sessions")
                        }
                )

                ListHeader {
                    Text(stringResource(R.string.settings_category_general))
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_appearance)
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_palette_24dp),
                            contentDescription = stringResource(id = R.string.settings_appearance)
                        )
                    },
                    modifier = Modifier
                        .testTag("settings_view_appearance")
                        .clickable {
                            navController.navigate("settings/appearance")
                        }
                )

                ListHeader {
                    Text(stringResource(R.string.settings_category_miscellaneous))
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.about)
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(id = R.string.about),
                        )
                    },
                    modifier = Modifier
                        .testTag("settings_view_about")
                        .clickable {
                            navController.navigate("about")
                        }
                )

                if (BuildConfig.DEBUG) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Debug"
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Debug",
                            )
                        },
                        modifier = Modifier
                            .testTag("settings_view_debug")
                            .clickable {
                                navController.navigate("settings/debug")
                            }
                    )
                }

                if (FeatureFlags.labsAccessControl is LabsAccessControlVariates.Restricted &&
                    (FeatureFlags.labsAccessControl as LabsAccessControlVariates.Restricted).predicate()
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Labs"
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Labs",
                            )
                        },
                        modifier = Modifier
                            .testTag("settings_view_labs")
                            .clickable {
                                navController.navigate("labs")
                            }
                    )
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = "Closed Beta Updater"
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Closed Beta Updater",
                        )
                    },
                    modifier = Modifier
                        .testTag("settings_view_updater")
                        .clickable {
                            navController.navigate("settings/updater")
                        }
                )

                ListHeader {
                    Text(stringResource(R.string.settings_category_last, BuildConfig.VERSION_NAME))
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_changelogs)
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(id = R.string.settings_changelogs),
                        )
                    },
                    modifier = Modifier
                        .testTag("settings_view_changelogs")
                        .clickable {
                            navController.navigate("settings/changelogs")
                        }
                )

                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(id = R.string.settings_feedback)
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = stringResource(id = R.string.settings_feedback),
                        )
                    },
                    modifier = Modifier
                        .testTag("settings_view_feedback")
                        .clickable {
                            navController.navigate("settings/feedback")
                        }
                )

                ListItem(
                    headlineContent = {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                            Text(
                                text = stringResource(id = R.string.logout)
                            )
                        }
                    },
                    leadingContent = {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.logout),
                            )
                        }
                    },
                    modifier = Modifier
                        .testTag("settings_view_logout")
                        .clickable {
                            viewModel.logout()
                            navController.navigate("login/greeting") {
                                popUpTo("chat") {
                                    inclusive = true
                                }
                            }
                        }
                )
            }
        }
    }
}
