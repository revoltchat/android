package chat.revolt.screens.chat.views

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.activities.InviteActivity
import chat.revolt.components.screens.home.LinkOnHome
import chat.revolt.internals.extensions.zero

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, useDrawer: Boolean, onDrawerClicked: () -> Unit) {
    val context = LocalContext.current

    val catTransition = rememberInfiniteTransition(label = "cat")
    val catRotation by catTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "catRotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (useDrawer) {
                        IconButton(onClick = {
                            onDrawerClicked()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(id = R.string.menu)
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.zero
            )
        },
    ) { pv ->
        Column(
            modifier = Modifier
                .padding(pv)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🐈",
                    fontSize = 100.sp,
                    modifier = Modifier.rotate(catRotation)
                )
            }

            Column(Modifier.padding(16.dp)) {
                LinkOnHome(
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(id = R.string.home_join_jenvolt),
                            modifier = modifier
                        )
                    },
                    heading = { Text(text = stringResource(id = R.string.home_join_jenvolt)) },
                    description = {
                        Text(
                            text = stringResource(id = R.string.home_join_jenvolt_description)
                        )
                    }
                ) {
                    context.startActivity(
                        Intent(
                            context,
                            InviteActivity::class.java
                        )
                            .setData(Uri.parse("https://rvlt.gg/jen"))
                            .setAction(Intent.ACTION_VIEW)
                    )
                }
            }
        }
    }
}
