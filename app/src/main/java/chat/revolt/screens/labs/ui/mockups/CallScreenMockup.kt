package chat.revolt.screens.labs.ui.mockups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import chat.revolt.R
import chat.revolt.api.schemas.ChannelType
import chat.revolt.components.screens.chat.ChannelIcon
import chat.revolt.screens.labs.LabsFeature

@LabsFeature
@Composable
fun CallScreenMockup() {
    var showOptions by remember { mutableStateOf(false) }
    var pushToTalk by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val pushToTalkIsHeld by interactionSource.collectIsPressedAsState()

    val pttBackground by animateColorAsState(
        targetValue = if (pushToTalkIsHeld) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = spring(),
        label = "pttBackground"
    )
    val pttText by animateColorAsState(
        targetValue = if (pushToTalkIsHeld) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
        animationSpec = spring(),
        label = "pttText"
    )

    if (showOptions) {
        Dialog(
            onDismissRequest = { showOptions = false }
        ) {
            BoxWithConstraints {
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp)
                        .width(maxWidth * 0.85f)
                        .heightIn(max = maxHeight * 0.85f)
                ) {
                    Row {
                        Checkbox(
                            checked = pushToTalk,
                            onCheckedChange = { pushToTalk = it },
                            modifier = Modifier
                                .padding(16.dp)
                        )
                        Text(
                            text = "Push to talk",
                            modifier = Modifier
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .height(48.dp)
                        .width(12.dp)
                )

                ChannelIcon(
                    channelType = ChannelType.VoiceChannel,
                    modifier = Modifier.alpha(0.6f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Channel",
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.menu),
                        modifier = Modifier
                            .size(18.dp)
                            .alpha(0.4f)
                    )
                }
            }
            IconButton(onClick = {
                showOptions = true
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
        }
        if (pushToTalk) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = {},
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headphones_24dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Galaxy Buds Live",
                    )
                }
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pttBackground,
                        contentColor = pttText
                    ),
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gesture_tap_button_24dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hold to talk",
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!pushToTalk) {
                Button(
                    onClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_microphone_off_24dp),
                        contentDescription = null
                    )
                }
            }

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = "Leave call",
                )
            }

            if (!pushToTalk) {
                Button(
                    onClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headphones_24dp),
                        contentDescription = null
                    )
                }
            }
        }
    }
}