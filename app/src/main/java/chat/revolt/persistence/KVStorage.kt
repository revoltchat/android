package chat.revolt.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull

val Context.revoltKVStorage: DataStore<Preferences> by preferencesDataStore(name = "revolt_kv")

@Singleton
class KVStorage @Inject constructor(
    @ApplicationContext private val mContext: Context
) {
    private val dataStore = mContext.revoltKVStorage

    suspend fun set(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    suspend fun get(key: String): String? {
        return dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))
    }

    suspend fun set(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value.toString()
        }
    }

    suspend fun getBoolean(key: String): Boolean? {
        return dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))?.toBoolean()
    }

    suspend fun remove(key: String) {
        dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
        }
    }
}
