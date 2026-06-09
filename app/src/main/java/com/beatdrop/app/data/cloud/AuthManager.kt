package com.beatdrop.app.data.cloud

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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

private class AuthApiException(
    val statusCode: Int,
    val apiMessage: String,
    val errorCode: String? = null,
) : Exception(apiMessage)

class AuthManager(app: Application) : AndroidViewModel(app) {

    private val client = OkHttpClient()
    private val jsonMedia = "application/json".toMediaType()
    private val prefs = app.getSharedPreferences("beatdrop_auth", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        restoreSession()
    }

    fun signUp(email: String, password: String, displayName: String, username: String) {
        viewModelScope.launch {
            runAuthCall("Sign up failed") {
                val cleanEmail = email.trim().lowercase()
                val cleanUsername = username.trim().lowercase()
                val body = JSONObject().apply {
                    put("email", cleanEmail)
                    put("password", password)
                    put("data", JSONObject().apply {
                        put("display_name", displayName.trim())
                        put("username", cleanUsername)
                    })
                }
                val json = postAuth("/auth/v1/signup", body)
                val token = json.optString("access_token").takeIf { it.isNotBlank() }
                if (token == null) {
                    rememberEmail(cleanEmail)
                    if (cleanUsername.isNotBlank()) rememberUsername(cleanUsername, cleanEmail)
                    throw AuthApiException(200, "Account already exists. Sign in instead.", "user_exists")
                }
                applySession(json)
                rememberEmail(cleanEmail)
                if (cleanUsername.isNotBlank()) rememberUsername(cleanUsername, cleanEmail)
            }
        }
    }

    /** Identifier can be email or a username previously used on this device. */
    fun signIn(identifier: String, password: String) {
        viewModelScope.launch {
            val rawIdentifier = identifier.trim()
            val resolvedEmail = resolveLoginIdentifier(rawIdentifier)
            if (resolvedEmail == null) {
                _authState.value = AuthState.Error("Username not found. Sign in with email once or create an account.")
                return@launch
            }
            runAuthCall("Sign in failed") {
                val body = JSONObject().apply {
                    put("email", resolvedEmail)
                    put("password", password)
                }
                val json = postAuth("/auth/v1/token?grant_type=password", body)
                applySession(json)
                rememberEmail(resolvedEmail)
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
            clearStoredSession()
            _authState.value = AuthState.Unauthenticated
            _isLoading.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            runAuthCall("Failed to send reset email", updateState = false) {
                postAuth("/auth/v1/recover", JSONObject().put("email", email.trim().lowercase()))
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
                withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use {
                        if (!it.isSuccessful) throw AuthApiException(it.code, it.body?.string().orEmpty().ifBlank { it.message })
                    }
                }
            }
        }
    }

    fun refreshSession() = restoreSession()

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
            _authState.value = AuthState.Error(cleanAuthError(e, fallback))
        } finally {
            _isLoading.value = false
        }
    }

    private fun cleanAuthError(error: Exception, fallback: String): String {
        val raw = (error as? AuthApiException)?.apiMessage ?: error.message ?: fallback
        val lower = raw.lowercase()
        return when {
            "account already exists" in lower || "user_exists" in lower || "already registered" in lower ->
                "Account already exists. Sign in instead."
            "email not confirmed" in lower || "email_not_confirmed" in lower || "not verified" in lower ->
                "Account is ready, but sign-in was blocked by the server. Try forgot password or sign in again."
            "invalid login" in lower || "invalid_grant" in lower || "invalid credentials" in lower ->
                if (lastAttemptWasKnownEmail()) "Password is incorrect." else "Email is not registered. Create an account."
            raw.isBlank() -> fallback
            else -> raw.trim().take(180)
        }
    }

    private var lastResolvedEmail: String? = null

    private fun lastAttemptWasKnownEmail(): Boolean = lastResolvedEmail?.let { isKnownEmail(it) } == true

    private fun resolveLoginIdentifier(identifier: String): String? {
        val normalized = identifier.trim().lowercase()
        val email = if ("@" in normalized) normalized else prefs.getString(usernameKey(normalized), null)
        lastResolvedEmail = email
        return email
    }

    private suspend fun postAuth(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("${Supabase.SUPABASE_URL}$path")
            .headers(Supabase.headers())
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val parsed = runCatching { JSONObject(text) }.getOrNull()
                val message = parsed?.optString("msg")
                    ?.takeIf { it.isNotBlank() }
                    ?: parsed?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: text.ifBlank { res.message }
                val code = parsed?.optString("error_code")?.takeIf { it.isNotBlank() }
                    ?: parsed?.optString("code")?.takeIf { it.isNotBlank() }
                throw AuthApiException(res.code, message, code)
            }
            JSONObject(text.ifBlank { "{}" })
        }
    }

    private fun applySession(json: JSONObject) {
        val token = json.optString("access_token").takeIf { it.isNotBlank() }
        val refresh = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val userJson = json.optJSONObject("user") ?: json
        val user = parseUser(userJson)
        Supabase.setSession(token, user)
        if (token != null && user.id.isNotBlank()) {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .putString(KEY_REFRESH_TOKEN, refresh)
                .putString(KEY_USER_JSON, userJson.toString())
                .apply()
            user.email?.let { rememberEmail(it) }
            val username = user.userMetadata["username"]?.let { it as? JsonPrimitive }?.content?.trim()?.lowercase()
            if (!username.isNullOrBlank() && !user.email.isNullOrBlank()) rememberUsername(username, user.email)
        }
    }

    private fun restoreSession() {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val rawUser = prefs.getString(KEY_USER_JSON, null)
        val user = rawUser?.let { runCatching { parseUser(JSONObject(it)) }.getOrNull() }
        if (!token.isNullOrBlank() && user != null && user.id.isNotBlank()) {
            Supabase.setSession(token, user)
            _authState.value = AuthState.Authenticated(user)
        } else {
            Supabase.clearSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun clearStoredSession() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).remove(KEY_USER_JSON).apply()
    }

    private fun rememberEmail(email: String) {
        if (email.isBlank()) return
        val normalized = email.trim().lowercase()
        val current = prefs.getStringSet(KEY_KNOWN_EMAILS, emptySet()).orEmpty().toMutableSet()
        current.add(normalized)
        prefs.edit().putStringSet(KEY_KNOWN_EMAILS, current).apply()
    }

    private fun isKnownEmail(email: String): Boolean =
        email.trim().lowercase() in prefs.getStringSet(KEY_KNOWN_EMAILS, emptySet()).orEmpty()

    private fun rememberUsername(username: String, email: String) {
        if (username.isBlank() || email.isBlank()) return
        prefs.edit().putString(usernameKey(username.trim().lowercase()), email.trim().lowercase()).apply()
    }

    private fun usernameKey(username: String): String = "username_${username.trim().lowercase()}"

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

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_JSON = "user_json"
        private const val KEY_KNOWN_EMAILS = "known_emails"
    }
}
