package chat.revolt.c2dm

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import chat.revolt.R
import chat.revolt.activities.MainActivity
import chat.revolt.api.internals.SpecialUsers
import chat.revolt.api.internals.ULID
import chat.revolt.api.routes.push.subscribePush
import chat.revolt.c2dm.ChannelRegistrator.Companion.CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking

class HandlerService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        runBlocking {
            subscribePush(auth = token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val integ = ULID.asInteger(ULID.makeNext())
        val bitmap = Glide.with(this)
            .asBitmap()
            .load("https://autumn.revolt.chat/avatars/GJXjHUC1X7tGEgHFhYOvtCByuNRd72qFwlztjKZUHP/bfe0842b74ae716139138574e8a1b749751e2968~3.jpg")
            .circleCrop()
            .submit()
            .get()
        val jen =
            Person.Builder()
                .setBot(true)
                .setKey(SpecialUsers.JENNIFER)
                .setIcon(IconCompat.createWithBitmap(bitmap))
                .setName("Jennifer")
                .build()
        var remoteInput = RemoteInput.Builder("Reply").run {
            setLabel("Reply")
            build()
        }
        var action: NotificationCompat.Action =
            NotificationCompat.Action.Builder(
                R.drawable.ic_reply_24dp,
                "Reply", PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_MUTABLE
                )
            )
                .addRemoteInput(remoteInput)
                .build()
        var builder = NotificationCompat.Builder(this, CHANNEL_ID_GROUP_SOCIAL_FRIENDREQUESTS)
            .setSmallIcon(R.drawable.ic_message_text_24dp)
            .setContentTitle("Jennifer")
            .setContentText("Expand for details")
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setStyle(
                NotificationCompat.MessagingStyle(jen)
                    .setConversationTitle("#Genderal")
                    .addMessage("hiii 👋", System.currentTimeMillis(), jen)
            )
            .addAction(action)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        NotificationManagerCompat.from(this).apply {
            if (ActivityCompat.checkSelfPermission(
                    this@HandlerService,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(integ, builder.build())
        }
    }
}