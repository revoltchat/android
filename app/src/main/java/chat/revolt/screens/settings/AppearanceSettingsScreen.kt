package chat.revolt.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.settings.GlobalState
import chat.revolt.api.settings.SyncedSettings
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.screens.settings.appearance.ColourChip
import chat.revolt.ui.theme.ClearRippleTheme
import chat.revolt.ui.theme.Theme
import chat.revolt.ui.theme.systemSupportsDynamicColors
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.launch
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class AppearanceSettingsScreenViewModel : ViewModel() {
    var showColourOverrides by mutableStateOf(false)
    var selectedOverrideName by mutableStateOf<String?>(null)
    var selectedOverrideInitialValue by mutableStateOf<Int?>(null)
    var overridePickerSheetVisible by mutableStateOf(false)

    fun saveNewTheme(theme: Theme) {
        GlobalState.theme = theme
        viewModelScope.launch {
            SyncedSettings.updateAndroid(SyncedSettings.android.copy(theme = theme.name))
        }
    }

    fun updateColourOverrides(fieldName: String, value: Int?) {
        viewModelScope.launch {
            val overrides = SyncedSettings.android.copy().colourOverrides

            if (overrides != null) {
                val mutOverrides = overrides.toMutableMap()
                if (value == null) {
                    mutOverrides.remove(fieldName)
                } else {
                    mutOverrides[fieldName] = value
                }

                SyncedSettings.updateAndroid(
                    SyncedSettings.android.copy(
                        colourOverrides = mutOverrides
                    )
                )
            } else if (value != null) {
                SyncedSettings.updateAndroid(
                    SyncedSettings.android.copy(
                        colourOverrides = mapOf(
                            fieldName to value
                        )
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    navController: NavController,
    viewModel: AppearanceSettingsScreenViewModel = viewModel()
) {
    val colourOverridesOpenerArrowRotation by animateFloatAsState(
        if (viewModel.showColourOverrides) {
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) 90f else -90f
        } else 0f,
        label = "colourOverridesOpenerArrowRotation"
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (viewModel.overridePickerSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                viewModel.overridePickerSheetVisible = false
            }
        ) {
            ColourSelectorSheet(
                initialValue = Color(viewModel.selectedOverrideInitialValue ?: 0),
                onConfirm = { color ->
                    viewModel.updateColourOverrides(
                        viewModel.selectedOverrideName ?: return@ColourSelectorSheet,
                        color?.toArgb()
                    )
                    scope.launch {
                        sheetState.hide()
                        viewModel.overridePickerSheetVisible = false
                    }
                },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        viewModel.overridePickerSheetVisible = false
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        PageHeader(
            text = stringResource(id = R.string.settings_appearance),
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
            Text(
                text = stringResource(id = R.string.settings_appearance_theme),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp)
            ) {
                ColourChip(
                    color = Color(0xff1c243c),
                    text = stringResource(id = R.string.settings_appearance_theme_revolt),
                    selected = GlobalState.theme == Theme.Revolt,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("set_theme_revolt")
                ) {
                    viewModel.saveNewTheme(Theme.Revolt)
                }

                ColourChip(
                    color = Color(0xfff7f7f7),
                    text = stringResource(id = R.string.settings_appearance_theme_light),
                    selected = GlobalState.theme == Theme.Light,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("set_theme_light")
                ) {
                    viewModel.saveNewTheme(Theme.Light)
                }

                ColourChip(
                    color = Color(0xff000000),
                    text = stringResource(id = R.string.settings_appearance_theme_amoled),
                    selected = GlobalState.theme == Theme.Amoled,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("set_theme_amoled")
                ) {
                    viewModel.saveNewTheme(Theme.Amoled)
                }

                ColourChip(
                    color = if (isSystemInDarkTheme()) Color(0xff1c243c) else Color(0xfff7f7f7),
                    text = stringResource(id = R.string.settings_appearance_theme_none),
                    selected = GlobalState.theme == Theme.None,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("set_theme_none")
                ) {
                    viewModel.saveNewTheme(Theme.None)
                }

                if (systemSupportsDynamicColors()) {
                    ColourChip(
                        color = dynamicDarkColorScheme(LocalContext.current).primary,
                        text = stringResource(id = R.string.settings_appearance_theme_m3dynamic),
                        selected = GlobalState.theme == Theme.M3Dynamic,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_theme_m3dynamic")
                    ) {
                        viewModel.saveNewTheme(Theme.M3Dynamic)
                    }
                } else {
                    ColourChip(
                        color = Color(0xffa0a0a0),
                        text = stringResource(
                            id = R.string.settings_appearance_theme_m3dynamic_unsupported
                        ),
                        selected = false,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("set_theme_m3dynamic_unsupported")
                    ) {
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.settings_appearance_theme_m3dynamic_unsupported_toast
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .clickable {
                        viewModel.showColourOverrides = !viewModel.showColourOverrides
                    }
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 20.dp, end = 4.dp)
                            .rotate(colourOverridesOpenerArrowRotation)
                    )
                }

                Text(
                    text = stringResource(id = R.string.settings_appearance_colour_overrides),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .weight(1f)
                )

                if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp, end = 20.dp)
                            .rotate(colourOverridesOpenerArrowRotation)
                    )
                }
            }

            AnimatedVisibility(viewModel.showColourOverrides) {
                Column {
                    ColorScheme::class.memberProperties.forEach { member ->
                        if (member.visibility != KVisibility.PUBLIC) return@forEach

                        val name = member.name
                        val value = member.getter.call(MaterialTheme.colorScheme) as Color

                        ColourChip(
                            color = value,
                            text = try {
                                R.string::class.java.getField("settings_appearance_colour_overrides_${name.toSnakeCase()}")
                                    .getInt(null)
                                    .let { context.getString(it) }
                            } catch (e: Exception) {
                                name
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp)
                                .testTag("set_colour_override_$name")
                        ) {
                            viewModel.selectedOverrideName = name
                            viewModel.selectedOverrideInitialValue = value.toArgb()
                            viewModel.overridePickerSheetVisible = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColourSelectorSheet(
    initialValue: Color,
    onConfirm: (Color?) -> Unit,
    onDismiss: () -> Unit
) {
    val controller = rememberColorPickerController()
    val colour = remember { mutableStateOf(initialValue) }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .padding(10.dp),
            controller = controller,
            onColorChanged = { colorEnvelope: ColorEnvelope ->
                colour.value = colorEnvelope.color
            },
        )

        AlphaSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            controller = controller,
        )

        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            controller = controller,
        )

        CompositionLocalProvider(
            LocalRippleTheme provides ClearRippleTheme
        ) {
            ColourChip(
                color = colour.value,
                text = "#${
                    (0xFFFFFF and colour.value.toArgb()).toString(16).padStart(6, '0').uppercase()
                }",
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            ) {}
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(
                onClick = {
                    onConfirm(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(id = R.string.settings_appearance_colour_overrides_reset)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.cancel)
                    )
                }

                Button(
                    onClick = {
                        onConfirm(colour.value)
                    },
                    enabled = colour.value != initialValue,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.settings_appearance_colour_overrides_apply)
                    )
                }
            }
        }
    }
}

fun String.toSnakeCase(): String {
    return this.replace(Regex("([a-z])([A-Z]+)"), "$1_$2").lowercase()
}