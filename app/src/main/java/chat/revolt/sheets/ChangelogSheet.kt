package chat.revolt.sheets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import chat.revolt.api.internals.getFragmentActivity
import chat.revolt.fragments.ChangelogBottomSheetFragment

@Composable
fun ChangelogSheet(
    versionName: String,
    versionIsHistorical: Boolean,
    renderedContents: String,
    onDismiss: () -> Unit
) {
    val activity = LocalContext.current.getFragmentActivity()

    DisposableEffect(versionName, renderedContents) {
        val sheet = ChangelogBottomSheetFragment(onDismiss)
        sheet.arguments =
            ChangelogBottomSheetFragment.createArguments(
                versionName,
                versionIsHistorical,
                renderedContents,
            )

        activity?.supportFragmentManager?.let {
            sheet.show(it, ChangelogBottomSheetFragment.TAG)
        }

        onDispose {
            sheet.dismiss()
        }
    }
}