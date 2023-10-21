package chat.revolt.screens.chat.dialogs

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.revolt.BuildConfig
import chat.revolt.R
import chat.revolt.api.REVOLT_BASE
import chat.revolt.api.RevoltAPI
import chat.revolt.api.RevoltHttp
import chat.revolt.components.generic.FormTextField
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class FeedbackType(val value: String) {
    Satisfaction("satisfaction"),
    FeatureRequest("featurerequest"),
    BugReport("bug"),
    UxFeedback("ux"),
    Performance("performance"),
    Security("security"),
    Other("other")
}

@Serializable
data class FeedbackBody(
    val type: String,
    val message: String,
    @SerialName("api_host")
    val apiHost: String,
    @SerialName("app_id")
    val appId: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("app_build")
    val appBuild: String,
    @SerialName("android_api")
    val androidApi: String,
    @SerialName("android_device")
    val androidDevice: String,
    @SerialName("android_manufacturer")
    val androidManufacturer: String,
    @SerialName("id_for_spam_protection_pls_dont_spam_but_if_you_do_i_will_know")
    val author: String
)

suspend fun sendFeedback(type: FeedbackType, message: String): String {
    val response = RevoltHttp.post("${BuildConfig.ANALYSIS_BASEURL}/api/feedback/android") {
        setBody(
            FeedbackBody(
                type = type.value,
                message = message,
                apiHost = REVOLT_BASE,
                appId = BuildConfig.APPLICATION_ID,
                appVersion = BuildConfig.VERSION_NAME,
                appBuild = BuildConfig.VERSION_CODE.toString(),
                androidApi = android.os.Build.VERSION.SDK_INT.toString(),
                androidDevice = android.os.Build.DEVICE,
                androidManufacturer = android.os.Build.MANUFACTURER,
                author = RevoltAPI.selfId ?: "RevoltAPI.selfId is null"
            )
        )
        contentType(ContentType.Application.Json)
    }

    Log.d("FeedbackDialog", "Feedback sent: ${response.bodyAsText()}")

    return response.bodyAsText()
}

@Composable
fun FeedbackDialog(navController: NavController) {
    if (!BuildConfig.ANALYSIS_ENABLED) {
        AlertDialog(onDismissRequest = {
            navController.popBackStack()
        }, title = {
            Text(
                text = stringResource(id = R.string.settings_feedback_disabled_title),
                modifier = Modifier.fillMaxWidth()
            )
        }, text = {
            Text(
                text = stringResource(
                    id = R.string.settings_feedback_disabled_message,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.BUILD_TYPE
                )
            )
        }, confirmButton = {
            TextButton(onClick = {
                navController.popBackStack()
            }) {
                Text(text = stringResource(id = R.string.ok))
            }
        })
        return
    }

    val category = mapOf(
        FeedbackType.Satisfaction to stringResource(R.string.settings_feedback_category_satisfaction),
        FeedbackType.FeatureRequest to stringResource(R.string.settings_feedback_category_feature),
        FeedbackType.BugReport to stringResource(R.string.settings_feedback_category_bug),
        FeedbackType.UxFeedback to stringResource(R.string.settings_feedback_category_ux),
        FeedbackType.Performance to stringResource(R.string.settings_feedback_category_performance),
        FeedbackType.Security to stringResource(R.string.settings_feedback_category_security),
        FeedbackType.Other to stringResource(R.string.settings_feedback_category_other)
    )
    val categoryDropdownExpanded = remember { mutableStateOf(false) }
    val categoryDropdownSelected = remember { mutableStateOf(FeedbackType.Satisfaction) }
    val message = remember { mutableStateOf("") }
    val error = remember { mutableStateOf("") }
    val sending = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            navController.popBackStack()
        },
        title = {
            Text(
                text = stringResource(id = R.string.settings_feedback),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.settings_feedback_introduction)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box {
                    TextField(
                        value = category[categoryDropdownSelected.value]
                            ?: stringResource(id = R.string.unknown),
                        onValueChange = {},
                        label = {
                            Text(
                                text = stringResource(id = R.string.settings_feedback_category)
                            )
                        },
                        readOnly = true,
                        trailingIcon = {
                            IconToggleButton(
                                checked = categoryDropdownExpanded.value,
                                onCheckedChange = {
                                    categoryDropdownExpanded.value = it
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(
                                        id = R.string.settings_feedback_category
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = categoryDropdownExpanded.value,
                        onDismissRequest = {
                            categoryDropdownExpanded.value = false
                        }
                    ) {
                        category.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = {
                                    Text(text = value)
                                },
                                onClick = {
                                    categoryDropdownSelected.value = key
                                    categoryDropdownExpanded.value = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                FormTextField(
                    value = message.value,
                    label = stringResource(id = R.string.settings_feedback_message),
                    onChange = {
                        message.value = it
                    },
                    supportingText = {
                        Text(
                            text = "${message.value.length}/1250"
                        )
                    },
                    enabled = !sending.value,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier.testTag("feedback_cancel"),
                enabled = !sending.value
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (message.value.length > 1250) {
                        error.value =
                            context.getString(R.string.settings_feedback_message_too_long, 1250)
                    } else {
                        error.value = ""
                        sending.value = true
                        scope.launch {
                            try {
                                val result =
                                    sendFeedback(categoryDropdownSelected.value, message.value)
                                Log.d("FeedbackDialog", "Feedback sent with result: $result")

                                if (result.isBlank()) {
                                    navController.popBackStack()
                                } else {
                                    error.value = result
                                    Toast.makeText(context, error.value, Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("FeedbackDialog", "Error sending feedback", e)
                                error.value = context.getString(R.string.settings_feedback_error)
                            } finally {
                                sending.value = false
                            }
                        }
                    }
                },
                enabled = !sending.value && message.value.isNotBlank(),
                modifier = Modifier.testTag("feedback_submit")
            ) {
                Text(text = stringResource(id = R.string.report_submit))
            }
        }
    )
}
