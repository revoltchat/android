package chat.revolt.screens.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.routes.account.MfaResponseRecoveryCode
import chat.revolt.api.routes.account.MfaResponseTotpCode
import chat.revolt.api.routes.account.authenticateWithMfaRecoveryCode
import chat.revolt.api.routes.account.authenticateWithMfaTotpCode
import chat.revolt.components.generic.CollapsibleCard
import chat.revolt.components.generic.FormTextField
import chat.revolt.persistence.KVStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MfaScreenViewModel @Inject constructor(
    private val kvStorage: KVStorage,
) : ViewModel() {
    private var _totpCode by mutableStateOf("")
    val totpCode: String
        get() = _totpCode

    private var _recoveryCode by mutableStateOf("")
    val recoveryCode: String
        get() = _recoveryCode

    private var _error by mutableStateOf<String?>(null)
    val error: String?
        get() = _error

    private var _navigateToHome by mutableStateOf(false)
    val navigateToHome: Boolean
        get() = _navigateToHome

    fun setTotpCode(code: String) {
        _totpCode = code.replace(Regex("[^0-9]"), "")
    }

    fun setRecoveryCode(code: String) {
        _recoveryCode = code
    }

    fun tryAuthorizeTotp(mfaTicket: String) {
        _error = null
        viewModelScope.launch {
            val response = authenticateWithMfaTotpCode(mfaTicket, MfaResponseTotpCode(totpCode))
            if (response.error != null) {
                _error = response.error.type
            } else {
                Log.d(
                    "MFA",
                    "Successfully authorized with TOTP."
                )

                try {
                    RevoltAPI.loginAs(response.firstUserHints!!.token)
                    kvStorage.set("sessionToken", response.firstUserHints.token)

                    _navigateToHome = true
                } catch (e: Error) {
                    _error = e.message ?: "Unknown error"
                }
            }
        }
    }

    fun tryAuthorizeRecovery(mfaTicket: String) {
        _error = null
        viewModelScope.launch {
            val response =
                authenticateWithMfaRecoveryCode(mfaTicket, MfaResponseRecoveryCode(recoveryCode))
            if (response.error != null) {
                _error = response.error.type
            } else {
                Log.d(
                    "MFA",
                    "Successfully authorized with a recovery code."
                )

                try {
                    RevoltAPI.loginAs(response.firstUserHints!!.token)
                    kvStorage.set("sessionToken", response.firstUserHints.token)

                    _navigateToHome = true
                } catch (e: Error) {
                    _error = e.message ?: "Unknown error"
                }
            }
        }
    }
}

@Composable
fun MfaScreen(
    navController: NavController,
    allowedAuthTypesCommaSep: String,
    mfaTicket: String,
    viewModel: MfaScreenViewModel = hiltViewModel()
) {
    val allowedAuthTypes = allowedAuthTypesCommaSep.split(",")

    LaunchedEffect(viewModel.navigateToHome) {
        if (viewModel.navigateToHome) {
            navController.navigate("chat") {
                popUpTo("login/greeting") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .weight(1f)
                .verticalScroll(
                    rememberScrollState()
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(R.string.mfa_interstitial_header),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.mfa_interstitial_lead),
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.5f
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
            )

            if (viewModel.error != null) {
                Text(
                    text = viewModel.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .fillMaxWidth()
                )
            }


            // Collapsible cards for each auth type
            allowedAuthTypes.forEach { authType ->
                when (authType) {
                    "Totp" -> {
                        CollapsibleCard(title = stringResource(R.string.mfa_totp_header)) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.mfa_totp_lead),
                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                        alpha = 0.5f
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                )

                                FormTextField(
                                    label = stringResource(R.string.mfa_totp_code),
                                    onChange = viewModel::setTotpCode,
                                    type = KeyboardType.Number,
                                    value = viewModel.totpCode,
                                )

                                Button(
                                    onClick = { viewModel.tryAuthorizeTotp(mfaTicket) },
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.next),
                                    )
                                }
                            }
                        }
                    }
                    "Recovery" -> {
                        CollapsibleCard(title = stringResource(R.string.mfa_recovery_header)) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.mfa_recovery_lead),
                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                        alpha = 0.5f
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                )

                                FormTextField(
                                    label = stringResource(R.string.mfa_recovery_code),
                                    onChange = viewModel::setRecoveryCode,
                                    value = viewModel.recoveryCode,
                                )

                                Button(
                                    onClick = { viewModel.tryAuthorizeRecovery(mfaTicket) },
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.next),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp)
        ) {
            ElevatedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    }
}