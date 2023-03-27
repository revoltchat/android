package chat.revolt.screens.chat.dialogs.safety

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.safety.putMessageReport
import chat.revolt.api.routes.user.blockUser
import chat.revolt.api.schemas.ContentReportReason
import chat.revolt.components.chat.Message
import chat.revolt.components.generic.FormTextField
import kotlinx.coroutines.launch

enum class ReportFlowState {
    Reason,
    Sending,
    Done,
    Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMessageDialog(
    navController: NavController,
    messageId: String,
) {
    val message = RevoltAPI.messageCache[messageId]
    if (message == null) {
        navController.popBackStack()
        return
    }

    val author = RevoltAPI.userCache[message.author]
    val messageIsBridged = author?.let { author.bot != null && message.masquerade != null } ?: false

    val state = remember { mutableStateOf(ReportFlowState.Reason) }

    val selectedReason = remember { mutableStateOf("Illegal") }
    val userAddedContext = remember { mutableStateOf("") }

    when (state.value) {
        ReportFlowState.Reason -> {
            val reasons = mapOf(
                "Illegal" to stringResource(id = R.string.report_reason_content_illegal),
                "PromotesHarm" to stringResource(id = R.string.report_reason_content_promotes_harm),
                "SpamAbuse" to stringResource(id = R.string.report_reason_content_spam_abuse),
                "Malware" to stringResource(id = R.string.report_reason_content_malware),
                "Harassment" to stringResource(id = R.string.report_reason_content_harassment),
                "Other" to stringResource(id = R.string.report_reason_content_other),
            )
            val reasonDropdownExpanded = remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    // nothing - prevent mistaps from closing the dialog
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.report),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Text(text = stringResource(id = R.string.report_message))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(id = R.string.report_message_preview),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                .verticalScroll(rememberScrollState())
                                .heightIn(max = 200.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            Message(
                                message = message.copy(
                                    tail = false,
                                    masquerade = null
                                ),
                                truncate = false
                            )
                        }

                        if (messageIsBridged) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.report_message_bridge_notice),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box {
                            TextField(
                                value = reasons[selectedReason.value]
                                    ?: stringResource(id = R.string.unknown),
                                onValueChange = {
                                    selectedReason.value = it
                                },
                                label = {
                                    Text(
                                        text = stringResource(id = R.string.report_reason),
                                    )
                                },
                                readOnly = true,
                                trailingIcon = {
                                    IconToggleButton(
                                        checked = reasonDropdownExpanded.value,
                                        onCheckedChange = {
                                            reasonDropdownExpanded.value = it
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = stringResource(id = R.string.report_reason)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            DropdownMenu(
                                expanded = reasonDropdownExpanded.value,
                                onDismissRequest = {
                                    reasonDropdownExpanded.value = false
                                },
                            ) {
                                reasons.forEach { (key, value) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = value)
                                        },
                                        onClick = {
                                            selectedReason.value = key
                                            reasonDropdownExpanded.value = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FormTextField(
                            value = userAddedContext.value,
                            label = stringResource(id = R.string.report_reason_additional),
                            onChange = {
                                userAddedContext.value = it
                            },
                            supportingText = {
                                Text(
                                    text = stringResource(id = R.string.report_reason_additional_hint)
                                )
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.testTag("report_cancel")
                    ) {
                        Text(text = stringResource(id = R.string.report_cancel))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            state.value = ReportFlowState.Sending
                        },
                        modifier = Modifier.testTag("report_send")
                    ) {
                        Text(text = stringResource(id = R.string.report_submit))
                    }
                }
            )
        }

        ReportFlowState.Sending -> {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = stringResource(id = R.string.report_submitting),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        LaunchedEffect(true) {
                            launch {
                                try {
                                    Log.d("ReportMessageDialog", "Reporting message $messageId")
                                    putMessageReport(
                                        messageId,
                                        ContentReportReason.valueOf(selectedReason.value),
                                        userAddedContext.value
                                    )
                                    state.value = ReportFlowState.Done
                                } catch (e: Exception) {
                                    state.value = ReportFlowState.Error
                                    Log.e("ReportMessageDialog", "Failed to report message", e)
                                    return@launch
                                }
                            }
                        }
                    }
                },
                dismissButton = {},
                confirmButton = {}
            )
        }

        ReportFlowState.Done -> {
            val scope = rememberCoroutineScope()

            AlertDialog(
                onDismissRequest = {
                    navController.popBackStack()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null, // decorative
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.report_submit_success),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                text = {
                    Column() {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.report_submit_thanks),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = stringResource(id = R.string.report_block_question),
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.testTag("report_block_no")
                    ) {
                        Text(text = stringResource(id = R.string.report_block_no))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                blockUser(message.author ?: return@launch)
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier.testTag("report_block_yes")
                    ) {
                        Text(text = stringResource(id = R.string.report_block_yes))
                    }
                }
            )
        }

        ReportFlowState.Error -> {
            AlertDialog(
                onDismissRequest = {
                    navController.popBackStack()
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null, // decorative
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(id = R.string.report_submit_error_header),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                text = {
                    Column() {
                        Text(
                            text = stringResource(id = R.string.report_submit_error),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.testTag("report_error_ok")
                    ) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                },
                confirmButton = {}
            )
        }
    }
}