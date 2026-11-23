package cl.duoc.veterinariaapp.datos

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

val tasaIva = BigDecimal("0.19")

data class Importes(
    val subtotal: Long,
    val descuento: Long,
    val impuesto: Long,
    val total: Long,
    val descuentoPromocional: Long = 0
)

data class ImportesLinea(
    val subtotal: Long,
    val aplicaDescuento: Boolean,
    val descuento: Long
) {
    val neto: Long get() = subtotal - descuento
}

private const val minimoDescuentoPorMayor = 4

fun calcularImportesLinea(linea: LineaPedido): ImportesLinea {
    val subtotal = linea.subtotal()
    return ImportesLinea(subtotal, linea.cantidad >= minimoDescuentoPorMayor, calcularDescuento(subtotal, linea.cantidad))
}

fun calcularDescuento(subtotal: Long, cantidad: Int): Long {
    require(subtotal >= 0 && cantidad > 0)
    return if (cantidad >= minimoDescuentoPorMayor) subtotal / 10 else 0L
}

fun calcularImpuesto(base: Long, tasa: BigDecimal): Long {
    require(base >= 0 && tasa >= BigDecimal.ZERO && tasa <= BigDecimal.ONE)
    return base.toBigDecimal().multiply(tasa).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

internal fun sumarCantidades(productos: List<LineaPedido>): Int =
    productos.fold(0) { total, linea -> Math.addExact(total, linea.cantidad) }

fun calcularTotal(
    productos: List<LineaPedido>,
    tasa: BigDecimal = tasaIva,
    fecha: LocalDate = LocalDate.now(),
    promocion: Promocion? = null
): Importes {
    require(productos.isNotEmpty())
    var subtotal = 0L
    var porCantidad = 0L
    for (linea in productos) {
        val importes = calcularImportesLinea(linea)
        subtotal = Math.addExact(subtotal, importes.subtotal)
        porCantidad = Math.addExact(porCantidad, importes.descuento)
    }
    val porFecha = promocion?.descuento(productos, fecha) ?: 0L
    val descuento = maxOf(porCantidad, porFecha)
    val base = subtotal - descuento
    val impuesto = calcularImpuesto(base, tasa)
    return Importes(
        subtotal, descuento, impuesto, Math.addExact(base, impuesto),
        if (porFecha > porCantidad) porFecha else 0L
    )
}
