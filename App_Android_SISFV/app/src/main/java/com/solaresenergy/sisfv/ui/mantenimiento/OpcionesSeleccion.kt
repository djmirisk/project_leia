package com.solaresenergy.sisfv.ui.mantenimiento

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Casilla cuya fila completa (cuadro y texto) responde al toque.
@Composable
internal fun FilaCasilla(
    texto: String,
    marcado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .toggleable(value = marcado, role = Role.Checkbox, onValueChange = onCambio)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sin acción propia: el toque lo gestiona la fila. El relleno conserva el área de 48 dp.
        Checkbox(checked = marcado, onCheckedChange = null, modifier = Modifier.padding(14.dp))
        Text(texto)
    }
}

// Opción de selección única cuya fila completa (círculo y texto) responde al toque.
@Composable
internal fun OpcionRadio(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .selectable(selected = seleccionado, role = Role.RadioButton, onClick = onClick)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = seleccionado, onClick = null, modifier = Modifier.padding(14.dp))
        Text(texto)
    }
}
