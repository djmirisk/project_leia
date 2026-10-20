package com.solaresenergy.sisfv.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.solaresenergy.sisfv.R
import com.solaresenergy.sisfv.data.local.entity.TecnicoEntity
import com.solaresenergy.sisfv.ui.theme.SolarAccent
import com.solaresenergy.sisfv.ui.theme.SolarGreen
import com.solaresenergy.sisfv.ui.theme.SolarPrimary


@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (TecnicoEntity) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess && uiState.tecnicoLogueado != null) {
            onLoginSuccess(uiState.tecnicoLogueado!!)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // =========================================================
        // FONDO CORPORATIVO
        // =========================================================

        Image(
            painter = painterResource(
                id = R.drawable.solares_fondo
            ),
            contentDescription = "Fondo Solares Energy Group",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // =========================================================
        // DEGRADADO CORPORATIVO
        // Oscuro en la parte superior y transparente hacia abajo
        // =========================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.48f)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.68f),
                            Color.Black.copy(alpha = 0.42f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        // =========================================================
        // DEGRADADO SUAVE INFERIOR
        // Para mejorar la lectura de las credenciales
        // =========================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    )
                )
        )

        // =========================================================
        // CONTENIDO
        // =========================================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
// =========================================================
// LOGO CORPORATIVO
// =========================================================

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                contentAlignment = Alignment.Center
            ) {

                Image(
                    painter = painterResource(
                        id = R.drawable.solares_logo
                    ),
                    contentDescription = "Solares Energy Group",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )
            Spacer(
                modifier = Modifier.height(18.dp)
            )

            // =====================================================
            // TÍTULO
            // =====================================================

            Text(
                text = "SISFV",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Sistema de Información Solar Fotovoltaico",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            // =====================================================
            // CARD DE LOGIN
            // =====================================================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.96f)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Inicio de Sesión Técnico",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarPrimary,
                        modifier = Modifier.padding(
                            bottom = 16.dp
                        )
                    )

                    // =================================================
                    // DOCUMENTO
                    // =================================================

                    OutlinedTextField(
                        value = uiState.documento,
                        onValueChange = {
                            viewModel.onDocumentoChanged(it)
                        },
                        label = {
                            Text("Cédula / Documento de Identidad")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolarGreen,
                            focusedLabelColor = SolarGreen
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // =================================================
                    // PIN
                    // =================================================

                    OutlinedTextField(
                        value = uiState.passwordPin,
                        onValueChange = {
                            viewModel.onPasswordPinChanged(it)
                        },
                        label = {
                            Text("Contraseña o PIN")
                        },
                        singleLine = true,
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolarPrimary,
                            focusedLabelColor = SolarPrimary
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // =================================================
                    // ERROR
                    // =================================================

                    if (uiState.errorMessage != null) {

                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(
                                bottom = 12.dp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    // =================================================
                    // BOTÓN LOGIN
                    // =================================================

                    Button(
                        onClick = {
                            viewModel.iniciarSesion()
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarGreen
                        )
                    ) {

                        if (uiState.isLoading) {

                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )

                        } else {

                            Text(
                                text = "INICIAR SESIÓN EN CAMPO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // =====================================================
            // IDENTIFICACIÓN DE LA APP
            // =====================================================

            Text(
                text = "Mantenimiento SISFV · ZNI",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.90f),
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // =====================================================
            // CREDENCIALES DE PRUEBA
            // =====================================================

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.45f)
            ) {

                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Credenciales de prueba inicial",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarAccent
                    )

                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = "123456789 / 1234   •   987654321 / 1234",
                        fontSize = 10.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}