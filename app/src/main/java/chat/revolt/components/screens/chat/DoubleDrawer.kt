package chat.revolt.components.screens.chat

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

enum class DoubleDrawerOpenState {
    Start,
    Center,
    End
}

@OptIn(ExperimentalMaterialApi::class)
class DoubleDrawerState(
    var initialValue: DoubleDrawerOpenState = DoubleDrawerOpenState.Center,
    val confirmStateChange: (DoubleDrawerOpenState) -> Boolean = { true }
) {
    val swipeableState = SwipeableState<DoubleDrawerOpenState>(
        initialValue = initialValue,
        animationSpec = spring(),
        confirmStateChange = confirmStateChange
    )

    suspend fun focusStart() {
        swipeableState.animateTo(DoubleDrawerOpenState.Start)
    }

    suspend fun focusCenter() {
        swipeableState.animateTo(DoubleDrawerOpenState.Center)
    }

    suspend fun focusEnd() {
        swipeableState.animateTo(DoubleDrawerOpenState.End)
    }

    val isStart: Boolean
        get() = swipeableState.currentValue == DoubleDrawerOpenState.Start
    val isCenter: Boolean
        get() = swipeableState.currentValue == DoubleDrawerOpenState.Center
    val isEnd: Boolean
        get() = swipeableState.currentValue == DoubleDrawerOpenState.End

    val currentValue: DoubleDrawerOpenState
        get() = swipeableState.currentValue

    companion object {
        fun Saver(
            confirmStateChange: (DoubleDrawerOpenState) -> Boolean
        ): Saver<DoubleDrawerState, DoubleDrawerOpenState> = Saver(
            save = { it.currentValue },
            restore = { DoubleDrawerState(it, confirmStateChange) }
        )
    }
}

@Composable
fun rememberDoubleDrawerState(
    initialValue: DoubleDrawerOpenState = DoubleDrawerOpenState.Center,
    confirmStateChange: (DoubleDrawerOpenState) -> Boolean = { true }
): DoubleDrawerState = rememberSaveable(
    saver = DoubleDrawerState.Saver(confirmStateChange)
) {
    DoubleDrawerState(initialValue, confirmStateChange)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DoubleDrawer(
    state: DoubleDrawerState = rememberDoubleDrawerState(),
    startPanel: @Composable () -> Unit,
    endPanel: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isPortrait =
            LocalContext.current.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val drawerWeight =
            if (isPortrait) 0.9f else 0.8f

        val offsetValue =
            (constraints.maxWidth * drawerWeight) + (LocalDensity.current.run { 16.dp.toPx() })
        val isAtOffset = abs(state.swipeableState.offset.value) == abs(offsetValue)

        val contentCornerRadius by animateDpAsState(
            targetValue = if (isAtOffset) 16.dp else 0.dp,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .swipeable(
                    state = state.swipeableState,
                    orientation = Orientation.Horizontal,
                    velocityThreshold = 500.dp,
                    anchors = mapOf(
                        offsetValue to DoubleDrawerOpenState.Start,
                        0f to DoubleDrawerOpenState.Center,
                        -offsetValue to DoubleDrawerOpenState.End
                    ),
                    reverseDirection = layoutDirection == LayoutDirection.Rtl,
                    resistance = ResistanceConfig(0.5f, 0.5f)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(drawerWeight)
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset(
                            x = state.swipeableState.offset.value.roundToInt() - offsetValue.roundToInt(),
                            y = 0
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                ) {
                    startPanel()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(
                            x = state.swipeableState.offset.value.roundToInt(),
                            y = 0
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(contentCornerRadius))
                ) {
                    content()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(drawerWeight)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .align(Alignment.CenterEnd)
                    .offset {
                        IntOffset(
                            x = state.swipeableState.offset.value.roundToInt() + offsetValue.roundToInt(),
                            y = 0
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                ) {
                    endPanel()
                }
            }
        }
    }
}