package com.euphoria.aimentor.ui.screens

import android.app.Activity
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.euphoria.aimentor.R
import com.euphoria.aimentor.ui.theme.*
import com.euphoria.aimentor.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: MainViewModel, onAuthSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val context = LocalContext.current

    // FIX: Check if Google Sign-In is properly configured
    val webClientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) { "" }
    val isGoogleConfigured = webClientId.isNotBlank() &&
        !webClientId.startsWith("YOUR_") &&
        webClientId.contains(".apps.googleusercontent.com")

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.signInWithGoogle(it) }
            } catch (e: ApiException) {
                Log.e("AuthScreen", "Google Sign-In failed! Status Code: ${e.statusCode}")
                Toast.makeText(context, "Sign-In Error: ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("AuthScreen", "Google Sign-In cancelled or failed. Result code: ${result.resultCode}")
        }
    }

    // FIX: Clear error when switching between login/register
    LaunchedEffect(isLogin) {
        viewModel.clearError()
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onAuthSuccess()
        }
    }

    // Email validation
    val isEmailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordValid = password.isBlank() || password.length >= 6

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MentorBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MentorPrimary.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MentorPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = if (isLogin) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MentorOnSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                isError = email.isNotBlank() && !isEmailValid,
                supportingText = {
                    if (email.isNotBlank() && !isEmailValid) {
                        Text("Please enter a valid email", color = MentorError)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MentorPrimary,
                    unfocusedBorderColor = MentorSurfaceVariant,
                    focusedTextColor = MentorOnSurface,
                    unfocusedTextColor = MentorOnSurface,
                    errorBorderColor = MentorError
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MentorPrimary) },
                singleLine = true
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                isError = password.isNotBlank() && !isPasswordValid,
                supportingText = {
                    if (password.isNotBlank() && !isPasswordValid) {
                        Text("Password must be at least 6 characters", color = MentorError)
                    } else if (!isLogin && password.isNotBlank()) {
                        // Password strength indicator
                        val strength = when {
                            password.length >= 12 && password.any { it.isUpperCase() } && password.any { it.isDigit() } -> "Strong"
                            password.length >= 8 -> "Medium"
                            password.length >= 6 -> "Weak"
                            else -> ""
                        }
                        val color = when(strength) {
                            "Strong" -> MentorSuccess
                            "Medium" -> MentorWarning
                            "Weak" -> MentorError
                            else -> MentorOnSurface
                        }
                        if (strength.isNotBlank()) {
                            Text("Password strength: $strength", color = color)
                        }
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MentorPrimary,
                    unfocusedBorderColor = MentorSurfaceVariant,
                    focusedTextColor = MentorOnSurface,
                    unfocusedTextColor = MentorOnSurface,
                    errorBorderColor = MentorError
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MentorPrimary) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MentorOnSurface.copy(alpha = 0.4f)
                        )
                    }
                },
                singleLine = true
            )

            // Error / Success messages
            uiState.error?.let { error ->
                Text(error, color = MentorError, fontSize = 12.sp)
            }
            uiState.successMessage?.let { msg ->
                Text(msg, color = MentorSuccess, fontSize = 12.sp)
            }

            // Forgot Password (login mode only)
            if (isLogin) {
                TextButton(
                    onClick = {
                        forgotEmail = email
                        showForgotPassword = true
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?", color = MentorPrimary.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            Button(
                onClick = {
                    if (isLogin) viewModel.signIn(email, password)
                    else viewModel.signUp(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MentorPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isLoading && isEmailValid && isPasswordValid &&
                    email.isNotBlank() && password.isNotBlank()
            ) {
                if (uiState.isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(if (isLogin) "Login" else "Register", fontWeight = FontWeight.Bold)
            }

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MentorSurfaceVariant)
                Text(" OR ", color = MentorOnSurface.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = MentorSurfaceVariant)
            }

            // Google Sign In — only show if configured
            if (isGoogleConfigured) {
                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MentorSurfaceVariant)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MentorOnSurface)
                        Text("Continue with Google", color = MentorOnSurface)
                    }
                }
            }

            TextButton(onClick = {
                isLogin = !isLogin
                password = ""
            }) {
                Text(if (isLogin) "Don't have an account? Register" else "Already have an account? Login", color = MentorPrimary)
            }

            TextButton(onClick = { viewModel.signInAnonymously() }) {
                Text("Continue as Guest", color = MentorOnSurface.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Reset Password", color = MentorOnSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your email to receive a password reset link.", color = MentorOnSurface.copy(alpha = 0.7f), fontSize = 14.sp)
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MentorPrimary,
                            focusedTextColor = MentorOnSurface,
                            unfocusedTextColor = MentorOnSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.sendPasswordReset(forgotEmail)
                    showForgotPassword = false
                }) {
                    Text("Send Reset Link", color = MentorPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Cancel", color = MentorOnSurface.copy(alpha = 0.5f))
                }
            },
            containerColor = MentorSurface
        )
    }
}
