package cl.duoc.veterinariaapp.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentidadClienteHistoricaTest {
    private val cliente = Cliente("José Pérez", "+56912345678", "jose@example.com", "Centro")

    @Test fun igualdadClienteCumpleContratoConNombreYCorreoNormalizados() {
        val segundo = Cliente(" JOSÉ   PÉREZ ", "12345678", "JOSE@example.com", "Otra dirección")
        val tercero = Cliente("josé pérez", null, "jose@EXAMPLE.COM", null)
        assertTrue(cliente == cliente)
        assertTrue(cliente == segundo && segundo == cliente)
        assertTrue(segundo == tercero && cliente == tercero)
        assertEquals(cliente.hashCode(), segundo.hashCode())
        assertEquals(segundo.hashCode(), tercero.hashCode())
        assertFalse(cliente.equals(null))
        assertFalse(cliente.equals("José Pérez"))
        assertNotEquals(cliente, Cliente("Otra persona", null, "jose@example.com", null))
        assertNotEquals(cliente, Cliente("José Pérez", null, "otro@example.com", null))
        assertEquals(1, hashSetOf(cliente, segundo, tercero).size)
        assertTrue(hashSetOf(cliente).contains(segundo))
    }
}
