package cl.duoc.veterinariaapp.utilidades

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TelefonosHistoricosTest {
    @Test fun normalizaMovilesNacionalesYConCodigoDePais() {
        for (entrada in listOf("956781234", "+56956781234", "+56 9 5678 1234", " (+56) 9 5678 1234 ")) {
            assertEquals("(+56) 9 5678 1234", normalizarTelefono(entrada))
        }
    }

    @Test fun normalizaFijosYConservaTodosLosDigitosNacionales() {
        for (entrada in listOf("223456789", "+56223456789", "(2) 2345-6789")) {
            val normalizado = normalizarTelefono(entrada)
            assertNotNull(normalizado)
            assertEquals("(+56) 2 2345 6789", normalizado)
            assertEquals(entrada.filter(Char::isDigit).takeLast(9), normalizado!!.filter(Char::isDigit).drop(2))
        }
        assertEquals("(+56) 9 0123 4567", normalizarTelefono("+56 (9) 0123-4567"))
    }

    @Test fun rechazaPrefijosMalFormadosLongitudesIncorrectasYLetras() {
        for (entrada in listOf("9567812", "+5695678123", "+569567812345", "1234567890123456", "+56 9 5678 abcd",
            "++56956781234", "56+956781234", "(+57) 9 5678 1234", "+001234567890")) {
            assertNull(entrada, telefonoValido(entrada))
            assertNull(entrada, normalizarTelefono(entrada))
        }
    }
}
