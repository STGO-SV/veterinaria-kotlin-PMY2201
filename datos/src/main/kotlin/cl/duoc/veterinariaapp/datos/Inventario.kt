package cl.duoc.veterinariaapp.datos

data class Faltante(val producto: Producto, val disponible: Long, val requerido: Long)

class StockInsuficiente internal constructor(faltantes: List<Faltante>) :
    IllegalStateException("Existencias insuficientes para la cantidad solicitada.") {
    private val detalle = faltantes.toList()
    val faltantes: List<Faltante> get() = detalle.toList()
}

internal class Inventario(inicial: Map<Producto, Long>) {
    private val existencias = inicial.toMutableMap()

    init {
        require(existencias.values.all { it >= 0 }) { "Las existencias iniciales no pueden ser negativas." }
    }

    fun consultar(): Map<Producto, Long> = existencias.toMap()

    fun reabastecer(producto: Producto, cantidad: Long) {
        require(cantidad > 0) { "El reabastecimiento requiere una cantidad positiva." }
        existencias[producto] = Math.addExact(existencias[producto] ?: 0L, cantidad)
    }

    fun validarDisponibilidad(linea: LineaPedido) {
        val faltante = faltante(linea.producto, linea.cantidad.toLong())
        if (faltante != null) throw StockInsuficiente(listOf(faltante))
    }

    fun consumir(lineas: List<LineaPedido>) {
        val necesarios = linkedMapOf<Producto, Long>()
        for (linea in lineas) {
            val producto = linea.producto
            necesarios[producto] = Math.addExact(necesarios[producto] ?: 0L, linea.cantidad.toLong())
        }
        val faltantes = necesarios.mapNotNull { (producto, requerido) -> faltante(producto, requerido) }
        if (faltantes.isNotEmpty()) throw StockInsuficiente(faltantes)
        for ((producto, requerido) in necesarios) {
            existencias[producto] = existencias.getValue(producto) - requerido
        }
    }

    private fun faltante(producto: Producto, requerido: Long): Faltante? {
        val disponible = existencias[producto] ?: 0L
        return if (disponible < requerido) Faltante(producto, disponible, requerido) else null
    }
}
