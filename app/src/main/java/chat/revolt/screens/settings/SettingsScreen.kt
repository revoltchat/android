package chat.revolt.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.activities.InviteActivity
import chat.revolt.api.RevoltAPI
import chat.revolt.api.settings.FeatureFlags
import chat.revolt.api.settings.GlobalState
import chat.revolt.components.generic.ListHeader
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Box(Modifier.padding(pv)) {
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
                                contentDescription = null,
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
                                contentDescription = null,
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
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier
                            .testTag("settings_view_appearance")
                            .clickable {
                                navController.navigate("settings/appearance")
                            }
                    )

                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(id = R.string.settings_chat)
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_message_text_24dp),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier
                            .testTag("settings_view_chat")
                            .clickable {
                                navController.navigate("settings/chat")
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
                                contentDescription = null,
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
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier
                                .testTag("settings_view_debug")
                                .clickable {
                                    navController.navigate("settings/debug")
                                }
                        )
                    }

                    if (FeatureFlags.labsAccessControlGranted) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Labs"
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier
                                .testTag("settings_view_labs")
                                .clickable {
                                    navController.navigate("labs")
                                }
                        )
                    }

                    if (GlobalState.experimentsEnabled) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Experiments"
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_flask_24dp),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier
                                .testTag("settings_view_experiments")
                                .clickable {
                                    navController.navigate("settings/experiments")
                                }
                        )
                    }

                    ListHeader {
                        Text(
                            stringResource(
                                R.string.settings_category_last,
                                BuildConfig.VERSION_NAME
                            )
                        )
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
                                contentDescription = null,
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
                        supportingContent = {
                            Text(
                                text = stringResource(id = R.string.settings_feedback_description)
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_comment_quote_24dp),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier
                            .testTag("settings_view_feedback")
                            .clickable {
                                val intent = Intent(
                                    context,
                                    InviteActivity::class.java
                                ).setAction(Intent.ACTION_VIEW)

                                intent.data = "https://rvlt.gg/Testers".toUri()
                                context.startActivity(intent)
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
                                    contentDescription = null,
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
}
