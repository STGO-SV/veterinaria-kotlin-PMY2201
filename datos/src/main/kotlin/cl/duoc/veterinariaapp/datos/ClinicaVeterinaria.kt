package cl.duoc.veterinariaapp.datos

import cl.duoc.veterinariaapp.utilidades.correoValido
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

data class ResumenClinica(val totalClientes: Int, val totalPedidos: Int, val ultimoCliente: Cliente?)

/** Mantiene un carrito y los registros de una sesión. Cada operación usa el mismo monitor. */
class ClinicaVeterinaria(
    productosIniciales: Map<Producto, Long> = catalogoInicial(),
    private val promocion: Promocion? = null,
    private val reloj: Clock = Clock.system(ZoneId.of("America/Santiago"))
) {
    private val clientes = Registro<Cliente>()
    private val catalogo = Registro<Producto>().apply { productosIniciales.keys.forEach(::registrar) }
    private val inventario = Inventario(productosIniciales)
    private val carrito = Carrito(inventario)
    private val pedidos = mutableListOf<Pedido>()

    @Synchronized
    fun registrarCliente(nombre: String?, telefono: String?, correo: String?, direccion: String?): Boolean {
        val correoValidado = requireNotNull(correoValido(correo)) { "El registro requiere un correo válido." }
        return clientes.registrar(Cliente(nombre, telefono, correoValidado, direccion))
    }

    @Synchronized
    fun registrarProducto(producto: Producto, existencias: Long = 0): Boolean {
        require(existencias >= 0) { "Las existencias iniciales no pueden ser negativas." }
        if (!catalogo.registrar(producto)) return false
        if (existencias > 0) inventario.reabastecer(producto, existencias)
        return true
    }

    @Synchronized
    fun reabastecer(producto: Producto, cantidad: Long) =
        inventario.reabastecer(productoRegistrado(producto), cantidad)

    @Synchronized
    fun clientesRegistrados(): List<Cliente> = clientes.listar()

    @Synchronized
    fun productosRegistrados(): List<Producto> = catalogo.listar()

    @Synchronized
    fun existencias(): Map<Producto, Long> = inventario.consultar()

    @Synchronized
    fun lineasCarrito(): List<LineaPedido> = carrito.listar()

    @Synchronized
    fun pedidosConfirmados(): List<Pedido> = pedidos.toList()

    @Synchronized
    fun resumen(): ResumenClinica = ResumenClinica(clientes.total(), pedidos.size, clientes.ultimo())

    @Synchronized
    fun agregarAlCarrito(producto: Producto, cantidad: Int) =
        carrito.agregar(productoRegistrado(producto), cantidad)

    @Synchronized
    fun modificarCantidad(producto: Producto, cantidad: Int) = carrito.modificarCantidad(producto, cantidad)

    @Synchronized
    fun quitarDelCarrito(producto: Producto): Boolean = carrito.quitar(producto)

    @Synchronized
    fun vaciarCarrito() = carrito.vaciar()

    @Synchronized
    fun cotizar(): Importes = calcularTotal(carritoValido(), fecha = LocalDate.now(reloj), promocion = promocion)

    @Synchronized
    fun confirmarPedido(cliente: Cliente): Pedido {
        val registrado = requireNotNull(clientes.buscar(cliente)) { "El pedido requiere un cliente registrado." }
        val lineas = carritoValido()
        val fecha = LocalDate.now(reloj)
        val importes = calcularTotal(lineas, fecha = fecha, promocion = promocion)
        val numero = Math.incrementExact(pedidos.size)
        val pedido = Pedido("PED-${numero.toString().padStart(3, '0')}", registrado, lineas, fecha, importes)
        inventario.consumir(lineas)
        pedidos.add(pedido)
        carrito.vaciar()
        return pedido
    }

    private fun productoRegistrado(producto: Producto): Producto =
        requireNotNull(catalogo.buscar(producto)) { "El producto no está registrado." }

    private fun carritoValido(): List<LineaPedido> {
        val lineas = carrito.listar()
        require(lineas.isNotEmpty()) { "El carrito está vacío." }
        check(lineas.all { catalogo.buscar(it.producto) === it.producto }) { "El carrito contiene fichas no vigentes." }
        return lineas
    }
}
