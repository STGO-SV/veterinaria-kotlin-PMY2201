package cl.duoc.veterinariaapp.datos

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculosHistoricosTest {
    private val articulo = Producto("Artículo A", 8_000, CategoriaProducto.ALIMENTOS)
    private fun lineas(cantidad: Int) = listOf(LineaPedido(articulo, cantidad))

    @Test fun unArticuloSinDescuento() {
        assertEquals(Importes(8000, 0, 1520, 9520), calcularTotal(lineas(1)))
    }

    @Test fun tresArticulosSinDescuento() {
        assertEquals(Importes(24000, 0, 4560, 28560), calcularTotal(lineas(3)))
    }

    @Test fun cuatroArticulosConDiezPorCiento() {
        assertEquals(Importes(32000, 3200, 5472, 34272), calcularTotal(lineas(4)))
    }

    @Test fun cincoArticulosConDiezPorCiento() {
        assertEquals(42840L, calcularTotal(lineas(5)).total)
    }

    @Test fun ochoUnidadesDescuentanSobreElSubtotalDeSuLinea() {
        assertEquals(Importes(64000, 6400, 10944, 68544), calcularTotal(lineas(8)))
    }

    @Test fun tasaCeroConservaTotalesSemanaUno() {
        for ((cantidad, total) in listOf(1 to 8000L, 3 to 24000L, 4 to 28800L, 5 to 36000L, 8 to 57600L)) {
            assertEquals(total, calcularTotal(lineas(cantidad), BigDecimal.ZERO).total)
        }
    }

    @Test fun redondeoTruncaDescuentoYRedondeaImpuestoAlPeso() {
        assertEquals(3L, calcularDescuento(32, 4))
        assertEquals(1L, calcularImpuesto(5, BigDecimal("0.10")))
        assertEquals(0L, calcularImpuesto(4, BigDecimal("0.10")))
        val producto = Producto("P", 8, CategoriaProducto.OTROS)
        assertEquals(29L, calcularTotal(listOf(LineaPedido(producto, 4)), BigDecimal.ZERO).total)
    }

    @Test fun tasasInvalidasSeRechazan() {
        for (tasa in listOf(BigDecimal("-0.01"), BigDecimal("1.01"))) {
            assertThrows(IllegalArgumentException::class.java) { calcularTotal(lineas(1), tasa) }
            assertThrows(IllegalArgumentException::class.java) { calcularImpuesto(10, tasa) }
        }
    }

    @Test fun importesMixtosNoAcumulanCantidadesParaDescuento() {
        val otro = Producto("Artículo B", 9_000, CategoriaProducto.JUGUETES)
        val productos = listOf(LineaPedido(articulo, 2), LineaPedido(otro, 2))
        assertEquals(4, sumarCantidades(productos))
        assertEquals(Importes(34000, 0, 6460, 40460), calcularTotal(productos))
    }

    @Test fun descuentoPorLineaEImpuestoEnCarritosMixtos() {
        val a = Producto("A", 1_375, CategoriaProducto.ALIMENTOS)
        val b = Producto("B", 2_265, CategoriaProducto.JUGUETES)
        val c = Producto("C", 995, CategoriaProducto.OTROS)

        assertEquals(Importes(6390, 0, 1214, 7604),
            calcularTotal(listOf(LineaPedido(a, 3), LineaPedido(b, 1))))
        assertEquals(Importes(11025, 550, 1990, 12465),
            calcularTotal(listOf(LineaPedido(a, 4), LineaPedido(b, 2), LineaPedido(c, 1))))
        assertEquals(Importes(14560, 1456, 2490, 15594),
            calcularTotal(listOf(LineaPedido(a, 4), LineaPedido(b, 4))))
    }

    @Test fun desglosePorLineaCoincideConLaCotizacion() {
        val otro = Producto("B", 2_265, CategoriaProducto.JUGUETES)
        val productos = listOf(LineaPedido(articulo, 4), LineaPedido(otro, 2))
        val (primero, segundo) = productos.map(::calcularImportesLinea)

        assertEquals(ImportesLinea(24_000, false, 0), calcularImportesLinea(LineaPedido(articulo, 3)))
        assertEquals(ImportesLinea(32_000, true, 3_200), primero)
        assertEquals(ImportesLinea(4_530, false, 0), segundo)
        assertEquals(28_800L, primero.neto)
        assertEquals(4_530L, segundo.neto)

        val total = calcularTotal(productos)
        assertEquals(total.subtotal, primero.subtotal + segundo.subtotal)
        assertEquals(total.descuento, primero.descuento + segundo.descuento)
        assertEquals(total.total - total.impuesto, primero.neto + segundo.neto)
    }

    @Test fun descuentoTruncaCadaLineaAntesDeSumar() {
        val primero = Producto("A", 8, CategoriaProducto.ALIMENTOS)
        val segundo = Producto("B", 12, CategoriaProducto.JUGUETES)
        assertEquals(Importes(80, 7, 14, 87),
            calcularTotal(listOf(LineaPedido(primero, 4), LineaPedido(segundo, 4))))
    }
}
