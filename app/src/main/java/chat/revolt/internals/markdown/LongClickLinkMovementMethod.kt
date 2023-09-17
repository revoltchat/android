package chat.revolt.internals.markdown

import android.os.Handler
import android.os.Looper
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

abstract class LongClickableSpan : ClickableSpan() {
    abstract fun onLongClick(view: View?)
}

// https://stackoverflow.com/a/63398843
class LongClickLinkMovementMethod : LinkMovementMethod() {
    private var longClickHandler: Handler? = null
    private var isLongPressed = false
    override fun onTouchEvent(
        widget: TextView, buffer: Spannable,
        event: MotionEvent
    ): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_CANCEL) {
            longClickHandler?.removeCallbacksAndMessages(null)
        }
        if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_DOWN
        ) {
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val link = buffer.getSpans(
                off, off,
                LongClickableSpan::class.java
            )
            if (link.isNotEmpty()) {
                if (action == MotionEvent.ACTION_UP) {
                    longClickHandler?.removeCallbacksAndMessages(null)
                    if (!isLongPressed) {
                        link[0].onClick(widget)
                    }
                    isLongPressed = false
                } else {
                    Selection.setSelection(
                        buffer,
                        buffer.getSpanStart(link[0]),
                        buffer.getSpanEnd(link[0])
                    )
                    longClickHandler?.postDelayed({
                        link[0].onLongClick(widget)
                        isLongPressed = true
                    }, LONG_CLICK_TIME)
                }
                return true
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }

    companion object {
        private const val LONG_CLICK_TIME = 500L
        val instance: MovementMethod?
            get() {
                if (sInstance == null) {
                    sInstance = LongClickLinkMovementMethod()
                    // Handler deprecated https://stackoverflow.com/a/62477706/4116924
                    sInstance!!.longClickHandler = Handler(Looper.getMainLooper())
                }
                return sInstance
            }

        private var sInstance: LongClickLinkMovementMethod? = null
    }
}