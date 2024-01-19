package chat.revolt.api.internals

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

fun Context.getComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getComponentActivity()
    else -> null
}