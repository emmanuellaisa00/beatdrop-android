package com.beatdrop.app.data.cloud

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class AuthState {
    data object Loading : AuthState()
    data object Unauthenticated : AuthState()
    data class Authenticated(val user: UserInfo) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            // Listen to session changes
            Supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _authState.value = AuthState.Authenticated(status.session.user)
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _authState.value = AuthState.Unauthenticated
                    }
                    // SDK 3.x renamed LoadingFromStorage -> Initializing and added
                    // RefreshFailure; treat anything else as a transient loading state.
                    else -> {
                        _authState.value = AuthState.Loading
                    }
                }
            }
        }
    }

    // ─── SIGN UP ───────────────────────────────────────
    fun signUp(email: String, password: String, displayName: String, username: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = buildJsonObject {
                        put("display_name", displayName)
                        put("username", username)
                    }
                }
                // After signUp, status will update via sessionStatus flow
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── SIGN IN ───────────────────────────────────────
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── SIGN OUT ──────────────────────────────────────
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Supabase.auth.signOut()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── PASSWORD RESET ────────────────────────────────
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Supabase.auth.resetPasswordForEmail(email)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── UPDATE PASSWORD (after recovery) ──────────────
    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Supabase.auth.updateUser {
                    password = newPassword
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Password update failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── REFRESH SESSION ───────────────────────────────
    fun refreshSession() {
        viewModelScope.launch {
            try {
                Supabase.auth.refreshCurrentSession()
            } catch (e: Exception) {
                // Silently fail — the sessionStatus flow will handle unauthenticated
            }
        }
    }

    val currentUser: UserInfo?
        get() = Supabase.auth.currentSessionOrNull()?.user

    val currentUserId: String?
        get() = currentUser?.id
}
