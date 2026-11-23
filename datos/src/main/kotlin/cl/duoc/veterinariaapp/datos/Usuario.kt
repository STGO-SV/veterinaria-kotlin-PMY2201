package cl.duoc.veterinariaapp.datos

import cl.duoc.veterinariaapp.utilidades.claveIdentidad
import cl.duoc.veterinariaapp.utilidades.correoValido
import cl.duoc.veterinariaapp.utilidades.normalizarTelefono
import cl.duoc.veterinariaapp.utilidades.textoOValorPredeterminado

open class Usuario(nombre: String?, telefono: String?, correo: String?) {
    val nombre = textoOValorPredeterminado(nombre, "Cliente sin nombre")
    val telefono = normalizarTelefono(telefono) ?: "No informado"
    val correo = correoValido(correo) ?: "correo@invalido.com"
    val correoNotificable: String? = correoValido(correo)?.takeUnless { it == "correo@invalido.com" }
    protected fun contacto(): String = "Teléfono: $telefono\nCorreo: $correo"
    open fun describirUsuario(): String = "Usuario: $nombre\n${contacto()}"
}

class Cliente(nombre: String?, telefono: String?, correo: String?, direccion: String?) :
    Usuario(nombre, telefono, correo) {
    val direccion = textoOValorPredeterminado(direccion, "Retiro en clínica")
    override fun describirUsuario(): String = "Cliente: $nombre\nDirección: $direccion\n${contacto()}"

    override fun equals(other: Any?): Boolean = other is Cliente &&
        claveIdentidad(nombre) == claveIdentidad(other.nombre) && claveIdentidad(correo) == claveIdentidad(other.correo)

    override fun hashCode(): Int = 31 * claveIdentidad(nombre).hashCode() + claveIdentidad(correo).hashCode()

    operator fun component1(): String = nombre
    operator fun component2(): String = correo
    operator fun component3(): String = telefono
}
