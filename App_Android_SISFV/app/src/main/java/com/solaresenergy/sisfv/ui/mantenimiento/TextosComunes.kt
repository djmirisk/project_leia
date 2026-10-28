package com.solaresenergy.sisfv.ui.mantenimiento

/**
 * Normaliza un nombre propio: cada palabra empieza en mayúscula y el resto queda
 * en minúscula. Se aplica al escribir porque algunos teclados (tabletas MIUI)
 * ignoran la sugerencia de capitalización automática del campo.
 */
internal fun capitalizarNombre(texto: String): String =
    texto.split(" ").joinToString(" ") { palabra ->
        if (palabra.isEmpty()) {
            palabra
        } else {
            palabra.replaceFirstChar { caracter ->
                caracter.uppercaseChar()
            }
        }
    }
