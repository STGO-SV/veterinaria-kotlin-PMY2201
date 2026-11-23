package cl.duoc.veterinariaapp.datos

import cl.duoc.veterinariaapp.utilidades.rangoCantidadArticulos

data class LineaPedido(val producto: Producto, val cantidad: Int) {
    init {
        require(cantidad in rangoCantidadArticulos) { "La cantidad por línea debe estar entre 1 y 100." }
    }
}

internal fun LineaPedido.subtotal(): Long = Math.multiplyExact(producto.precio, cantidad.toLong())

fun calcularSubtotal(productos: List<LineaPedido>): Long = productos.fold(0L) { total, linea ->
    Math.addExact(total, linea.subtotal())
}
