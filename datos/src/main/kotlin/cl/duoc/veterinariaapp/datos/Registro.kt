package cl.duoc.veterinariaapp.datos

/** Conserva la primera ficha de una identidad; nunca reemplaza sus datos. */
class Registro<T : Any> {
    private val entidades = linkedSetOf<T>()
    fun registrar(entidad: T): Boolean = entidades.add(entidad)
    fun buscar(entidad: T): T? = entidades.find { it == entidad }
    fun listar(): List<T> = entidades.toList()
    fun total(): Int = entidades.size
    fun ultimo(): T? = entidades.lastOrNull()
}
