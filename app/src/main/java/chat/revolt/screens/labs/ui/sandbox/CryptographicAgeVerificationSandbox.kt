package chat.revolt.screens.labs.ui.sandbox

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import chat.revolt.api.REVOLT_KJBOOK
import chat.revolt.internals.CryptographicAgeVerification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptographicAgeVerificationSandbox(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crypto Age Verify Sandbox",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .fillMaxSize()
        ) {
            Button(onClick = {
                val cav = CryptographicAgeVerification()
                val (ageHash, verifyHash) = cav.getProofHash(
                    seed = REVOLT_KJBOOK,
                    userAge = 21,
                    minAllowedAge = 18
                )

                Log.d(
                    "CryptographicAgeVerificationSandbox",
                    "ageHash: ${ageHash.contentToString()}"
                )
                Log.d(
                    "CryptographicAgeVerificationSandbox",
                    "verifyHash: ${verifyHash.contentToString()}"
                )

                val verification = cav.proveAge(
                    minAllowedAge = 18,
                    proofHash = verifyHash,
                    ageHash = ageHash
                )

                Log.d("CryptographicAgeVerificationSandbox", "Verification result: $verification")
            }) {
                Text("Run Cryptographic Age Verification")
            }
        }
    }
}