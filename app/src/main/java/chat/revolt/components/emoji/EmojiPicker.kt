package chat.revolt.components.emoji

import android.util.TypedValue
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.revolt.R
import chat.revolt.api.REVOLT_FILES
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import chat.revolt.components.generic.IconPlaceholder
import chat.revolt.components.generic.RemoteImage
import chat.revolt.internals.Category
import chat.revolt.internals.EmojiMetadata
import chat.revolt.internals.EmojiPickerItem
import chat.revolt.internals.UnicodeEmojiSection
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
) {
    val view = LocalView.current
    val metadata = remember { EmojiMetadata() }
    val pickerList = remember(metadata) { metadata.flatPickerList() }
    val servers = remember(metadata) { metadata.serversWithEmotes() }
    val categorySpans = remember(pickerList) { metadata.categorySpans(pickerList) }
    val gridState = rememberLazyGridState()
    val categoryRowScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val spanCount = 9 // https://github.com/googlefonts/emoji-metadata/#readme

    // The current category is the one that the user is currently looking at.
    val currentCategory = remember(gridState, categorySpans) {
        derivedStateOf {
            val firstVisible = gridState.firstVisibleItemIndex
            val firstCategory =
                categorySpans.entries.firstOrNull { it.value.first <= firstVisible && it.value.second >= firstVisible }?.key

            firstCategory
        }
    }

    LaunchedEffect(currentCategory.value) {
        // Scroll to the server icon of the current category.
        val offset = categorySpans.entries.indexOfFirst { it.key == currentCategory.value }
        var px = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            37f + 4f,
            view.resources.displayMetrics
        ).toInt()

        // If the user is looking at the unicode emoji, scroll to the end instead
        // so that the category icons are all neatly aligned.
        //
        // Impl -> Not scrolling to "the end" but to the current category plus 50.
        // (Which technically is an evil hack, but technically I could also
        // poke an eye out with a spoon, so let's not worry about technicalities.)
        if (currentCategory.value is Category.UnicodeEmojiCategory) px += 50

        categoryRowScrollState.animateScrollTo(offset * px)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(categoryRowScrollState)
                .height(37.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            servers.forEach { server ->
                Column(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            scope.launch {
                                val index =
                                    pickerList.indexOfFirst { it is EmojiPickerItem.Section && it.category is Category.ServerEmoteCategory && it.category.server == server }
                                gridState.animateScrollToItem(index)
                            }
                        }
                        .then(
                            if (currentCategory.value is Category.ServerEmoteCategory && (currentCategory.value as Category.ServerEmoteCategory).server == server) {
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            } else {
                                Modifier
                            }
                        )
                        .aspectRatio(1f)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (server.icon == null) {
                        IconPlaceholder(
                            name = server.name ?: stringResource(R.string.unknown),
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .fillMaxSize()
                        )
                    } else {
                        RemoteImage(
                            url = "$REVOLT_FILES/icons/${server.icon.id}/icon.gif?max_side=64",
                            description = server.name,
                            modifier = Modifier
                                .clip(CircleShape)
                                .fillMaxSize()
                        )
                    }
                }
            }
            UnicodeEmojiSection.entries.forEach { category ->
                Column(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            scope.launch {
                                val index =
                                    pickerList.indexOfFirst { it is EmojiPickerItem.Section && it.category is Category.UnicodeEmojiCategory && it.category.definition == category }
                                gridState.animateScrollToItem(index)
                            }
                        }
                        .then(
                            if (currentCategory.value is Category.UnicodeEmojiCategory && (currentCategory.value as Category.UnicodeEmojiCategory).definition == category) {
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            } else {
                                Modifier
                            }
                        )
                        .aspectRatio(1f)
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = when (category) {
                            UnicodeEmojiSection.Smileys -> painterResource(R.drawable.ic_emoticon_24dp)
                            UnicodeEmojiSection.People -> painterResource(R.drawable.ic_human_greeting_variant_24dp)
                            UnicodeEmojiSection.Animals -> painterResource(R.drawable.ic_snake_24dp)
                            UnicodeEmojiSection.Food -> painterResource(R.drawable.ic_glass_mug_variant_24dp)
                            UnicodeEmojiSection.Travel -> painterResource(R.drawable.ic_train_bus_24dp)
                            UnicodeEmojiSection.Activities -> painterResource(R.drawable.ic_skate_24dp)
                            UnicodeEmojiSection.Objects -> painterResource(R.drawable.ic_table_chair_24dp)
                            UnicodeEmojiSection.Symbols -> painterResource(R.drawable.ic_symbol_24dp)
                            UnicodeEmojiSection.Flags -> painterResource(R.drawable.ic_flag_24dp)
                        },
                        contentDescription = null,
                        tint = if (currentCategory.value is Category.UnicodeEmojiCategory && (currentCategory.value as Category.UnicodeEmojiCategory).definition == category) {
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
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                pickerList.size,
                span = {
                    val item = pickerList[it]
                    when (item) {
                        is EmojiPickerItem.UnicodeEmoji -> GridItemSpan(1)
                        is EmojiPickerItem.ServerEmote -> GridItemSpan(1)
                        is EmojiPickerItem.Section -> GridItemSpan(spanCount)
                    }
                }
            ) { index ->
                when (val item = pickerList[index]) {
                    is EmojiPickerItem.UnicodeEmoji -> {
                        Column(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onEmojiSelected(item.emoji)
                                }
                                .aspectRatio(1f)
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(item.emoji, style = LocalTextStyle.current.copy(fontSize = 20.sp))
                        }
                    }

                    is EmojiPickerItem.ServerEmote -> {
                        Column(
                            modifier = Modifier
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        onEmojiSelected(":${item.emote.id}:")
                                    },
                                    onLongClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        scope.launch {
                                            item.emote.id?.let {
                                                ActionChannel.send(
                                                    Action.EmoteInfo(
                                                        it
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                                .aspectRatio(1f)
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            RemoteImage(
                                url = "$REVOLT_FILES/emojis/${item.emote.id}/emoji.gif",
                                description = item.emote.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }

                    is EmojiPickerItem.Section -> {
                        Text(
                            when (item.category) {
                                is Category.UnicodeEmojiCategory -> stringResource(item.category.definition.nameResource)
                                is Category.ServerEmoteCategory -> item.category.server.name
                                    ?: stringResource(R.string.unknown)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}