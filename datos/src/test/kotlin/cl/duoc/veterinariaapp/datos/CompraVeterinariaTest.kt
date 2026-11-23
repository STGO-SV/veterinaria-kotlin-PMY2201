package cl.duoc.veterinariaapp.datos

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CompraVeterinariaTest {
    private val alimento = Producto("Alimento", 8_000, CategoriaProducto.ALIMENTOS)
    private val juguete = Producto("Pelota", 9_000, CategoriaProducto.JUGUETES)
    private val cliente = Cliente("Ana Pérez", "912345678", "ana@example.cl", null)
    private val zona = ZoneId.of("America/Santiago")
    private val reloj = Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), zona)
    private val fecha = LocalDate.of(2026, 11, 3)

    private fun clinicaConCliente(stock: Map<Producto, Long> = mapOf(alimento to 10L, juguete to 5L)): ClinicaVeterinaria =
        ClinicaVeterinaria(stock, reloj = reloj).apply {
            registrarCliente(cliente.nombre, cliente.telefono, cliente.correo, null)
        }

    @Test fun agrupaModificaQuitaYVaciaManteniendoOrdenYFichaCanonica() {
        val clinica = clinicaConCliente()
        val equivalente = Producto(" ALIMENTO ", 1, CategoriaProducto.ALIMENTOS)
        clinica.agregarAlCarrito(alimento, 1)
        clinica.agregarAlCarrito(juguete, 1)
        clinica.agregarAlCarrito(equivalente, 2)
        assertEquals(listOf(LineaPedido(alimento, 3), LineaPedido(juguete, 1)), clinica.lineasCarrito())
        clinica.modificarCantidad(equivalente, 2)
        assertSame(alimento, clinica.lineasCarrito().first().producto)
        assertEquals(16_000L + 9_000L, clinica.cotizar().subtotal)
        assertTrue(clinica.quitarDelCarrito(equivalente))
        assertFalse(clinica.quitarDelCarrito(equivalente))
        assertEquals(listOf(LineaPedido(juguete, 1)), clinica.lineasCarrito())
        clinica.vaciarCarrito()
        assertTrue(clinica.lineasCarrito().isEmpty())
    }

    @Test fun limitesInvalidosNoCreanNiModificanLineas() {
        val clinica = clinicaConCliente(mapOf(alimento to 150L))
        for (cantidad in listOf(0, -1, 101, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { clinica.agregarAlCarrito(alimento, cantidad) }
            assertThrows(IllegalArgumentException::class.java) { LineaPedido(alimento, cantidad) }
        }
        assertTrue(clinica.lineasCarrito().isEmpty())
        val desconocido = Producto("Correa", 1_000, CategoriaProducto.OTROS)
        assertThrows(IllegalArgumentException::class.java) { clinica.agregarAlCarrito(desconocido, 1) }
        assertThrows(IllegalArgumentException::class.java) { clinica.modificarCantidad(alimento, 1) }
        clinica.agregarAlCarrito(alimento, 99)
        clinica.agregarAlCarrito(alimento, 1)
        assertThrows(IllegalArgumentException::class.java) { clinica.agregarAlCarrito(alimento, 1) }
        for (cantidad in listOf(0, 101, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { clinica.modificarCantidad(alimento, cantidad) }
        }
        assertEquals(100, clinica.lineasCarrito().single().cantidad)
        clinica.modificarCantidad(alimento, 1)
        assertEquals(1, clinica.lineasCarrito().single().cantidad)
    }

    @Test fun cotizarCalculaSinConfirmarNiReservarExistencias() {
        val clinica = clinicaConCliente()
        val stock = clinica.existencias()
        clinica.agregarAlCarrito(alimento, 2)
        clinica.agregarAlCarrito(juguete, 2)
        val lineas = clinica.lineasCarrito()
        assertEquals(Importes(34000, 0, 6460, 40460), clinica.cotizar())
        assertEquals(stock, clinica.existencias())
        assertEquals(lineas, clinica.lineasCarrito())
        assertEquals(0, clinica.resumen().totalPedidos)
        assertTrue(clinica.pedidosConfirmados().isEmpty())
    }

    @Test fun confirmaConFichaCanonicaCopiaLineasYActualizaElResumen() {
        val clinica = clinicaConCliente()
        val ficha = clinica.clientesRegistrados().single()
        val duplicado = Cliente(" ANA  PÉREZ ", null, "ANA@example.cl", "Otro domicilio")
        clinica.agregarAlCarrito(alimento, 10)
        clinica.agregarAlCarrito(juguete, 2)
        val cotizacion = clinica.cotizar()
        val pedido = clinica.confirmarPedido(duplicado)
        val (clienteDelPedido, lineas, total) = pedido
        assertSame(ficha, clienteDelPedido)
        assertEquals(cotizacion.total, total)
        assertEquals(cotizacion, pedido.calcularImportes())
        assertEquals(fecha, pedido.fecha)
        assertEquals("PED-001", pedido.idPedido)
        assertEquals(12, pedido.cantidadArticulos)
        assertEquals(listOf(LineaPedido(alimento, 10), LineaPedido(juguete, 2)), lineas)
        assertEquals(mapOf(alimento to 0L, juguete to 3L), clinica.existencias())
        assertTrue(clinica.lineasCarrito().isEmpty())
        assertEquals(ResumenClinica(1, 1, ficha), clinica.resumen())
        clinica.agregarAlCarrito(juguete, 1)
        assertEquals("PED-002", clinica.confirmarPedido(cliente).idPedido)
        assertEquals(2, clinica.resumen().totalPedidos)
        assertEquals(12, pedido.cantidadArticulos)
    }

    @Test fun agregarYEditarAceptanElStockExactoYRechazanElExcesoSinCambiarEstado() {
        val clinica = clinicaConCliente(mapOf(alimento to 10L, juguete to 2L))
        val inicial = clinica.existencias()
        clinica.agregarAlCarrito(alimento, 1)
        clinica.agregarAlCarrito(juguete, 1)
        clinica.agregarAlCarrito(juguete, 1)
        val lineas = listOf(LineaPedido(alimento, 1), LineaPedido(juguete, 2))
        val cotizacion = clinica.cotizar()
        val resumen = clinica.resumen()

        val operaciones = listOf(
            { clinica.agregarAlCarrito(juguete, 1) },
            { clinica.modificarCantidad(juguete, 3) }
        )
        for (operar in operaciones) {
            val rechazo = assertThrows(StockInsuficiente::class.java) { operar() }
            assertEquals(listOf(Faltante(juguete, 2, 3)), rechazo.faltantes)
            assertEquals(inicial, clinica.existencias())
            assertEquals(lineas, clinica.lineasCarrito())
            assertEquals(cotizacion, clinica.cotizar())
            assertEquals(resumen, clinica.resumen())
            assertTrue(clinica.pedidosConfirmados().isEmpty())
        }

        clinica.modificarCantidad(juguete, 1)
        clinica.reabastecer(juguete, 1)
        clinica.modificarCantidad(juguete, 3)
        assertEquals(3, clinica.lineasCarrito().last().cantidad)
        assertEquals(3L, clinica.existencias()[juguete])
        assertEquals("PED-001", clinica.confirmarPedido(cliente).idPedido)
        assertEquals(0L, clinica.existencias()[juguete])
        assertEquals(1, clinica.resumen().totalPedidos)
    }

    @Test fun agregarRechazaStockAusenteYCero() {
        val clinica = clinicaConCliente(mapOf(alimento to 0L))
        clinica.registrarProducto(juguete)
        val inicial = clinica.existencias()
        val sinStock = assertThrows(StockInsuficiente::class.java) { clinica.agregarAlCarrito(alimento, 1) }
        val sinRegistro = assertThrows(StockInsuficiente::class.java) { clinica.agregarAlCarrito(juguete, 2) }
        assertEquals(listOf(Faltante(alimento, 0, 1)), sinStock.faltantes)
        assertEquals(listOf(Faltante(juguete, 0, 2)), sinRegistro.faltantes)
        assertTrue(clinica.lineasCarrito().isEmpty())
        assertEquals(inicial, clinica.existencias())
        assertEquals(0, clinica.resumen().totalPedidos)
    }

    @Test fun consumoAtomicoRechazaTodosLosFaltantesYConservaExistencias() {
        val tercero = Producto("Correa", 1_000, CategoriaProducto.OTROS)
        val inicial = mapOf(alimento to 10L, juguete to 1L)
        val inventario = Inventario(inicial)
        val lineas = listOf(
            LineaPedido(alimento, 1), LineaPedido(juguete, 1),
            LineaPedido(juguete, 1), LineaPedido(tercero, 1)
        )

        val rechazo = assertThrows(StockInsuficiente::class.java) { inventario.consumir(lineas) }

        assertEquals(listOf(Faltante(juguete, 1, 2), Faltante(tercero, 0, 1)), rechazo.faltantes)
        assertEquals(inicial, inventario.consultar())
    }

    @Test fun requiereClienteRegistradoYCarritoNoVacio() {
        val clinica = clinicaConCliente()
        assertThrows(IllegalArgumentException::class.java) { clinica.cotizar() }
        assertThrows(IllegalArgumentException::class.java) { clinica.confirmarPedido(cliente) }
        clinica.agregarAlCarrito(alimento, 1)
        val otro = Cliente("Otra persona", null, "otra@example.cl", null)
        val stock = clinica.existencias()
        assertThrows(IllegalArgumentException::class.java) { clinica.confirmarPedido(otro) }
        assertEquals(stock, clinica.existencias())
        assertEquals(listOf(LineaPedido(alimento, 1)), clinica.lineasCarrito())
        assertEquals(0, clinica.resumen().totalPedidos)
    }

    @Test fun desbordamientosDeLineaSubtotalYTotalNoAlteranLaCompra() {
        val extremo = Producto("Precio extremo", Long.MAX_VALUE, CategoriaProducto.OTROS)
        val clinica = clinicaConCliente(mapOf(extremo to 2L, juguete to 1L))
        val inicial = clinica.existencias()
        clinica.agregarAlCarrito(extremo, 2)
        comprobarDesbordamiento(clinica, inicial)
        clinica.modificarCantidad(extremo, 1)
        clinica.agregarAlCarrito(juguete, 1)
        comprobarDesbordamiento(clinica, inicial)
        clinica.quitarDelCarrito(juguete)
        comprobarDesbordamiento(clinica, inicial)
    }

    private fun comprobarDesbordamiento(clinica: ClinicaVeterinaria, inicial: Map<Producto, Long>) {
        val lineas = clinica.lineasCarrito()
        assertThrows(ArithmeticException::class.java) { clinica.cotizar() }
        assertThrows(ArithmeticException::class.java) { clinica.confirmarPedido(cliente) }
        assertEquals(inicial, clinica.existencias())
        assertEquals(lineas, clinica.lineasCarrito())
        assertEquals(0, clinica.resumen().totalPedidos)
    }

    @Test fun reabastecerConDesbordamientoPreservaLasExistencias() {
        val clinica = clinicaConCliente(mapOf(alimento to Long.MAX_VALUE))
        assertThrows(ArithmeticException::class.java) { clinica.reabastecer(alimento, 1) }
        assertEquals(Long.MAX_VALUE, clinica.existencias()[alimento])
    }

    @Test fun consultaYCambioDeColeccionesExternasNoAlcanzanElEstadoInterno() {
        val inicial = linkedMapOf(alimento to 10L, juguete to 5L)
        val clinica = clinicaConCliente(inicial)
        inicial.clear()
        clinica.registrarCliente("Otra", null, "otra@example.cl", null)
        (clinica.clientesRegistrados() as MutableList<Cliente>).clear()
        (clinica.productosRegistrados() as MutableList<Producto>).clear()
        (clinica.existencias() as MutableMap<Producto, Long>).clear()
        clinica.agregarAlCarrito(alimento, 1)
        clinica.agregarAlCarrito(juguete, 1)
        (clinica.lineasCarrito() as MutableList<LineaPedido>).clear()
        val primero = clinica.confirmarPedido(cliente)
        (primero.productos as MutableList<LineaPedido>).clear()
        clinica.agregarAlCarrito(juguete, 1)
        clinica.confirmarPedido(cliente)
        (clinica.pedidosConfirmados() as MutableList<Pedido>).clear()
        assertEquals(2, primero.productos.size)
        assertEquals(2, clinica.productosRegistrados().size)
        assertEquals(2, clinica.clientesRegistrados().size)
        assertEquals(2, clinica.pedidosConfirmados().size)
        assertEquals(mapOf(alimento to 9L, juguete to 3L), clinica.existencias())
    }

    @Test fun confirmarDesdeDosHilosRegistraYConsumeUnaSolaVez() {
        val clinica = clinicaConCliente()
        clinica.agregarAlCarrito(alimento, 1)
        val inicio = CountDownLatch(1)
        val hilos = Executors.newFixedThreadPool(2)
        try {
            val intentos = List(2) {
                hilos.submit(Callable {
                    check(inicio.await(5, TimeUnit.SECONDS))
                    try {
                        clinica.confirmarPedido(cliente)
                        true
                    } catch (_: IllegalArgumentException) {
                        false
                    }
                })
            }
            inicio.countDown()
            assertEquals(1, intentos.count { it.get(5, TimeUnit.SECONDS) })
        } finally {
            hilos.shutdownNow()
        }
        assertEquals(1, clinica.resumen().totalPedidos)
        assertEquals(9L, clinica.existencias()[alimento])
    }

    @Test fun promocionUsaFechaDeSantiagoYLaAnotacionDeLaFichaCanonica() {
        val anotado = AlimentoPromocional()
        val equivalente = Producto(anotado.nombre, 1, anotado.categoria)
        val promocion = Promocion(fecha, fecha, BigDecimal("0.15"))
        val clinica = ClinicaVeterinaria(mapOf(anotado to 2L), promocion, reloj)
        clinica.registrarCliente(cliente.nombre, cliente.telefono, cliente.correo, null)
        clinica.agregarAlCarrito(equivalente, 1)
        assertSame(anotado, clinica.lineasCarrito().single().producto)
        assertEquals(Importes(8000, 1200, 1292, 8092, 1200), clinica.cotizar())
        val pedido = clinica.confirmarPedido(cliente)
        assertEquals(fecha, pedido.fecha)
        assertEquals(1200L, pedido.importes.descuentoPromocional)

        val sinAnotacion = ClinicaVeterinaria(mapOf(alimento to 1L), promocion, reloj)
        sinAnotacion.agregarAlCarrito(anotado, 1)
        assertEquals(0L, sinAnotacion.cotizar().descuentoPromocional)
    }

    @Promocionable("Artículo de prueba")
    private class AlimentoPromocional : Producto("Alimento", 8_000, CategoriaProducto.ALIMENTOS)
}
