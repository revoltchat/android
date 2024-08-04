package chat.revolt.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.components.generic.SheetEnd

enum class ColourPickerMode {
    Sliders,
    Palette,
    Hex
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.ColourPickerSheet(initialValue: Int, onColourSelected: (Int) -> Unit) {
    var selectedColour by remember { mutableIntStateOf(initialValue and 0xFFFFFF) }

    var mode by remember { mutableStateOf(ColourPickerMode.Sliders) }

    val hueComponent by remember(selectedColour) { derivedStateOf { selectedColour shr 16 and 0xFF } }
    val saturationComponent by remember(selectedColour) { derivedStateOf { selectedColour shr 8 and 0xFF } }
    val valueComponent by remember(selectedColour) { derivedStateOf { selectedColour and 0xFF } }

    val hueTrackColours = remember {
        (0..255).map {
            Color(
                android.graphics.Color.HSVToColor(
                    floatArrayOf(
                        it.toFloat(),
                        1f,
                        1f
                    )
                )
            )
        }
    }

    var pendingHexColourString by remember(selectedColour) {
        // First we convert the colour from HHSSVV to #RRGGBB.
        val asRgb = android.graphics.Color.HSVToColor(
            floatArrayOf(
                hueComponent.toFloat(),
                saturationComponent / 255f,
                valueComponent / 255f
            )
        )
        mutableStateOf("#${asRgb.toString(16).padStart(6, '0')}")
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == ColourPickerMode.Sliders,
                onClick = { mode = ColourPickerMode.Sliders },
                shape = CircleShape.copy(
                    topEnd = CornerSize(0),
                    bottomEnd = CornerSize(0)
                )
            ) {
                Text(stringResource(R.string.colour_picker_mode_sliders))
            }
            SegmentedButton(
                selected = mode == ColourPickerMode.Palette,
                onClick = { mode = ColourPickerMode.Palette },
                shape = RectangleShape
            ) {
                Text(stringResource(R.string.colour_picker_mode_palette))
            }
            SegmentedButton(
                selected = mode == ColourPickerMode.Hex,
                onClick = { mode = ColourPickerMode.Hex },
                shape = CircleShape.copy(
                    topStart = CornerSize(0),
                    bottomStart = CornerSize(0)
                )
            ) {
                Text(stringResource(R.string.colour_picker_hex))
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(targetState = mode, label = "picker mode") { pickerMode ->
            when (pickerMode) {
                ColourPickerMode.Sliders -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.colour_picker_hue),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                hueComponent.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        Slider(
                            value = hueComponent.toFloat(),
                            onValueChange = {
                                selectedColour =
                                    (selectedColour and 0x00FFFF) or (it.toInt() shl 16)
                            },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors().copy(
                                // The thumb colour is the current hue at full saturation and value.
                                thumbColor = Color(
                                    android.graphics.Color.HSVToColor(
                                        floatArrayOf(
                                            hueComponent.toFloat(),
                                            1f,
                                            1f
                                        )
                                    )
                                )
                            ),
                            track = {
                                Canvas(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                ) {
                                    drawRect(
                                        Brush.horizontalGradient(hueTrackColours, endX = size.width)
                                    )
                                }
                            }
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.colour_picker_saturation),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                saturationComponent.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        Slider(
                            value = saturationComponent.toFloat(),
                            onValueChange = {
                                selectedColour =
                                    (selectedColour and 0xFF00FF) or (it.toInt() shl 8)
                            },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors().copy(
                                thumbColor = Color(
                                    android.graphics.Color.HSVToColor(
                                        floatArrayOf(
                                            hueComponent.toFloat(),
                                            (selectedColour shr 8 and 0xFF) / 255f,
                                            1f
                                        )
                                    )
                                )
                            ),
                            track = {
                                Canvas(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                ) {
                                    drawRect(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(
                                                    android.graphics.Color.HSVToColor(
                                                        floatArrayOf(
                                                            hueComponent.toFloat(),
                                                            0f,
                                                            1f
                                                        )
                                                    )
                                                ),
                                                Color(
                                                    android.graphics.Color.HSVToColor(
                                                        floatArrayOf(
                                                            hueComponent.toFloat(),
                                                            1f,
                                                            1f
                                                        )
                                                    )
                                                )
                                            ),
                                            endX = size.width
                                        )
                                    )
                                }
                            }
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.colour_picker_value),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                valueComponent.toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        Slider(
                            value = valueComponent.toFloat(),
                            onValueChange = {
                                selectedColour =
                                    (selectedColour and 0xFFFF00) or it.toInt()
                            },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors().copy(
                                thumbColor = Color(
                                    android.graphics.Color.HSVToColor(
                                        floatArrayOf(
                                            hueComponent.toFloat(),
                                            (selectedColour shr 8 and 0xFF) / 255f,
                                            (selectedColour and 0xFF) / 255f
                                        )
                                    )
                                )
                            ),
                            track = {
                                Canvas(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                ) {
                                    drawRect(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(
                                                    android.graphics.Color.HSVToColor(
                                                        floatArrayOf(
                                                            hueComponent.toFloat(),
                                                            (selectedColour shr 8 and 0xFF) / 255f,
                                                            0f
                                                        )
                                                    )
                                                ),
                                                Color(
                                                    android.graphics.Color.HSVToColor(
                                                        floatArrayOf(
                                                            hueComponent.toFloat(),
                                                            (selectedColour shr 8 and 0xFF) / 255f,
                                                            1f
                                                        )
                                                    )
                                                )
                                            ),
                                            endX = size.width
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                ColourPickerMode.Palette -> {
                    Text("TODO: Palette picker", Modifier.fillMaxWidth())
                }

                ColourPickerMode.Hex -> {
                    OutlinedTextField(
                        value = pendingHexColourString,
                        onValueChange = {
                            pendingHexColourString = it

                            if ("#[0-9a-fA-F]{6}".toRegex().matches(it)) {
                                val newColour =
                                    it.substring(1).toIntOrNull(16) ?: return@OutlinedTextField
                                val floatArr = FloatArray(3)
                                android.graphics.Color.RGBToHSV(
                                    newColour shr 16 and 0xFF,
                                    newColour shr 8 and 0xFF,
                                    newColour and 0xFF,
                                    floatArr
                                )
                                selectedColour = floatArr.fold(0) { acc, f ->
                                    (acc shl 8) or (f.toInt() and 0xFF)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.colour_picker_preview),
            style = MaterialTheme.typography.labelLarge
        )

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            drawRect(
                Color(
                    android.graphics.Color.HSVToColor(
                        floatArrayOf(
                            hueComponent.toFloat(),
                            saturationComponent / 255f,
                            valueComponent / 255f
                        )
                    )
                ),
                size = size
            )
        }
    }

    SheetEnd()
}