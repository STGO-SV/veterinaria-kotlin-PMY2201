package cl.duoc.veterinariaapp.utilidades

import java.util.Locale

fun claveIdentidad(texto: String): String =
    texto.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
