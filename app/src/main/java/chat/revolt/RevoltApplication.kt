package chat.revolt

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RevoltApplication : Application() {
    init {
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}