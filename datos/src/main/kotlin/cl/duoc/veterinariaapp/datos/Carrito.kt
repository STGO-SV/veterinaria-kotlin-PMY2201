package cl.duoc.veterinariaapp.datos

import cl.duoc.veterinariaapp.utilidades.rangoCantidadArticulos

/** Opera con las fichas canónicas que entrega el catálogo de la clínica. */
internal class Carrito(private val inventario: Inventario) {
    private val lineas = linkedMapOf<Producto, LineaPedido>()

    fun agregar(producto: Producto, cantidad: Int) {
        require(cantidad in rangoCantidadArticulos) { "La cantidad por línea debe estar entre 1 y 100." }
        val anterior = lineas[producto]?.cantidad ?: 0
        guardar(LineaPedido(producto, Math.addExact(anterior, cantidad)))
    }

    fun modificarCantidad(producto: Producto, cantidad: Int) {
        val anterior = requireNotNull(lineas[producto]) { "El producto no está en el carrito." }
        guardar(LineaPedido(anterior.producto, cantidad))
    }

    private fun guardar(linea: LineaPedido) {
        inventario.validarDisponibilidad(linea)
        lineas[linea.producto] = linea
    }

    fun quitar(producto: Producto): Boolean = lineas.remove(producto) != null

    fun vaciar() = lineas.clear()

    fun listar(): List<LineaPedido> = lineas.values.toList()
}
