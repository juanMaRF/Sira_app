package com.example.sira.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val resetState by viewModel.resetState.collectAsStateWithLifecycle()

    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val isLoading = uiState == AuthUiState.Loading

    // Cuando hay sesión activa, navega a "Mis plantas".
    LaunchedEffect(currentUser) {
        if (currentUser != null) onLoggedIn()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Encabezado ---
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(46.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sira",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (isRegister) "Crea tu cuenta" else "Inicia sesión para cuidar tus plantas",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // --- Formulario ---
        if (isRegister) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                viewModel.consumeError()
            },
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                viewModel.consumeError()
            },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña"
                        else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // --- ¿Olvidaste tu contraseña? (solo al iniciar sesión) ---
        if (!isRegister) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { showResetDialog = true },
                    enabled = !isLoading
                ) { Text("¿Olvidaste tu contraseña?") }
            }
        }

        // --- Error ---
        val state = uiState
        if (state is AuthUiState.Error) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        // --- Botón principal (iniciar sesión / crear cuenta) ---
        Button(
            onClick = {
                if (isRegister) viewModel.registerWithEmail(email, password, name)
                else viewModel.signInWithEmail(email, password)
            },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = if (isRegister) "Crear cuenta" else "Iniciar sesión",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // --- Alternar entre iniciar sesión / registrarse ---
        TextButton(
            onClick = {
                isRegister = !isRegister
                viewModel.consumeError()
            },
            enabled = !isLoading
        ) {
            Text(
                if (isRegister) "¿Ya tienes cuenta? Inicia sesión"
                else "¿No tienes cuenta? Crea una"
            )
        }

        Spacer(Modifier.height(8.dp))

        // --- Separador "o" ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "  o  ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // --- Google ---
        OutlinedButton(
            onClick = { viewModel.signInWithGoogle(context) },
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Continuar con Google", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showResetDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            resetState = resetState,
            onSend = { viewModel.sendPasswordReset(it) },
            onDismiss = {
                showResetDialog = false
                viewModel.consumeResetState()
            }
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    resetState: ResetUiState,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var emailInput by remember { mutableStateOf(initialEmail) }
    val sending = resetState == ResetUiState.Sending
    val sent = resetState == ResetUiState.Sent

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (sent) "Correo enviado" else "Restablecer contraseña") },
        text = {
            if (sent) {
                Text(
                    "Te enviamos un enlace a $emailInput para crear una nueva contraseña. " +
                        "Revisa tu bandeja de entrada (y la carpeta de spam)."
                )
            } else {
                Column {
                    Text(
                        "Ingresa tu correo y te enviaremos un enlace para restablecer tu contraseña.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        enabled = !sending,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetState is ResetUiState.Error) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = resetState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (sent) {
                TextButton(onClick = onDismiss) { Text("Entendido") }
            } else {
                TextButton(
                    onClick = { onSend(emailInput) },
                    enabled = !sending && emailInput.isNotBlank()
                ) { Text(if (sending) "Enviando…" else "Enviar enlace") }
            }
        },
        dismissButton = {
            if (!sent) {
                TextButton(onClick = onDismiss, enabled = !sending) { Text("Cancelar") }
            }
        }
    )
}
