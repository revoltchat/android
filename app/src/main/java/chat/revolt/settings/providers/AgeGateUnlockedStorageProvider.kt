package chat.revolt.settings.providers

import chat.revolt.RevoltApplication
import chat.revolt.persistence.KVStorage

object AgeGateUnlockedStorageProvider {
    private val kv = KVStorage(RevoltApplication.instance)

    suspend fun setAgeGateUnlocked(unlocked: Boolean) {
        kv.set("ageGateUnlocked", unlocked)
    }

    suspend fun getAgeGateUnlocked(): Boolean {
        return kv.getBoolean("ageGateUnlocked") ?: false
    }
}