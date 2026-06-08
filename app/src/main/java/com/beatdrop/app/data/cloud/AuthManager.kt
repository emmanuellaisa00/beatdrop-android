package com.beatdrop.app.data.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Minimal user shape the Compose UI needs from Supabase Auth. */
data class CloudUser(
    val id: String,
    val email: String? = null,
    val userMetadata: Map<String, JsonElement> = emptyMap(),
)

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: CloudUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager : ViewModel() {

    private val client = OkHttpClient()
    private val jsonMedia = "application/json".toMediaType()

    private val _authState = MutableStateFlow<AuthState>(
        Supabase.currentUser?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun signUp(email: String, password: String, displayName: String, username: String) {
        viewModelScope.launch {
            runAuthCall("Sign up failed") {
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                    put("data", JSONObject().apply {
                        put("display_name", displayName)
                        put("username", username)
                    })
                }
                val json = postAuth("/auth/v1/signup", body)
                applySession(json)
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            runAuthCall("Sign in failed") {
                val body = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }
                val json = postAuth("/auth/v1/token?grant_type=password", body)
                applySession(json)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val req = Request.Builder()
                        .url("${Supabase.SUPABASE_URL}/auth/v1/logout")
                        .headers(Supabase.headers())
                        .post(ByteArray(0).toRequestBody(jsonMedia))
                        .build()
                    client.newCall(req).execute().close()
                }
            }
            Supabase.clearSession()
            _authState.value = AuthState.Unauthenticated
            _isLoading.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            runAuthCall("Failed to send reset email", updateState = false) {
                postAuth("/auth/v1/recover", JSONObject().put("email", email))
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            runAuthCall("Password update failed", updateState = false) {
                val req = Request.Builder()
                    .url("${Supabase.SUPABASE_URL}/auth/v1/user")
                    .headers(Supabase.headers())
                    .put(JSONObject().put("password", newPassword).toString().toRequestBody(jsonMedia))
                    .build()
                withContext(Dispatchers.IO) { client.newCall(req).execute().use { if (!it.isSuccessful) error(it.message) } }
            }
        }
    }

    fun refreshSession() = Unit

    val currentUser: CloudUser? get() = Supabase.currentUser
    val currentUserId: String? get() = currentUser?.id

    private suspend fun runAuthCall(
        fallback: String,
        updateState: Boolean = true,
        block: suspend () -> Unit,
    ) {
        _isLoading.value = true
        try {
            block()
            if (updateState) {
                _authState.value = Supabase.currentUser?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(cleanAuthError(e.message ?: fallback, fallback))
        } finally {
            _isLoading.value = false
        }
    }

    private fun cleanAuthError(message: String, fallback: String): String {
        val m = message.ifBlank { fallback }
        val lower = m.lowercase()
        return when {
            "email not confirmed" in lower || "email_not_confirmed" in lower || "not verified" in lower ->
                "Account sign-in is not ready. Try creating the account again or use forgot password."
            "invalid login" in lower || "invalid_grant" in lower || "invalid credentials" in lower ->
                "Incorrect email or password."
            else -> m
        }
    }

    private suspend fun postAuth(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("${Supabase.SUPABASE_URL}$path")
            .headers(Supabase.headers())
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) error(JSONObject(text.ifBlank { "{}" }).optString("msg", res.message))
            JSONObject(text.ifBlank { "{}" })
        }
    }

    private fun applySession(json: JSONObject) {
        val token = json.optString("access_token").takeIf { it.isNotBlank() }
        val userJson = json.optJSONObject("user") ?: json
        val user = parseUser(userJson)
        Supabase.setSession(token, user)
    }

    private fun parseUser(json: JSONObject): CloudUser {
        val metaJson = json.optJSONObject("user_metadata") ?: JSONObject()
        val meta = buildMap {
            metaJson.keys().forEach { key -> put(key, JsonPrimitive(metaJson.optString(key))) }
        }
        return CloudUser(
            id = json.optString("id"),
            email = json.optString("email").takeIf { it.isNotBlank() },
            userMetadata = meta,
        )
    }
}
