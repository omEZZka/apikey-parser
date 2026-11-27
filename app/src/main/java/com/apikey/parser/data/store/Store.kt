package com.apikey.parser.data.store

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.apikey.parser.data.model.AppState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Store(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun load(): AppState {
        val raw = prefs.getString("state", null) ?: return AppState()
        return try { json.decodeFromString(AppState.serializer(), raw) } catch (e: Exception) { AppState() }
    }

    fun save(state: AppState) {
        prefs.edit().putString("state", json.encodeToString(AppState.serializer(), state)).apply()
    }
}
