package chat.revolt.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chat.revolt.api.REVOLT_BASE
import chat.revolt.components.generic.SheetEnd

enum class DebugPrepSheetPage {
    API
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPrepSheet(modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf(DebugPrepSheetPage.API) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Debug Preparation", style = MaterialTheme.typography.headlineMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == DebugPrepSheetPage.API,
                onClick = { mode = DebugPrepSheetPage.API },
                shape = CircleShape
            ) {
                Text("API")
            }
        }

        when (mode) {
            DebugPrepSheetPage.API -> {
                Text("API Base URL (not reactive)")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = REVOLT_BASE == "https://api.revolt.chat",
                        onClick = {
                            REVOLT_BASE = "https://api.revolt.chat"
                        },
                        shape = CircleShape.copy(
                            topEnd = CornerSize(0),
                            bottomEnd = CornerSize(0)
                        )
                    ) {
                        Text("api.")
                    }
                    SegmentedButton(
                        selected = REVOLT_BASE == "https://revolt.chat/api",
                        onClick = {
                            REVOLT_BASE = "https://revolt.chat/api"
                        },
                        shape = CircleShape.copy(
                            topStart = CornerSize(0),
                            bottomStart = CornerSize(0)
                        )
                    ) {
                        Text("/api")
                    }
                }
            }
        }
    }
    SheetEnd()
}