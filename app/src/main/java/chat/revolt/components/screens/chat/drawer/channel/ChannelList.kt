package chat.revolt.components.screens.chat.drawer.channel

import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import chat.revolt.R
import chat.revolt.activities.RevoltTweenColour
import chat.revolt.activities.RevoltTweenDp
import chat.revolt.activities.RevoltTweenFloat
import chat.revolt.api.REVOLT_FILES
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.CategorisedChannelList
import chat.revolt.api.internals.ChannelUtils
import chat.revolt.api.schemas.ChannelType
import chat.revolt.api.schemas.ServerFlags
import chat.revolt.api.schemas.User
import chat.revolt.api.schemas.has
import chat.revolt.components.generic.presenceFromStatus
import chat.revolt.components.screens.chat.drawer.server.DrawerChannel
import chat.revolt.sheets.ChannelContextSheet
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlin.math.max

const val BANNER_HEIGHT_COMPACT = 56
const val BANNER_HEIGHT_EXPANDED = 128

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RowScope.ChannelList(
    serverId: String,
    currentDestination: String?,
    currentChannel: String?,
    onChannelClick: (String) -> Unit,
    onSpecialClick: (String) -> Unit,
    onServerSheetOpenFor: (String) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val enableSmallBanner by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemScrollOffset > 40 ||
                    lazyListState.firstVisibleItemIndex > 0
        }
    }

    val bannerHeight by animateDpAsState(
        targetValue = if (enableSmallBanner) BANNER_HEIGHT_COMPACT.dp else BANNER_HEIGHT_EXPANDED.dp,
        animationSpec = RevoltTweenDp,
        label = "Banner Height"
    )
    val bannerImageOpacity by animateFloatAsState(
        targetValue = if (enableSmallBanner) 0f else 1f,
        animationSpec = RevoltTweenFloat,
        label = "Banner Image Opacity"
    )
    val bannerTextColour by animateColorAsState(
        targetValue = if (enableSmallBanner) LocalContentColor.current else Color.White,
        animationSpec = RevoltTweenColour,
        label = "Banner Text Colour"
    )

    var channelContextSheetShown by remember { mutableStateOf(false) }
    var channelContextSheetTarget by remember { mutableStateOf("") }

    if (channelContextSheetShown) {
        val channelContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = channelContextSheetState,
            onDismissRequest = {
                channelContextSheetShown = false
            }
        ) {
            ChannelContextSheet(
                channelId = channelContextSheetTarget,
                onHideSheet = {
                    channelContextSheetState.hide()
                    channelContextSheetShown = false
                }
            )
        }
    }

    val dmAbleChannels =
        RevoltAPI.channelCache.values
            .filter { it.channelType == ChannelType.DirectMessage || it.channelType == ChannelType.Group }
            .filter { if (it.channelType == ChannelType.DirectMessage) it.active == true else true }
            .sortedBy { it.lastMessageID ?: it.id }
            .reversed()

    val server = RevoltAPI.serverCache[serverId]
    val categorisedChannels = server?.let {
        ChannelUtils.categoriseServerFlat(it)
    }

    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
    ) {
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxSize(),
            state = lazyListState
        ) {
            if (serverId == "home") {
                stickyHeader(
                    key = "header"
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp)
                            .alpha(0.9f)
                            .height(BANNER_HEIGHT_COMPACT.dp + 8.dp) // due to padding in Text
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            .weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.direct_messages),
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
                        )
                    }
                }

                item(
                    key = "home"
                ) {
                    DrawerChannel(
                        name = stringResource(R.string.home),
                        channelType = ChannelType.TextChannel,
                        selected = currentDestination == "home",
                        hasUnread = false,
                        onClick = {
                            onSpecialClick("home")
                        },
                        large = true
                    )
                }

                item(
                    key = "notes"
                ) {
                    val notesChannelId =
                        RevoltAPI.channelCache.values.firstOrNull { it.channelType == ChannelType.SavedMessages }?.id

                    DrawerChannel(
                        name = stringResource(R.string.channel_notes),
                        channelType = ChannelType.SavedMessages,
                        selected = currentDestination == "channel/{channelId}" && currentChannel == notesChannelId,
                        hasUnread = false,
                        onClick = {
                            onChannelClick(notesChannelId ?: return@DrawerChannel)
                        },
                        large = true
                    )
                }

                item(
                    key = "divider"
                ) {
                    Surface(
                        Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .height(1.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    ) {}
                }

                items(
                    dmAbleChannels.size,
                    key = { index ->
                        val channel = dmAbleChannels.getOrNull(index)
                        channel?.id ?: index
                    }
                ) {
                    val channel = dmAbleChannels.getOrNull(it) ?: return@items

                    val partner =
                        if (channel.channelType == ChannelType.DirectMessage) {
                            RevoltAPI.userCache[
                                ChannelUtils.resolveDMPartner(
                                    channel
                                )
                            ]
                        } else {
                            null
                        }

                    DrawerChannel(
                        name = partner?.let { p -> User.resolveDefaultName(p) } ?: channel.name
                        ?: stringResource(R.string.unknown),
                        channelType = channel.channelType ?: ChannelType.TextChannel,
                        selected = currentDestination == "channel/{channelId}" && currentChannel == channel.id,
                        hasUnread = channel.lastMessageID?.let { lastMessageID ->
                            RevoltAPI.unreads.hasUnread(
                                channel.id!!,
                                lastMessageID
                            )
                        } ?: false,
                        dmPartnerIcon = partner?.avatar ?: channel.icon,
                        dmPartnerId = partner?.id,
                        dmPartnerName = partner?.let { p -> User.resolveDefaultName(p) },
                        dmPartnerStatus = presenceFromStatus(
                            status = partner?.status?.presence,
                            online = partner?.online ?: false
                        ),
                        onClick = {
                            onChannelClick(channel.id ?: return@DrawerChannel)
                        },
                        onLongClick = {
                            channelContextSheetTarget = channel.id ?: return@DrawerChannel
                            channelContextSheetShown = true
                        }
                    )
                }
            } else {
                stickyHeader {
                    Box(
                        contentAlignment = Alignment.BottomStart,
                        modifier = Modifier
                            .then(
                                // if there is no banner, we change the design slightly.
                                // instead of there being a banner card we make a "classic"
                                // sticky header á la Google Messages
                                if (server?.banner != null) {
                                    Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                } else {
                                    Modifier.padding(
                                        start = 0.dp,
                                        end = 8.dp,
                                        top = 0.dp,
                                        bottom = 0.dp
                                    )
                                }
                            )
                            .fillMaxWidth()
                    ) {
                        if (server?.banner != null) {
                            Box(modifier = Modifier.height(bannerHeight)) {
                                Box(
                                    modifier = Modifier
                                        .alpha(max(0.95f, bannerImageOpacity))
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                )

                                // *** ANDROIDVIEW RATIONALE ***
                                // Compose w/ Glide looks super laggy when resizing, because
                                // it tries to refetch the image every time. (luckily from cache)
                                // This is a temporary workaround until Glide can be resized
                                // without refetching in Compose.
                                AndroidView(
                                    factory = { ctx ->
                                        AppCompatImageView(ctx).apply {
                                            scaleType = ImageView.ScaleType.CENTER_CROP

                                            Glide.with(this)
                                                .load("$REVOLT_FILES/banners/${server.banner.id}")
                                                .transition(
                                                    DrawableTransitionOptions.withCrossFade()
                                                )
                                                .into(this)
                                        }
                                    },
                                    update = {
                                        it.layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    },
                                    modifier = Modifier
                                        .alpha(bannerImageOpacity)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                )

                                Box(
                                    modifier = Modifier
                                        .alpha(bannerImageOpacity)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.3f)
                                                )
                                            )
                                        )
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .alpha(0.9f)
                                    .height(
                                        BANNER_HEIGHT_COMPACT.dp + 8.dp
                                    ) // due to padding in Text
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                    )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width(16.dp))

                            if (server?.flags has ServerFlags.Official) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_revolt_decagram_24dp
                                    ),
                                    contentDescription = stringResource(
                                        R.string.server_flag_official
                                    ),
                                    tint = if (server?.banner != null) {
                                        bannerTextColour
                                    } else {
                                        LocalContentColor.current
                                    },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(24.dp)
                                )
                            }
                            if (server?.flags has ServerFlags.Verified) {
                                Icon(
                                    painter = painterResource(
                                        id = R.drawable.ic_check_decagram_24dp
                                    ),
                                    contentDescription = stringResource(
                                        R.string.server_flag_verified
                                    ),
                                    tint = if (server?.banner != null) {
                                        bannerTextColour
                                    } else {
                                        LocalContentColor.current
                                    },
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(24.dp)
                                )
                            }

                            Text(
                                text = (
                                        server?.name
                                            ?: stringResource(R.string.unknown)
                                        ),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (server?.banner != null) {
                                    bannerTextColour
                                } else {
                                    LocalContentColor.current
                                },
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .then(
                                        if (server?.banner != null) {
                                            Modifier.padding(
                                                start = 0.dp,
                                                end = 16.dp,
                                                top = 16.dp,
                                                bottom = 16.dp
                                            )
                                        } else {
                                            Modifier.padding(
                                                start = 0.dp,
                                                end = 24.dp,
                                                top = 16.dp,
                                                bottom = 16.dp
                                            )
                                        }
                                    )
                                    .weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(onClick = {
                                onServerSheetOpenFor(serverId)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(
                                        id = R.string.settings
                                    ),
                                    tint = if (server?.banner != null) {
                                        bannerTextColour
                                    } else {
                                        LocalContentColor.current
                                    }
                                )
                            }
                        }
                    }
                }

                if (categorisedChannels.isNullOrEmpty()) {
                    item {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_channels_heading),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_channels_body),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(
                        categorisedChannels.size,
                        key = { index ->
                            val channel = categorisedChannels.getOrNull(index)
                            channel?.let {
                                when (it) {
                                    is CategorisedChannelList.Channel -> it.channel.id
                                    is CategorisedChannelList.Category -> it.category.id
                                }
                            } ?: index
                        }
                    ) {
                        when (val item = categorisedChannels.getOrNull(it)) {
                            is CategorisedChannelList.Channel -> {
                                val channel = item.channel

                                val partner =
                                    if (channel.channelType == ChannelType.DirectMessage) {
                                        RevoltAPI.userCache[
                                            ChannelUtils.resolveDMPartner(
                                                channel
                                            )
                                        ]
                                    } else {
                                        null
                                    }

                                DrawerChannel(
                                    name = partner?.let { p -> User.resolveDefaultName(p) }
                                        ?: channel.name
                                        ?: stringResource(R.string.unknown),
                                    channelType = channel.channelType ?: ChannelType.TextChannel,
                                    selected = currentDestination == "channel/{channelId}" && currentChannel == channel.id,
                                    hasUnread = channel.lastMessageID?.let { lastMessageID ->
                                        RevoltAPI.unreads.hasUnread(
                                            channel.id!!,
                                            lastMessageID
                                        )
                                    } ?: false,
                                    dmPartnerIcon = partner?.avatar ?: channel.icon,
                                    dmPartnerId = partner?.id,
                                    dmPartnerName = partner?.let { p ->
                                        User.resolveDefaultName(
                                            p
                                        )
                                    },
                                    dmPartnerStatus = presenceFromStatus(
                                        status = partner?.status?.presence,
                                        online = partner?.online ?: false
                                    ),
                                    onClick = {
                                        onChannelClick(channel.id ?: return@DrawerChannel)
                                    },
                                    onLongClick = {
                                        channelContextSheetTarget =
                                            channel.id ?: return@DrawerChannel
                                        channelContextSheetShown = true
                                    }
                                )
                            }

                            is CategorisedChannelList.Category -> {
                                val category = item.category

                                Text(
                                    text = category.title ?: stringResource(R.string.unknown),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 24.dp,
                                            bottom = 16.dp
                                        )
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
