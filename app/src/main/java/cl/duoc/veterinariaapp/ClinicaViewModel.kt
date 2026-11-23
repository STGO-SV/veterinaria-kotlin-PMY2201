package cl.duoc.veterinariaapp

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.veterinariaapp.datos.CategoriaProducto
import cl.duoc.veterinariaapp.datos.Cliente
import cl.duoc.veterinariaapp.datos.ClinicaVeterinaria
import cl.duoc.veterinariaapp.datos.Faltante
import cl.duoc.veterinariaapp.datos.Importes
import cl.duoc.veterinariaapp.datos.ImportesLinea
import cl.duoc.veterinariaapp.datos.LineaPedido
import cl.duoc.veterinariaapp.datos.Pedido
import cl.duoc.veterinariaapp.datos.Producto
import cl.duoc.veterinariaapp.datos.ResumenClinica
import cl.duoc.veterinariaapp.datos.StockInsuficiente
import cl.duoc.veterinariaapp.datos.calcularImportesLinea
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal data class Aviso(
    @param:StringRes val recurso: Int,
    val error: Boolean = false,
    val argumento: String? = null,
    val faltantes: List<Faltante> = emptyList()
)

internal data class EstadoClinica(
    val resumen: ResumenClinica,
    val clientes: List<Cliente>,
    val productos: List<Producto>,
    val existencias: Map<Producto, Long>,
    val carrito: List<LineaPedido>,
    val pedidos: List<Pedido>,
    val importes: Importes?,
    val importesPorProducto: Map<Producto, ImportesLinea?>,
    val clienteSeleccionado: Cliente?,
    val confirmando: Boolean,
    val aviso: Aviso? = null,
    val errorPendiente: Aviso? = null
)

internal class ClinicaViewModel(
    private val clinica: ClinicaVeterinaria = ClinicaVeterinaria(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : ViewModel(scope) {
    private var seleccionado: Cliente? = null
    private var confirmando = false
    private val mutableEstado = MutableLiveData(leerEstado())
    val estado: LiveData<EstadoClinica> = mutableEstado

    fun registrarCliente(nombre: String, correo: String, telefono: String, direccion: String) =
        operar(R.string.error_correo) {
            if (clinica.registrarCliente(nombre, telefono, correo, direccion)) {
                Aviso(R.string.cliente_registrado)
            } else {
                Aviso(R.string.cliente_duplicado, error = true)
            }
        }

    fun seleccionarCliente(cliente: Cliente) = operar(R.string.error_cliente) {
        seleccionado = requireNotNull(clinica.clientesRegistrados().firstOrNull { it == cliente })
        null
    }

    fun registrarProducto(nombre: String, categoria: CategoriaProducto, precio: String, stock: String) =
        operar(R.string.error_producto) {
            val neto = precio.trim().toLongOrNull()
            val existencias = stock.trim().toLongOrNull()
            if (neto == null || existencias == null) {
                Aviso(R.string.error_numeros_producto, error = true)
            } else if (clinica.registrarProducto(Producto(nombre, neto, categoria), existencias)) {
                Aviso(R.string.producto_registrado)
            } else {
                Aviso(R.string.producto_duplicado, error = true)
            }
        }

    fun agregarProducto(producto: Producto) = operar(R.string.error_cantidad, revisarCotizacion = true) {
        clinica.agregarAlCarrito(producto, 1)
        Aviso(R.string.producto_agregado, argumento = producto.nombre)
    }

    fun modificarCantidad(producto: Producto, cantidad: String) = operar(R.string.error_cantidad, revisarCotizacion = true) {
        val unidades = requireNotNull(cantidad.trim().toIntOrNull())
        clinica.modificarCantidad(producto, unidades)
        null
    }

    fun quitarProducto(producto: Producto) = operar(R.string.error_cantidad, revisarCotizacion = true) {
        clinica.quitarDelCarrito(producto)
        null
    }

    fun vaciarCarrito() = operar(R.string.error_cantidad, revisarCotizacion = true) {
        clinica.vaciarCarrito()
        null
    }

    fun confirmarPedido() {
        if (confirmando || mutableEstado.value?.carrito.isNullOrEmpty()) return
        val cliente = seleccionado
        if (cliente == null) {
            publicar(Aviso(R.string.error_cliente, error = true), revisarCotizacion = true)
            return
        }
        confirmando = true
        publicar()
        viewModelScope.launch {
            val aviso = try {
                val pedido = clinica.confirmarPedido(cliente)
                Aviso(R.string.pedido_confirmado, argumento = pedido.idPedido)
            } catch (error: StockInsuficiente) {
                Aviso(R.string.error_stock, error = true, faltantes = error.faltantes)
            } catch (_: IllegalArgumentException) {
                Aviso(R.string.error_confirmacion, error = true)
            } catch (_: ArithmeticException) {
                Aviso(R.string.error_importes, error = true)
            } finally {
                confirmando = false
            }
            publicar(aviso, revisarCotizacion = true)
        }
    }

    fun reconocerError() {
        val actual = checkNotNull(mutableEstado.value)
        mutableEstado.value = actual.copy(errorPendiente = null)
    }

    private fun operar(@StringRes errorEntrada: Int, revisarCotizacion: Boolean = false, accion: () -> Aviso?) {
        if (confirmando) return
        viewModelScope.launch {
            val aviso = try {
                accion()
            } catch (error: StockInsuficiente) {
                Aviso(R.string.error_stock, error = true, faltantes = error.faltantes)
            } catch (_: IllegalArgumentException) {
                Aviso(errorEntrada, error = true)
            } catch (_: ArithmeticException) {
                Aviso(R.string.error_importes, error = true)
            }
            publicar(aviso, revisarCotizacion)
        }
    }

    private fun publicar(aviso: Aviso? = null, revisarCotizacion: Boolean = false) {
        val anterior = mutableEstado.value?.errorPendiente
        val esErrorModal = aviso?.error == true
        val actual = leerEstado(if (esErrorModal) null else aviso)
        val error = when {
            esErrorModal -> aviso
            revisarCotizacion && actual.carrito.isNotEmpty() && actual.importes == null ->
                Aviso(R.string.error_importes, error = true)
            else -> anterior
        }
        mutableEstado.value = actual.copy(errorPendiente = error)
    }

    private fun leerEstado(aviso: Aviso? = null): EstadoClinica {
        val carrito = clinica.lineasCarrito()
        val importes = if (carrito.isEmpty()) null else try {
            clinica.cotizar()
        } catch (_: ArithmeticException) {
            null
        }
        val importesPorProducto = carrito.associate { linea ->
            linea.producto to try {
                calcularImportesLinea(linea)
            } catch (_: ArithmeticException) {
                null
            }
        }
        return EstadoClinica(
            clinica.resumen(), clinica.clientesRegistrados(), clinica.productosRegistrados(),
            clinica.existencias(), carrito, clinica.pedidosConfirmados().reversed(), importes,
            importesPorProducto, seleccionado, confirmando, aviso
        )
    }
}
