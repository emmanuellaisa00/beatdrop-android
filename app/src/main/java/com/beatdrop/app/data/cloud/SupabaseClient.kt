package com.beatdrop.app.data.cloud

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object Supabase {

    // Replace with your values from local.properties / BuildConfig
    private const val SUPABASE_URL = "https://hvkehvpvbwgklqiznrqq.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh2a2VodnB2Yndna2xxaXpucnFxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA3ODU4NzAsImV4cCI6MjA5NjM2MTg3MH0.bnrZTZFeFTZm97kHm0osehmVHKeM7tjhulL46U86Y0c"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = "beatdrop"
                host = "auth"
            }
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }

    val auth get() = client.auth
    val postgrest get() = client.postgrest
    val storage get() = client.storage
    val realtime get() = client.realtime

    // Current user ID or null
    val currentUserId: String?
        get() = auth.currentSessionOrNull()?.user?.id

    fun isAuthenticated(): Boolean = currentUserId != null
}
