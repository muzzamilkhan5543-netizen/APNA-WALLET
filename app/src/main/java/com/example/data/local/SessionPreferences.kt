package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "apna_wallet_session")

class SessionPreferences(private val context: Context) {
    private val KEY_REMEMBERED_PHONE = stringPreferencesKey("remembered_phone")

    val rememberedPhoneFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[KEY_REMEMBERED_PHONE]
    }

    suspend fun setRememberedPhone(phone: String?) {
        context.sessionDataStore.edit { prefs ->
            if (phone.isNullOrBlank()) {
                prefs.remove(KEY_REMEMBERED_PHONE)
            } else {
                prefs[KEY_REMEMBERED_PHONE] = phone
            }
        }
    }
}
