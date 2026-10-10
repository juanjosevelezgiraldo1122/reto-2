package com.example.reto2.auth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF10172B)
private val Indigo = Color(0xFF5046E5)
private val IndigoDeep = Color(0xFF302A9B)
private val CanvasWhite = Color(0xFFFFFFFF)
private val PageTint = Color(0xFFF3F5FB)
private val Muted = Color(0xFF718096)

@Composable
fun AuthPage(
    title: String,
    description: String,
    onBackToMarket: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = PageTint) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .background(
                        Brush.linearGradient(listOf(Ink, IndigoDeep, Indigo))
                    )
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        radius = size.minDimension * 0.42f,
                        center = Offset(size.width * 0.96f, size.height * 0.02f)
                    )
                    drawCircle(
                        color = Color(0xFF8DE8D0).copy(alpha = 0.15f),
                        radius = size.minDimension * 0.22f,
                        center = Offset(size.width * 0.87f, size.height * 0.1f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.minDimension * 0.3f,
                        center = Offset(size.width * 0.05f, size.height * 0.95f)
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 28.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.13f),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .background(Color(0xFF8DE8D0), CircleShape)
                            )
                            Text(
                                "ESPACIO PERSONAL",
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.1.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 34.sp,
                        lineHeight = 39.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.7).sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        description,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .offset(y = (-24).dp),
                shape = RoundedCornerShape(28.dp),
                color = CanvasWhite,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    content = content
                )
            }
            if (onBackToMarket != null) {
                TextButton(
                    onClick = onBackToMarket,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Volver al mercado", color = Indigo, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
fun AuthInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CanvasWhite,
            unfocusedContainerColor = CanvasWhite,
            focusedIndicatorColor = Indigo,
            unfocusedIndicatorColor = Color(0xFFE0E4EF),
            focusedLabelColor = Indigo,
            cursorColor = Indigo
        )
    )
}

@Composable
fun AuthPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null
) {
    var visible by rememberSaveable(label) { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Ocultar" else "Mostrar", color = Indigo, fontSize = 12.sp)
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CanvasWhite,
            unfocusedContainerColor = CanvasWhite,
            focusedIndicatorColor = Indigo,
            unfocusedIndicatorColor = Color(0xFFE0E4EF),
            focusedLabelColor = Indigo,
            cursorColor = Indigo
        )
    )
}

@Composable
fun AuthPrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Indigo),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuthSwitchAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, color = Indigo, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AuthStatus(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF0F2FF)
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = IndigoDeep,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
