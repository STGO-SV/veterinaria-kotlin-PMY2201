package cl.duoc.veterinariaapp.datos

import java.time.LocalDate

class Pedido internal constructor(
    val idPedido: String,
    val cliente: Cliente,
    productos: List<LineaPedido>,
    val fecha: LocalDate,
    val importes: Importes
) {
    private val lineas = productos.toList()
    val productos: List<LineaPedido> get() = lineas.toList()
    val cantidadArticulos: Int get() = sumarCantidades(lineas)

    fun calcularImportes(): Importes = importes

    operator fun component1(): Cliente = cliente
    operator fun component2(): List<LineaPedido> = productos
    operator fun component3(): Long = importes.total
}
