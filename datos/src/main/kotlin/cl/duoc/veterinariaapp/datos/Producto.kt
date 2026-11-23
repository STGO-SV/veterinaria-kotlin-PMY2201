package cl.duoc.veterinariaapp.datos

import cl.duoc.veterinariaapp.utilidades.claveIdentidad

enum class CategoriaProducto(val etiqueta: String) {
    MEDICAMENTOS("Medicamentos"),
    ALIMENTOS("Alimentos"),
    ASEO_E_HIGIENE("Aseo e higiene"),
    JUGUETES("Juguetes"),
    OTROS("Otros")
}

/** El precio unitario se expresa en pesos chilenos netos de IVA. */
open class Producto(val nombre: String, val precio: Long, val categoria: CategoriaProducto) {
    init {
        require(nombre.isNotBlank() && precio > 0) { "El producto requiere un nombre y un precio positivo." }
    }

    final override fun equals(other: Any?): Boolean = other is Producto &&
        claveIdentidad(nombre) == claveIdentidad(other.nombre) && categoria == other.categoria

    final override fun hashCode(): Int = 31 * claveIdentidad(nombre).hashCode() + categoria.hashCode()
}

/** Catálogo de demostración con existencias y precios ficticios. */
fun catalogoInicial(): Map<Producto, Long> = linkedMapOf(
    Producto("Antiparasitario (gatos y perros)", 5_900, CategoriaProducto.MEDICAMENTOS) to 10L,
    Producto("Alimento gato senior", 8_000, CategoriaProducto.ALIMENTOS) to 12L,
    Producto("Champú hipoalergénico para perros", 4_500, CategoriaProducto.ASEO_E_HIGIENE) to 8L,
    Producto("Pelota con cascabel para perros", 2_000, CategoriaProducto.JUGUETES) to 15L,
    Producto("Comedero elevado para gatos", 3_500, CategoriaProducto.OTROS) to 6L
)
