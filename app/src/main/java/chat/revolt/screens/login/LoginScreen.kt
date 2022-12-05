package chat.revolt.screens.login

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.REVOLT_SUPPORT
import chat.revolt.api.routes.account.EmailPasswordAssessment
import chat.revolt.api.routes.account.negotiateAuthentication
import chat.revolt.components.generic.AnyLink
import chat.revolt.components.generic.FormTextField
import chat.revolt.components.generic.Weblink
import kotlinx.coroutines.launch

class LoginViewModel() : ViewModel() {
    private var _email by mutableStateOf("")
    val email: String
        get() = _email

    private var _password by mutableStateOf("")
    val password: String
        get() = _password

    private var _error by mutableStateOf<String?>(null)
    val error: String?
        get() = _error

    private var _navigateToMfa by mutableStateOf(false)
    val navigateToMfa: Boolean
        get() = _navigateToMfa

    private var _mfaResponse by mutableStateOf<EmailPasswordAssessment?>(null)
    val mfaResponse: EmailPasswordAssessment?
        get() = _mfaResponse

    fun doLogin() {
        _error = null

        viewModelScope.launch {
            val response = negotiateAuthentication(_email, _password)
            if (response.error != null) {
                _error = response.error.type
            } else {
                Log.d("Login", "Checking for MFA")
                if (response.proceedMfa) {
                    Log.d("Login", "MFA required. Navigating to MFA screen")
                    _mfaResponse = response
                    _navigateToMfa = true
                } else {
                    Log.d(
                        "Login",
                        "No MFA required. Login is complete! We have a session token: ${response.firstUserHints!!.token}"
                    )
                }
            }
        }
    }

    fun mfaComplete() {
        _navigateToMfa = false
    }

    fun setEmail(email: String) {
        _email = email
    }

    fun setPassword(password: String) {
        _password = password
    }
}

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = viewModel()
) {
    if (viewModel.navigateToMfa) {
        navController.navigate(
            "setup/mfa/${viewModel.mfaResponse!!.mfaSpec!!.ticket}/${
                viewModel.mfaResponse!!.mfaSpec!!.allowedMethods.joinToString(
                    ","
                )
            }"
        )
        viewModel.mfaComplete()
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
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.login_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
            )


            Column(
                modifier = Modifier
                    .width(270.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FormTextField(
                    value = viewModel.email,
                    label = stringResource(R.string.email),
                    onChange = { viewModel.setEmail(it) },
                    modifier = Modifier.padding(vertical = 25.dp)
                )
                FormTextField(
                    value = viewModel.password,
                    label = stringResource(R.string.password),
                    password = true,
                    onChange = { viewModel.setPassword(it) })

                AnyLink(
                    text = stringResource(R.string.password_forgot),
                    action = { navController.navigate("about/placeholder") },
                    modifier = Modifier.padding(vertical = 7.dp)
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
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Weblink(
                text = stringResource(R.string.password_manager_hint),
                url = "$REVOLT_SUPPORT/kb/interface/android/using-a-password-manager",
            )

            AnyLink(
                text = stringResource(R.string.resend_verification),
                action = { navController.navigate("about/placeholder") },
                modifier = Modifier.padding(vertical = 7.dp)
            )

            ElevatedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.back))
            }

            Button(
                onClick = { viewModel.doLogin() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.login))
            }
        }
    }
}