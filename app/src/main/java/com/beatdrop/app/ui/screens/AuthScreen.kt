package com.beatdrop.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.beatdrop.app.data.cloud.AuthState
import com.beatdrop.app.ui.theme.BeatColors
import com.beatdrop.app.ui.theme.BeatType
import com.beatdrop.app.ui.theme.PillActiveBrush

private enum class AuthMode { SignIn, SignUp, Reset }

/**
 * Optional account screen — sign in / create account / reset password.
 * The app is fully usable signed-out; this only appears when the user opts in
 * to cloud sync from Profile. Drives the cloud AuthManager.
 */
@Composable
fun AuthScreen(
    state: AuthState,
    isLoading: Boolean,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String, displayName: String, username: String) -> Unit,
    onReset: (email: String) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val error = (state as? AuthState.Error)?.message

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1E0F1A), Color(0xFF0B070E), Color(0xFF000000)))
            )
    ) {
        // Close
        Box(
            Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Close, "Close", tint = Color.White, modifier = Modifier.size(18.dp)) }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(80.dp))
            // Logo mark
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(PillActiveBrush),
                contentAlignment = Alignment.Center
            ) { Text("♪", style = BeatType.LargeTitle.copy(fontSize = 36.sp), color = Color.White) }

            Text(
                when (mode) {
                    AuthMode.SignIn -> "Welcome back"
                    AuthMode.SignUp -> "Create your account"
                    AuthMode.Reset -> "Reset password"
                },
                style = BeatType.LargeTitle.copy(fontSize = 28.sp, letterSpacing = (-0.03f).em),
                color = BeatColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                "Sign in to sync your library across devices",
                style = BeatType.CardSub, color = BeatColors.TextSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(28.dp))

            if (mode == AuthMode.SignUp) {
                AuthField(displayName, { displayName = it }, "Display name")
                Spacer(Modifier.height(12.dp))
                AuthField(username, { username = it }, "Username")
                Spacer(Modifier.height(12.dp))
            }
            AuthField(email, { email = it }, "Email", keyboard = KeyboardType.Email)
            if (mode != AuthMode.Reset) {
                Spacer(Modifier.height(12.dp))
                AuthField(password, { password = it }, "Password", isPassword = true)
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error, style = BeatType.CardSub, color = BeatColors.Accent, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(22.dp))

            // Primary CTA
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(PillActiveBrush)
                    .clickable(enabled = !isLoading) {
                        when (mode) {
                            AuthMode.SignIn -> onSignIn(email.trim(), password)
                            AuthMode.SignUp -> onSignUp(email.trim(), password, displayName.trim(), username.trim())
                            AuthMode.Reset -> onReset(email.trim())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        when (mode) {
                            AuthMode.SignIn -> "Sign in"
                            AuthMode.SignUp -> "Create account"
                            AuthMode.Reset -> "Send reset link"
                        },
                        style = BeatType.Pill.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Mode switches
            when (mode) {
                AuthMode.SignIn -> {
                    AuthLink("New here? Create an account") { mode = AuthMode.SignUp }
                    Spacer(Modifier.height(8.dp))
                    AuthLink("Forgot password?") { mode = AuthMode.Reset }
                }
                AuthMode.SignUp -> AuthLink("Already have an account? Sign in") { mode = AuthMode.SignIn }
                AuthMode.Reset -> AuthLink("Back to sign in") { mode = AuthMode.SignIn }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x17FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = BeatType.CardSub.copy(fontSize = 14.sp), color = Color(0x73FFFFFF))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = BeatType.CardSub.copy(fontSize = 14.sp, color = Color.White),
            cursorBrush = SolidColor(BeatColors.Accent),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AuthLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = BeatType.CardSub.copy(fontWeight = FontWeight.SemiBold),
        color = BeatColors.TextSecondary,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    )
}
