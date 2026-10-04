package com.alvarosega.trackingventas.ui.login

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvarosega.trackingventas.R

private val IosBackgroundGrouped = Color(0xFFF2F2F7)
private val IosCardSurface = Color(0xFFFFFFFF)
private val IosBorderSeparator = Color(0xFFE5E5EA)
private val IosSystemBlue = Color(0xFF007AFF)
private val IosLabelPrimary = Color(0xFF000000)
private val IosLabelSecondary = Color(0xFF8E8E93)
private val IosSystemRed = Color(0xFFFF3B30)
private val IosSystemRedLight = Color(0xFFFFEBEA)

@Composable
fun LoginScreen(
    onLoginSuccess: (role: String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val passwordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state) {
        if (state is LoginUiState.Success) {
            onLoginSuccess((state as LoginUiState.Success).role)
            viewModel.resetState()
        }
    }

    val submitLogin: () -> Unit = {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val windowToken = (context as? Activity)?.window?.decorView?.windowToken
        if (imm != null && windowToken != null) {
            imm.hideSoftInputFromWindow(windowToken, 0)
        }
        viewModel.login(username.trim(), password.trim())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IosBackgroundGrouped)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Logo Central Estilo Apple
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color(0x1A000000),
                        spotColor = Color(0x1A000000)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(IosCardSurface)
                    .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tdb_logo),
                    contentDescription = "Logo TDB",
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TDB",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabelPrimary,
                    letterSpacing = (-0.5).sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ingresa tus credenciales para continuar",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = IosLabelSecondary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Formulario Agrupado Estilo iOS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(IosCardSurface)
                    .border(0.8.dp, IosBorderSeparator, RoundedCornerShape(16.dp))
            ) {
                // Fila 1: Usuario
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (username.isEmpty()) {
                        Text(
                            text = "Usuario",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = IosLabelSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                    BasicTextField(
                        value = username,
                        onValueChange = { username = it },
                        singleLine = true,
                        cursorBrush = SolidColor(IosSystemBlue),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = IosLabelPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .height(0.5.dp)
                        .background(IosBorderSeparator)
                )

                // Fila 2: Contraseña
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (password.isEmpty()) {
                        Text(
                            text = "Contraseña",
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = IosLabelSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                    BasicTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        cursorBrush = SolidColor(IosSystemBlue),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = IosLabelPrimary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitLogin() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Principal iOS
            Button(
                onClick = { submitLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IosSystemBlue,
                    disabledContainerColor = IosSystemBlue.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                enabled = state !is LoginUiState.Loading
            ) {
                if (state is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "Iniciar Sesión",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = state is LoginUiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (state is LoginUiState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(IosSystemRedLight)
                            .border(0.8.dp, IosSystemRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = (state as LoginUiState.Error).message,
                            style = TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = IosSystemRed
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}