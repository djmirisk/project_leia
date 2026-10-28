package com.solaresenergy.sisfv.ui.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

    // Campo que recibe los dígitos del teclado en pantalla: 0 = documento, 1 = PIN.
    var campoActivo by rememberSaveable { mutableStateOf(CAMPO_DOCUMENTO) }

    // El login usa su propio teclado numérico para no depender del teclado del
    // sistema. En tabletas MIUI el IME del sistema puede quedar flotante y sin
    // teclas, impidiendo escribir la cédula; con teclado propio eso no ocurre.
    val agregarDigito: (Char) -> Unit = { digito ->
        if (campoActivo == CAMPO_DOCUMENTO) {
            if (uiState.documento.length < 15) {
                viewModel.onDocumentoChanged(uiState.documento + digito)
            }
        } else {
            if (uiState.passwordPin.length < 20) {
                viewModel.onPasswordPinChanged(uiState.passwordPin + digito)
            }
        }
    }

    val borrarDigito: () -> Unit = {
        if (campoActivo == CAMPO_DOCUMENTO) {
            if (uiState.documento.isNotEmpty()) {
                viewModel.onDocumentoChanged(uiState.documento.dropLast(1))
            }
        } else {
            if (uiState.passwordPin.isNotEmpty()) {
                viewModel.onPasswordPinChanged(uiState.passwordPin.dropLast(1))
            }
        }
    }

    val avanzar: () -> Unit = {
        if (campoActivo == CAMPO_DOCUMENTO) {
            campoActivo = CAMPO_PIN
        } else if (!uiState.isLoading) {
            viewModel.iniciarSesion()
        }
    }

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
                .verticalScroll(rememberScrollState())
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
                    .height(130.dp),
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
                modifier = Modifier.height(12.dp)
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

                    CampoTeclado(
                        etiqueta = "Cédula / Documento de Identidad",
                        valor = uiState.documento,
                        placeholder = "Número de documento",
                        esActivo = campoActivo == CAMPO_DOCUMENTO,
                        esPassword = false,
                        colorActivo = SolarGreen,
                        onClick = { campoActivo = CAMPO_DOCUMENTO }
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    // =================================================
                    // PIN
                    // =================================================

                    CampoTeclado(
                        etiqueta = "Contraseña o PIN",
                        valor = uiState.passwordPin,
                        placeholder = "PIN del técnico",
                        esActivo = campoActivo == CAMPO_PIN,
                        esPassword = true,
                        colorActivo = SolarPrimary,
                        onClick = { campoActivo = CAMPO_PIN }
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // =================================================
                    // TECLADO NUMÉRICO PROPIO
                    // =================================================

                    TecladoNumericoLogin(
                        onDigito = agregarDigito,
                        onBorrar = borrarDigito,
                        onAccion = avanzar,
                        textoAccion = if (campoActivo == CAMPO_DOCUMENTO) "SIGUIENTE" else "ENTRAR",
                        habilitado = !uiState.isLoading
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
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

// =============================================================
// COMPONENTES DEL TECLADO NUMÉRICO DE LOGIN
// No usan TextField ni el teclado del sistema: evitan que un IME
// defectuoso (tabletas MIUI/Gboard) impida escribir la cédula.
// =============================================================

private const val CAMPO_DOCUMENTO = 0
private const val CAMPO_PIN = 1

@Composable
private fun CampoTeclado(
    etiqueta: String,
    valor: String,
    placeholder: String,
    esActivo: Boolean,
    esPassword: Boolean,
    colorActivo: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = etiqueta,
            fontSize = 12.sp,
            fontWeight = if (esActivo) FontWeight.Bold else FontWeight.Normal,
            color = if (esActivo) colorActivo else Color(0xFF6B7B72)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(
                    width = if (esActivo) 2.dp else 1.dp,
                    color = if (esActivo) colorActivo else Color(0xFFC9D3CD),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val textoVisible = when {
                valor.isEmpty() -> placeholder
                esPassword -> "•".repeat(valor.length)
                else -> valor
            }

            Text(
                text = textoVisible,
                fontSize = if (valor.isEmpty()) 14.sp else 22.sp,
                fontWeight = if (valor.isEmpty()) FontWeight.Normal else FontWeight.Medium,
                letterSpacing = if (esPassword && valor.isNotEmpty()) 4.sp else 1.sp,
                color = if (valor.isEmpty()) Color(0xFF9AA8A0) else Color(0xFF1B1B1B)
            )
        }
    }
}

@Composable
private fun TecladoNumericoLogin(
    onDigito: (Char) -> Unit,
    onBorrar: () -> Unit,
    onAccion: () -> Unit,
    textoAccion: String,
    habilitado: Boolean
) {
    val filas = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9')
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        filas.forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                fila.forEach { digito ->
                    BotonTecla(
                        texto = digito.toString(),
                        modifier = Modifier.weight(1f),
                        habilitado = habilitado
                    ) {
                        onDigito(digito)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BotonTecla(
                texto = "⌫",
                modifier = Modifier.weight(1f),
                habilitado = habilitado,
                onClick = onBorrar
            )

            BotonTecla(
                texto = "0",
                modifier = Modifier.weight(1f),
                habilitado = habilitado
            ) {
                onDigito('0')
            }

            BotonTecla(
                texto = textoAccion,
                modifier = Modifier.weight(1.7f),
                habilitado = habilitado,
                destacado = true,
                onClick = onAccion
            )
        }
    }
}

@Composable
private fun BotonTecla(
    texto: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    destacado: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(0.dp),
        border = if (destacado) null else BorderStroke(1.dp, Color(0xFFD5DED8)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destacado) SolarGreen else Color(0xFFF1F4F2),
            contentColor = if (destacado) Color.White else Color(0xFF1F3D2B),
            disabledContainerColor = Color(0xFFE6E6E6),
            disabledContentColor = Color(0xFF9E9E9E)
        )
    ) {
        Text(
            text = texto,
            fontSize = if (destacado) 12.sp else 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
