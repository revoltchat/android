package chat.revolt.components.generic

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R

@Composable
fun PageHeader(
    text: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackButtonClicked: () -> Unit = {},
    additionalButtons: @Composable () -> Unit = {},
    maxLines: Int = Int.MAX_VALUE,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBackButton) {
            IconButton(onClick = onBackButtonClicked) {
                Icon(
                    modifier = modifier,
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        }
        Text(
            text = text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Left,
                fontSize = 24.sp
            ),
            modifier = modifier
                .padding(horizontal = 15.dp, vertical = 15.dp)
                .weight(1f),
        )
        additionalButtons()
    }
}

@Preview(showBackground = true)
@Composable
fun PageHeaderPreview() {
    PageHeader(text = "Page Header")
}

@Preview(showBackground = true)
@Composable
fun PageHeaderPreviewWithBackButton() {
    PageHeader(text = "Page Header", showBackButton = true)
}

@Preview(showBackground = true)
@Composable
fun PageHeaderPreviewWithAdditionalButtons() {
    PageHeader(text = "Page Header", showBackButton = true, additionalButtons = {
        IconButton(onClick = {}) {
            Icon(
                modifier = Modifier,
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null
            )
        }
    })
}