package com.apikey.parser.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Provider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val docsUrl: String? = null,
    val keyExample: String? = null,
    val keyTemplate: String? = null,
    val modelsPath: String? = null,
    val authHeader: String? = null,
    val authPrefix: String? = null,
    val profileId: String? = null
)

@Serializable
data class ApiKey(
    val id: String = UUID.randomUUID().toString(),
    val providerId: String,
    val key: String,
    val label: String = "",
    val balance: String? = null,
    val models: List<String> = emptyList(),
    val status: String = "unknown",
    val lastChecked: Long = 0L,
    val expiresAt: Long? = null,
    val favorite: Boolean = false
)

@Serializable
data class AppState(
    val providers: List<Provider> = emptyList(),
    val keys: List<ApiKey> = emptyList(),
    val themeIndex: Int = 0,
    val language: String = "ru",
    val balanceThreshold: Double = 1.0,
    val concurrency: Int = 4,
    val autoRefreshMinutes: Int = 30
)
