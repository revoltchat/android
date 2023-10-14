package chat.revolt.components.emoji

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.internals.EmojiCategory
import chat.revolt.internals.EmojiMetadata
import chat.revolt.internals.EmojiPickerItem
import kotlinx.coroutines.launch

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
) {
    val view = LocalView.current
    val metadata = remember { EmojiMetadata() }
    val pickerList = remember(metadata) { metadata.flatPickerList() }
    val categorySpans = remember(pickerList) { metadata.categorySpans(pickerList) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val spanCount = 9 // https://github.com/googlefonts/emoji-metadata/#readme

    // The current category is the one that the user is currently looking at.
    // We calculate this using the grid state and the category spans
    // (which contain the start and end index of each category).
    val currentCategory = remember {
        derivedStateOf {
            val firstVisibleItem = gridState.firstVisibleItemIndex

            for (category in categorySpans.keys) {
                val (start, end) = categorySpans[category] ?: continue
                if (firstVisibleItem + 1 in start..end) {
                    return@derivedStateOf category
                }
            }

            return@derivedStateOf EmojiCategory.entries.last()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row {
            EmojiCategory.entries.forEach { category ->
                Column(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            scope.launch {
                                val index =
                                    pickerList.indexOfFirst { it is EmojiPickerItem.Category && it.category == category }
                                gridState.animateScrollToItem(index)
                            }
                        }
                        .then(
                            if (currentCategory.value == category) {
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            } else {
                                Modifier
                            }
                        )
                        .aspectRatio(1f)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = when (category) {
                            EmojiCategory.Smileys -> painterResource(R.drawable.ic_emoticon_24dp)
                            EmojiCategory.People -> painterResource(R.drawable.ic_human_greeting_variant_24dp)
                            EmojiCategory.Animals -> painterResource(R.drawable.ic_snake_24dp)
                            EmojiCategory.Food -> painterResource(R.drawable.ic_glass_mug_variant_24dp)
                            EmojiCategory.Travel -> painterResource(R.drawable.ic_train_bus_24dp)
                            EmojiCategory.Activities -> painterResource(R.drawable.ic_skate_24dp)
                            EmojiCategory.Objects -> painterResource(R.drawable.ic_table_chair_24dp)
                            EmojiCategory.Symbols -> painterResource(R.drawable.ic_symbol_24dp)
                            EmojiCategory.Flags -> painterResource(R.drawable.ic_flag_24dp)
                        },
                        contentDescription = null,
                        tint = if (currentCategory.value == category) {
                            MaterialTheme.colorScheme.primary
                        } else LocalContentColor.current
                    )
                }
            }
        }
        LazyVerticalGrid(
            state = gridState,
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
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}