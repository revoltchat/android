package chat.revolt.internals.markdown

import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import android.view.View
import chat.revolt.callbacks.Action
import chat.revolt.callbacks.ActionChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class EmoteSpan(drawable: Drawable) :
    ImageSpan(drawable, ALIGN_BOTTOM) {
}

class EmoteClickableSpan(private val emoteId: String) : LongClickableSpan() {
    override fun onClick(widget: View) {
        runBlocking(Dispatchers.IO) {
            ActionChannel.send(Action.EmoteInfo(emoteId))
        }
    }

    override fun onLongClick(view: View?) {
        // no-op
    }
}