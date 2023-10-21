package chat.revolt.components.generic

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import chat.revolt.api.settings.GlobalState
import chat.revolt.ui.theme.Theme
import com.google.android.material.tabs.TabLayout

@Composable
fun PrimaryTabs(tabs: List<String>, currentIndex: Int, onTabSelected: (Int) -> Unit) {
    when (GlobalState.theme) {
        Theme.M3Dynamic -> AndroidView(
            factory = {
                TabLayout(it).apply {
                    tabMode = TabLayout.MODE_FIXED
                    tabGravity = TabLayout.GRAVITY_FILL

                    tabs.forEach { tab ->
                        addTab(newTab().setText(tab))
                    }

                    addOnTabSelectedListener(object :
                        TabLayout.OnTabSelectedListener {
                        override fun onTabSelected(tab: TabLayout.Tab?) {
                            onTabSelected(tab?.position ?: 0)
                        }

                        override fun onTabUnselected(tab: TabLayout.Tab?) {
                        }

                        override fun onTabReselected(tab: TabLayout.Tab?) {
                        }
                    })
                }
            },
            update = {
                it.getTabAt(currentIndex)?.select()
            },
            modifier = Modifier.fillMaxWidth()
        )

        else -> TabRow(selectedTabIndex = currentIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == currentIndex,
                    onClick = { onTabSelected(index) },
                    text = { Text(tab) }
                )
            }
        }
    }
}

@Preview
@Composable
fun PrimaryTabsPreview() {
    PrimaryTabs(
        tabs = listOf("Tab 1", "Tab 2", "Tab 3"),
        currentIndex = 0,
        onTabSelected = {}
    )
}
