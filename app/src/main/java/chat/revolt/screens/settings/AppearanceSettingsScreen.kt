package chat.revolt.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.settings.GlobalState
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.settings.appearance.ThemeChip
import chat.revolt.ui.theme.Theme
import chat.revolt.ui.theme.systemSupportsDynamicColors
import com.google.accompanist.flowlayout.FlowRow
import kotlinx.coroutines.launch

class AppearanceSettingsScreenViewModel : ViewModel() {
    fun saveNewTheme(theme: Theme) {
        viewModelScope.launch {
            val android = SyncedSettings.android
            android.theme = theme.toString()
            SyncedSettings.updateAndroid(android)
        }
    }
}

@Composable
fun AppearanceSettingsScreen(
    navController: NavController,
    viewModel: AppearanceSettingsScreenViewModel = viewModel()
) {
    val context = LocalContext.current

    fun setNewTheme(theme: Theme) {
        GlobalState.setTheme(theme)
        viewModel.saveNewTheme(theme)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        PageHeader(
            text = stringResource(id = R.string.settings_appearance),
            showBackButton = true,
            onBackButtonClicked = {
                navController.popBackStack()
            })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.settings_appearance_theme),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Text(
                text = "old revolt blue will come back soon i promise, needs a bit of optimisation first 🐈",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.background,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(10.dp)
            )

            FlowRow(
                mainAxisSpacing = 10.dp,
                crossAxisSpacing = 10.dp,
            ) {
                ThemeChip(
                    color = Color(0xff1e1e1e),
                    text = stringResource(id = R.string.settings_appearance_theme_revolt),
                    selected = GlobalState.theme == Theme.Revolt,
                    modifier = Modifier.weight(1f),
                ) {
                    setNewTheme(Theme.Revolt)
                }

                ThemeChip(
                    color = Color(0xfff7f7f7),
                    text = stringResource(id = R.string.settings_appearance_theme_light),
                    selected = GlobalState.theme == Theme.Light,
                    modifier = Modifier.weight(1f),
                ) {
                    setNewTheme(Theme.Light)
                }

                ThemeChip(
                    color = Color(0xff000000),
                    text = stringResource(id = R.string.settings_appearance_theme_amoled),
                    selected = GlobalState.theme == Theme.Amoled,
                    modifier = Modifier.weight(1f),
                ) {
                    setNewTheme(Theme.Amoled)
                }

                ThemeChip(
                    color = if (isSystemInDarkTheme()) Color(0xff1e1e1e) else Color(0xfff7f7f7),
                    text = stringResource(id = R.string.settings_appearance_theme_none),
                    selected = GlobalState.theme == Theme.None,
                    modifier = Modifier.weight(1f),
                ) {
                    setNewTheme(Theme.None)
                }

                if (systemSupportsDynamicColors()) {
                    ThemeChip(
                        color = dynamicDarkColorScheme(LocalContext.current).primary,
                        text = stringResource(id = R.string.settings_appearance_theme_m3dynamic),
                        selected = GlobalState.theme == Theme.M3Dynamic,
                        modifier = Modifier.weight(1f),
                    ) {
                        setNewTheme(Theme.M3Dynamic)
                    }
                } else {
                    ThemeChip(
                        color = Color(0xffa0a0a0),
                        text = stringResource(id = R.string.settings_appearance_theme_m3dynamic_unsupported),
                        selected = false,
                        modifier = Modifier.weight(1f),
                    ) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_appearance_theme_m3dynamic_unsupported_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}