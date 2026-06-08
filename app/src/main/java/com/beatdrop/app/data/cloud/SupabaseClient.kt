package com.beatdrop.app.data.cloud

import okhttp3.Headers

object Supabase {

    internal const val SUPABASE_URL = "https://hvkehvpvbwgklqiznrqq.supabase.co"
    internal const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh2a2VodnB2Yndna2xxaXpucnFxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA3ODU4NzAsImV4cCI6MjA5NjM2MTg3MH0.bnrZTZFeFTZm97kHm0osehmVHKeM7tjhulL46U86Y0c"

    @Volatile internal var accessToken: String? = null
    @Volatile internal var currentUser: CloudUser? = null

    val currentUserId: String? get() = currentUser?.id

    fun isAuthenticated(): Boolean = currentUserId != null && accessToken != null

    internal fun headers(json: Boolean = true, prefer: String? = null): Headers = Headers.Builder().apply {
        add("apikey", SUPABASE_ANON_KEY)
        add("Authorization", "Bearer ${accessToken ?: SUPABASE_ANON_KEY}")
        if (json) add("Content-Type", "application/json")
        prefer?.let { add("Prefer", it) }
    }.build()

    internal fun setSession(token: String?, user: CloudUser?) {
        accessToken = token
        currentUser = user
    }

    internal fun clearSession() = setSession(null, null)
}
