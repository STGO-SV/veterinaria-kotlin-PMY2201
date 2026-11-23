package cl.duoc.veterinariaapp

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import cl.duoc.veterinariaapp.datos.CategoriaProducto
import cl.duoc.veterinariaapp.datos.ClinicaVeterinaria
import cl.duoc.veterinariaapp.datos.Faltante
import cl.duoc.veterinariaapp.datos.Importes
import cl.duoc.veterinariaapp.datos.LineaPedido
import cl.duoc.veterinariaapp.datos.ResumenClinica
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class ClinicaViewModelTest {
    private val cola = ColaPrincipal()
    private val scope = CoroutineScope(SupervisorJob() + cola)
    private val clinica = ClinicaVeterinaria()
    private lateinit var modelo: ClinicaViewModel
    private val estado: EstadoClinica get() = checkNotNull(modelo.estado.value)

    @Before fun preparar() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        modelo = ClinicaViewModel(clinica, scope)
    }

    @After fun cerrar() {
        scope.cancel()
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test fun resumenInicialVacio() {
        assertEquals(ResumenClinica(0, 0, null), estado.resumen)
        assertNull(estado.clienteSeleccionado)
        assertTrue(estado.pedidos.isEmpty())
        assertTrue(estado.carrito.isEmpty())
        assertNull(estado.importes)
    }

    @Test fun registraYSeleccionaLaFichaDelDominio() {
        registrarAna()
        val cliente = clinica.clientesRegistrados().single()
        modelo.seleccionarCliente(cliente)
        cola.ejecutar()
        assertEquals(1, estado.resumen.totalClientes)
        assertSame(cliente, estado.resumen.ultimoCliente)
        assertSame(cliente, estado.clienteSeleccionado)
        assertEquals("No informado", cliente.telefono)
        assertEquals("Retiro en clínica", cliente.direccion)
    }

    @Test fun duplicadoNoIncrementaNiCambiaAlUltimoCliente() {
        registrarAna()
        modelo.registrarCliente("Bruno", "bruno@example.cl", "", "")
        cola.ejecutar()
        val ultimo = estado.resumen.ultimoCliente
        modelo.registrarCliente(" ANA ", "ANA@example.cl", "987654321", "Otra calle")
        cola.ejecutar()
        assertEquals(R.string.cliente_duplicado, estado.errorPendiente?.recurso)
        assertEquals(2, estado.resumen.totalClientes)
        assertSame(ultimo, estado.resumen.ultimoCliente)
    }

    @Test fun catalogoEInventarioProcedenDelDominio() {
        assertEquals(clinica.productosRegistrados(), estado.productos)
        assertEquals(clinica.existencias(), estado.existencias)
        modelo.registrarProducto("Correa", CategoriaProducto.OTROS, "2500", "3")
        cola.ejecutar()
        val producto = clinica.productosRegistrados().last()
        assertEquals(R.string.producto_registrado, estado.aviso?.recurso)
        assertNull(estado.errorPendiente)
        assertSame(producto, estado.productos.last())
        assertEquals(3L, estado.existencias[producto])
        modelo.registrarProducto(" CORREA ", CategoriaProducto.OTROS, "1", "50")
        cola.ejecutar()
        assertEquals(R.string.producto_duplicado, estado.errorPendiente?.recurso)
        assertEquals(2_500L, estado.productos.last().precio)
        assertEquals(3L, estado.existencias[producto])
    }

    @Test fun rechazaEntradasInvalidasYConservaLosRegistros() {
        modelo.registrarCliente("Ana", "sin-arroba", "", "")
        cola.ejecutar()
        assertEquals(R.string.error_correo, estado.errorPendiente?.recurso)
        assertEquals(0, estado.resumen.totalClientes)
        assertNull(estado.aviso)
        val clienteRechazado = estado
        modelo.reconocerError()
        assertEquals(clienteRechazado.copy(errorPendiente = null), estado)

        val productos = estado.productos
        val existencias = estado.existencias
        modelo.registrarProducto("Correa", CategoriaProducto.OTROS, "1.25", "3")
        cola.ejecutar()
        assertEquals(R.string.error_numeros_producto, estado.errorPendiente?.recurso)
        modelo.registrarProducto("Correa", CategoriaProducto.OTROS, "0", "-1")
        cola.ejecutar()
        assertEquals(R.string.error_producto, estado.errorPendiente?.recurso)
        assertEquals(productos, estado.productos)
        assertEquals(existencias, estado.existencias)
        assertEquals(0, estado.resumen.totalPedidos)
        assertNull(estado.aviso)
        val productoRechazado = estado
        modelo.reconocerError()
        assertEquals(productoRechazado.copy(errorPendiente = null), estado)
    }

    @Test fun modificarCantidadesActualizaCotizacionSinConsumirStock() {
        val producto = estado.productos.first()
        val stock = estado.existencias
        modelo.agregarProducto(producto)
        cola.ejecutar()
        modelo.modificarCantidad(producto, "4")
        cola.ejecutar()
        assertEquals(4, estado.carrito.single().cantidad)
        assertEquals(clinica.cotizar(), estado.importes)
        assertTrue(checkNotNull(estado.importes).descuento > 0)
        modelo.modificarCantidad(producto, "101")
        cola.ejecutar()
        assertEquals(R.string.error_cantidad, estado.errorPendiente?.recurso)
        assertEquals(4, estado.carrito.single().cantidad)
        assertEquals(stock, estado.existencias)
        assertEquals(0, estado.resumen.totalPedidos)
    }

    @Test fun confirmaYActualizaResumenCarritoYExistencias() {
        prepararCompra()
        val producto = estado.carrito.single().producto
        val antes = estado.existencias.getValue(producto)
        modelo.confirmarPedido()
        assertTrue(estado.confirmando)
        cola.ejecutar()
        assertFalse(estado.confirmando)
        assertEquals(1, estado.resumen.totalPedidos)
        assertEquals(1, estado.resumen.totalClientes)
        assertEquals(R.string.pedido_confirmado, estado.aviso?.recurso)
        assertEquals(antes - 1, estado.existencias[producto])
        assertTrue(estado.carrito.isEmpty())
    }

    @Test fun cantidadEditadaSobreStockConservaCarritoCotizacionExistenciasYResumen() {
        prepararCompra()
        val producto = estado.carrito.single().producto
        val stock = estado.existencias
        val carrito = estado.carrito
        val importes = estado.importes
        val desglose = estado.importesPorProducto
        val disponible = stock.getValue(producto)
        modelo.modificarCantidad(producto, (disponible + 1).toString())
        cola.ejecutar()
        assertEquals(R.string.error_stock, estado.errorPendiente?.recurso)
        assertEquals(listOf(Faltante(producto, disponible, disponible + 1)), estado.errorPendiente?.faltantes)
        assertSame(producto, estado.errorPendiente?.faltantes?.single()?.producto)
        assertEquals(carrito, estado.carrito)
        assertEquals(importes, estado.importes)
        assertEquals(desglose, estado.importesPorProducto)
        assertEquals(stock, estado.existencias)
        assertEquals(0, estado.resumen.totalPedidos)

        val conError = estado
        modelo.reconocerError()
        cola.ejecutar()
        assertEquals(conError.copy(errorPendiente = null), estado)
    }

    @Test fun desbordamientoSeInformaYPermiteRecuperarLaCotizacion() {
        modelo.registrarProducto("Precio extremo", CategoriaProducto.OTROS, Long.MAX_VALUE.toString(), "2")
        cola.ejecutar()
        val producto = estado.productos.last()
        modelo.agregarProducto(producto)
        cola.ejecutar()
        assertEquals(R.string.error_importes, estado.errorPendiente?.recurso)
        assertNull(estado.importes)
        assertEquals(Long.MAX_VALUE, estado.importesPorProducto[producto]?.subtotal)

        modelo.reconocerError()
        modelo.modificarCantidad(producto, "2")
        cola.ejecutar()
        assertEquals(R.string.error_importes, estado.errorPendiente?.recurso)
        assertNull(estado.importesPorProducto[producto])
        assertEquals(2L, estado.existencias[producto])
        assertEquals(0, estado.resumen.totalPedidos)

        modelo.reconocerError()
        modelo.quitarProducto(producto)
        cola.ejecutar()
        assertTrue(estado.carrito.isEmpty())
        assertNull(estado.errorPendiente)
    }

    @Test fun confirmacionesRepetidasMientrasSeProcesaYTrasElExitoRegistranUnaVez() {
        prepararCompra()
        modelo.confirmarPedido()
        modelo.confirmarPedido()
        cola.ejecutar()
        modelo.confirmarPedido()
        cola.ejecutar()
        assertEquals(1, clinica.pedidosConfirmados().size)
        assertEquals(1, estado.resumen.totalPedidos)
        assertEquals(R.string.pedido_confirmado, estado.aviso?.recurso)
    }

    @Test fun historialExponeLosPedidosRegistradosDelMasRecienteAlMasAntiguo() {
        prepararCompra()
        val cliente = checkNotNull(estado.clienteSeleccionado)
        val producto = estado.carrito.single().producto
        modelo.modificarCantidad(producto, "4")
        cola.ejecutar()
        modelo.confirmarPedido()
        cola.ejecutar()

        val primero = estado.pedidos.single()
        assertSame(clinica.pedidosConfirmados().single(), primero)
        assertEquals("PED-001", primero.idPedido)
        assertEquals(cliente.nombre, primero.cliente.nombre)
        assertEquals(cliente.correo, primero.cliente.correo)
        assertEquals(listOf(LineaPedido(producto, 4)), primero.productos)
        assertEquals(5_900L, primero.productos.single().producto.precio)
        assertEquals(Importes(23_600, 2_360, 4_036, 25_276), primero.importes)

        modelo.agregarProducto(producto)
        cola.ejecutar()
        modelo.modificarCantidad(producto, "2")
        cola.ejecutar()
        assertSame(primero, estado.pedidos.single())
        modelo.confirmarPedido()
        cola.ejecutar()

        assertEquals(listOf("PED-002", "PED-001"), estado.pedidos.map { it.idPedido })
        assertSame(primero, estado.pedidos.last())
        assertEquals(4, primero.productos.single().cantidad)
        assertEquals(Importes(23_600, 2_360, 4_036, 25_276), primero.importes)
        assertEquals(2, estado.resumen.totalPedidos)
    }

    private fun registrarAna() {
        modelo.registrarCliente("Ana", "ana@example.cl", "inválido", "")
        cola.ejecutar()
    }

    private fun prepararCompra() {
        registrarAna()
        modelo.seleccionarCliente(estado.clientes.single())
        modelo.agregarProducto(estado.productos.first())
        cola.ejecutar()
    }

    private class ColaPrincipal : CoroutineDispatcher() {
        private val pendientes = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            pendientes.addLast(block)
        }
        fun ejecutar() {
            while (pendientes.isNotEmpty()) pendientes.removeFirst().run()
        }
    }
}
