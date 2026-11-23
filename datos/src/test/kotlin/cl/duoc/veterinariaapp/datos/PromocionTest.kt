package cl.duoc.veterinariaapp.datos

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromocionTest {
    private val inicio = LocalDate.of(2026, 11, 1)
    private val fin = LocalDate.of(2026, 11, 7)
    private val promocion = Promocion(inicio, fin, BigDecimal("0.15"))
    private val mixto = listOf(LineaPedido(Articulo8000(), 2), LineaPedido(Articulo9000(), 2))

    @Test fun rangoDeFechasIncluyeExtremosYExcluyeDiasExternos() {
        for ((fecha, activa) in listOf(inicio.minusDays(1) to false, inicio to true, inicio.plusDays(2) to true,
            fin to true, fin.plusDays(1) to false)) {
            assertEquals(fecha.toString(), activa, promocion.activa(fecha))
            val calculado = calcularTotal(mixto, BigDecimal("0.19"), fecha, promocion)
            assertEquals(if (activa) Importes(34000, 5100, 5491, 34391, 5100) else Importes(34000, 0, 6460, 40460), calculado)
        }
        assertTrue(Promocion(inicio, inicio, BigDecimal.ONE).activa(inicio))
    }

    @Test fun intervalosYPorcentajesInvalidosSeRechazan() {
        assertThrows(IllegalArgumentException::class.java) { Promocion(fin, inicio, BigDecimal("0.10")) }
        for (porcentaje in listOf("-0.01", "1.01")) {
            assertThrows(IllegalArgumentException::class.java) { Promocion(inicio, fin, porcentaje.toBigDecimal()) }
        }
    }

    @Test fun soloLosProductosAnotadosRecibenPromocion() {
        val noMarcado = Producto("Referencia", 10000, CategoriaProducto.ALIMENTOS)
        val productos = listOf(LineaPedido(Articulo8000(), 1), LineaPedido(noMarcado, 1))
        assertTrue(esPromocionable(Articulo8000()))
        assertTrue(esPromocionable(Articulo9000()))
        assertFalse(esPromocionable(noMarcado))
        assertEquals(1200L, promocion.descuento(productos, inicio))
        assertEquals(0L, promocion.descuento(listOf(LineaPedido(noMarcado, 1)), inicio))
        assertEquals(Importes(18000, 1200, 3192, 19992, 1200), calcularTotal(productos, BigDecimal("0.19"), inicio, promocion))
    }

    @Test fun descuentoMenorOEmpatadoNoSeAcumulaConCantidad() {
        val califican = listOf(LineaPedido(Articulo8000(), 4), LineaPedido(Articulo9000(), 4))
        for (porcentaje in listOf("0", "0.05", "0.10")) {
            assertEquals(Importes(68000, 6800, 11628, 72828),
                calcularTotal(califican, BigDecimal("0.19"), inicio, Promocion(inicio, fin, porcentaje.toBigDecimal())))
        }
        assertEquals(Importes(68000, 10200, 10982, 68782, 10200),
            calcularTotal(califican, BigDecimal("0.19"), inicio, promocion))
    }

    @Test fun descuentoDelCienPorCientoNoGeneraImpuestosNiTotalesNegativos() {
        assertEquals(Importes(34000, 34000, 0, 0, 34000),
            calcularTotal(mixto, BigDecimal.ONE, inicio, Promocion(inicio, fin, BigDecimal.ONE)))
    }

    @Test fun redondeoPromocionalTruncaAntesDelImpuestoHalfUp() {
        @Promocionable("Prueba de redondeo")
        class Economica : Producto("Económica", 11, CategoriaProducto.ALIMENTOS)
        assertEquals(Importes(11, 1, 1, 11, 1), calcularTotal(listOf(LineaPedido(Economica(), 1)),
            BigDecimal("0.05"), inicio, promocion))
    }

    @Test fun consultaLaAnotacionDeLaClaseConcretaYDescuentaSoloLineasElegibles() {
        val generico = Producto("Artículo A", 8_000, CategoriaProducto.ALIMENTOS)
        assertTrue(esPromocionable(Articulo8000()))
        assertTrue(esPromocionable(Articulo9000()))
        assertTrue(esPromocionable(BaseAnotada()))
        assertFalse(esPromocionable(SinAnotacion()))
        assertFalse(esPromocionable(generico))

        val lineas = listOf(LineaPedido(generico, 1), LineaPedido(Articulo8000(), 1))
        assertEquals(1_200L, promocion.descuento(lineas, inicio))
        assertEquals(1_200L, promocion.descuento(lineas, fin))
        assertEquals(0L, promocion.descuento(lineas, fin.plusDays(1)))
    }

    @Promocionable("Artículo de prueba")
    private class Articulo8000 : Producto("Artículo A", 8_000, CategoriaProducto.ALIMENTOS)

    @Promocionable("Artículo de prueba")
    private class Articulo9000 : Producto("Artículo B", 9_000, CategoriaProducto.JUGUETES)

    @Promocionable("Clase de prueba")
    private open class BaseAnotada : Producto("Prueba", 1, CategoriaProducto.OTROS)

    private class SinAnotacion : BaseAnotada()
}
