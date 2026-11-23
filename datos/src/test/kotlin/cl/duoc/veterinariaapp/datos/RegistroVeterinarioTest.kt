package cl.duoc.veterinariaapp.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistroVeterinarioTest {
    @Test fun modeloConservaPredeterminadosYDesestructuracion() {
        for (cliente in listOf(Cliente(null, null, null, null), Cliente(" ", "123", "mal", " "))) {
            val (nombre, correo, telefono) = cliente
            assertEquals("Cliente sin nombre", nombre)
            assertEquals("correo@invalido.com", correo)
            assertEquals("No informado", telefono)
            assertEquals("Retiro en clínica", cliente.direccion)
            assertNull(cliente.correoNotificable)
        }
        assertEquals("ana@example.cl", Cliente("Ana", null, " ana@example.cl ", null).correoNotificable)
    }

    @Test fun registroValidaElCorreoOriginalYAdmiteTelefonoAusenteOInvalido() {
        val clinica = ClinicaVeterinaria()
        for (correo in listOf(null, " ", "sin-arroba")) {
            assertThrows(IllegalArgumentException::class.java) { clinica.registrarCliente("Ana", null, correo, null) }
        }
        assertEquals(ResumenClinica(0, 0, null), clinica.resumen())
        assertTrue(clinica.registrarCliente(null, "mal", " ana@example.cl ", null))
        val cliente = clinica.clientesRegistrados().single()
        assertEquals("Cliente sin nombre", cliente.nombre)
        assertEquals("No informado", cliente.telefono)
        assertEquals("ana@example.cl", cliente.correo)
    }

    @Test fun registroDuplicadoConservaLaPrimeraFichaYElUltimoClienteNuevo() {
        val clinica = ClinicaVeterinaria()
        assertTrue(clinica.registrarCliente("José Pérez", "+56912345678", "jose@example.com", "Centro"))
        assertTrue(clinica.registrarCliente("Ana", null, "ana@example.com", null))
        val (primero, segundo) = clinica.clientesRegistrados()
        assertFalse(clinica.registrarCliente(" JOSÉ   PÉREZ ", "12345678", "JOSE@example.com", "Otra dirección"))
        assertSame(primero, clinica.clientesRegistrados().first())
        assertEquals("Centro", primero.direccion)
        assertEquals(ResumenClinica(2, 0, segundo), clinica.resumen())
        assertSame(segundo, clinica.resumen().ultimoCliente)
    }

    @Test fun identidadDeProductoUsaNombreYCategoriaConHashConsistente() {
        val primero = Producto("Pelota de goma", 2_000, CategoriaProducto.JUGUETES)
        val segundo = Producto(" PELOTA  DE GOMA ", 1, CategoriaProducto.JUGUETES)
        val tercero = Producto("pelota de goma", 2, CategoriaProducto.JUGUETES)
        val otraCategoria = Producto("Pelota de goma", 2_000, CategoriaProducto.OTROS)
        assertEquals(primero, segundo)
        assertEquals(segundo, tercero)
        assertEquals(primero, tercero)
        assertEquals(primero.hashCode(), segundo.hashCode())
        assertEquals(segundo.hashCode(), tercero.hashCode())
        assertEquals(2, hashSetOf(primero, segundo, tercero, otraCategoria).size)
        assertNotEquals(primero, otraCategoria)
        assertThrows(IllegalArgumentException::class.java) { Producto(" ", 1, CategoriaProducto.JUGUETES) }
        for (precio in listOf(-1L, 0L)) {
            assertThrows(IllegalArgumentException::class.java) { Producto("Pelota", precio, CategoriaProducto.JUGUETES) }
        }
    }

    @Test fun catalogoInicialCubreCategoriasYCareceDeElegibilidadPromocional() {
        val clinica = ClinicaVeterinaria()
        val catalogo = clinica.productosRegistrados()
        assertEquals(CategoriaProducto.entries, catalogo.map { it.categoria })
        assertEquals("Antiparasitario (gatos y perros)", catalogo.first().nombre)
        assertTrue(catalogo.all { it.precio > 0 && !esPromocionable(it) })
        assertTrue(clinica.existencias().values.all { it > 0 })
    }

    @Test fun registrarProductosConservaElOrdenLaPrimeraFichaYSuStock() {
        val clinica = ClinicaVeterinaria(emptyMap())
        val a = Producto("Cama", 10_000, CategoriaProducto.OTROS)
        val b = Producto("Pelota", 2_000, CategoriaProducto.JUGUETES)
        val duplicado = Producto(" CAMA ", 5, CategoriaProducto.OTROS)
        assertTrue(clinica.registrarProducto(a, 2))
        assertTrue(clinica.registrarProducto(b))
        assertFalse(clinica.registrarProducto(duplicado, 99))
        assertEquals(listOf(a, b), clinica.productosRegistrados())
        assertSame(a, clinica.productosRegistrados().first())
        assertEquals(2L, clinica.existencias()[a])
        assertThrows(IllegalArgumentException::class.java) { clinica.registrarProducto(Producto("Y", 1, CategoriaProducto.OTROS), -1) }
        assertEquals(2, clinica.productosRegistrados().size)
    }
}
