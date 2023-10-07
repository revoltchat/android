package chat.revolt.components.emoji

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.internals.EmojiMetadata
import chat.revolt.internals.EmojiPickerItem

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
) {
    val view = LocalView.current
    val metadata = remember { EmojiMetadata() }
    val pickerList = remember(metadata) { metadata.flatPickerList() }
    val spanCount = 9 // https://github.com/googlefonts/emoji-metadata/#readme

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row {
            Text("Categories", fontWeight = FontWeight.Black)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(spanCount),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(
                pickerList.size,
                span = {
                    val item = pickerList[it]
                    when (item) {
                        is EmojiPickerItem.Emoji -> GridItemSpan(1)
                        is EmojiPickerItem.Category -> GridItemSpan(spanCount)
                    }
                }
            ) { index ->
                when (val item = pickerList[index]) {
                    is EmojiPickerItem.Emoji -> {
                        Column(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onEmojiSelected(item.emoji)
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                .aspectRatio(1f)
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(item.emoji, style = LocalTextStyle.current.copy(fontSize = 20.sp))
                        }
                    }

                    is EmojiPickerItem.Category -> {
                        Text(
                            stringResource(item.category.nameResource),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}