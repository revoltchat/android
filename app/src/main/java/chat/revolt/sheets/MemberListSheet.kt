package chat.revolt.sheets

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chat.revolt.R
import chat.revolt.api.RevoltAPI
import chat.revolt.api.internals.Roles
import chat.revolt.api.internals.WebCompat
import chat.revolt.api.internals.solidColor
import chat.revolt.api.routes.server.fetchMembers
import chat.revolt.api.schemas.Member
import chat.revolt.api.schemas.User
import chat.revolt.components.generic.PageHeader
import chat.revolt.components.generic.UserAvatar
import chat.revolt.components.generic.presenceFromStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

val DO_NOT_FETCH_OFFLINE_MEMBERS_SERVERS = listOf(
    "01F7ZSBSFHQ8TA81725KQCSDDP" // Revolt Lounge
)

sealed class MemberListItem {
    data class MemberItem(val member: Member) : MemberListItem()
    data class CategoryItem(val category: String, val count: Int) : MemberListItem()
}

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MemberListSheetViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    val fullItemList = mutableStateListOf<MemberListItem>()

    fun fetchMemberList(
        serverId: String
    ) {
        viewModelScope.launch {
            val memberList = fetchMembers(
                serverId = serverId,
                includeOffline = serverId !in DO_NOT_FETCH_OFFLINE_MEMBERS_SERVERS
            ).members

            val categories = mutableMapOf<String, List<Member>>()

            val offlineCategoryName = context.getString(R.string.status_offline)
            val defaultCategoryName = context.getString(R.string.status_online)

            memberList.forEach { member ->
                val user = RevoltAPI.userCache[member.id!!.user] ?: run {
                    Log.w(
                        "MemberListSheet",
                        "User ${member.id.user} found in member list of server $serverId but not in user cache"
                    )
                    return@forEach
                }

                if (user.online == false) {
                    categories[offlineCategoryName] =
                        (categories[offlineCategoryName] ?: listOf()) + member
                    return@forEach
                }

                val highestHoistedRole =
                    Roles.resolveHighestRole(serverId, member.id.user, hoisted = true)

                val category = if (highestHoistedRole != null) {
                    highestHoistedRole.name ?: context.getString(R.string.unknown)
                } else {
                    defaultCategoryName
                }

                categories[category] = (categories[category] ?: listOf()) + member
            }

            fullItemList.clear()

            // Hoisted roles
            Roles.inOrder(serverId) { it.hoist == true }.forEach { role ->
                val members = categories[role.name] ?: return@forEach
                fullItemList.add(MemberListItem.CategoryItem(role.name ?: "", members.size))
                members.forEach { member ->
                    fullItemList.add(MemberListItem.MemberItem(member))
                }
            }

            // Online
            if (!categories[defaultCategoryName].isNullOrEmpty()) {
                fullItemList.add(
                    MemberListItem.CategoryItem(
                        defaultCategoryName,
                        categories[defaultCategoryName]?.size ?: 0
                    )
                )
                categories[defaultCategoryName]?.forEach { member ->
                    fullItemList.add(MemberListItem.MemberItem(member))
                }
            }

            // Offline
            if (!categories[offlineCategoryName].isNullOrEmpty()) {
                fullItemList.add(
                    MemberListItem.CategoryItem(
                        offlineCategoryName,
                        categories[offlineCategoryName]?.size ?: 0
                    )
                )
                categories[offlineCategoryName]?.forEach { member ->
                    fullItemList.add(MemberListItem.MemberItem(member))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MemberListSheet(
    serverId: String,
    viewModel: MemberListSheetViewModel = hiltViewModel()
) {
    var showUserContextSheet by remember { mutableStateOf(false) }
    var userContextSheetTarget by remember { mutableStateOf("") }

    // We use LaunchedEffect to make sure that this is called every time any of the users status changes
    LaunchedEffect(RevoltAPI.userCache) {
        snapshotFlow { RevoltAPI.userCache }.distinctUntilChanged().collect {
            viewModel.fetchMemberList(serverId)
        }
    }

    if (showUserContextSheet) {
        val userContextSheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = userContextSheetState,
            onDismissRequest = {
                showUserContextSheet = false
            }) {
            UserContextSheet(
                userId = userContextSheetTarget,
                serverId = serverId
            )
        }
    }

    if (viewModel.fullItemList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    Column {
        PageHeader(text = "Members")

        LazyColumn {
            viewModel.fullItemList.forEachIndexed { index, item ->
                when (item) {
                    is MemberListItem.CategoryItem -> stickyHeader(key = "${item.category}-$index") {
                        MemberListCategory(text = item.category, count = item.count)
                    }

                    is MemberListItem.MemberItem -> item(key = item.member.id!!.user) {
                        MemberListMember(
                            member = item.member,
                            user = RevoltAPI.userCache[item.member.id.user]!!,
                            serverId = serverId,
                            onSelectUser = {
                                userContextSheetTarget = it
                                showUserContextSheet = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberListMember(
    member: Member,
    user: User,
    serverId: String,
    onSelectUser: (String) -> Unit
) {
    val highestColourRole = Roles.resolveHighestRole(serverId, member.id!!.user, true)
    val colour = highestColourRole?.colour?.let { WebCompat.parseColour(it) }
        ?: Brush.solidColor(LocalContentColor.current)

    Row(
        modifier = Modifier
            .clickable {
                onSelectUser(member.id.user)
            }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            username = member.nickname
                ?: user.displayName
                ?: user.username
                ?: user.id!!,
            avatar = user.avatar,
            userId = user.id!!,
            presence = presenceFromStatus(
                user.status?.presence,
                user.online ?: false
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = member.nickname
                ?: user.displayName
                ?: user.username
                ?: user.id,
            style = LocalTextStyle.current.copy(
                fontWeight = FontWeight.Bold,
                brush = colour
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MemberListCategory(
    text: String,
    count: Int
) {
    Text(
        text = AnnotatedString.Builder().apply {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(text)
            pop()

            pushStyle(
                SpanStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = LocalTextStyle.current.fontSize * 0.8,
                    color = LocalContentColor.current.copy(alpha = 0.6f)
                )
            )
            append("—$count")
            pop()
        }.toAnnotatedString(),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}