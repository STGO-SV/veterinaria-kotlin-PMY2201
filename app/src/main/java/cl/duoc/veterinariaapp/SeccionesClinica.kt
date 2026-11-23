package cl.duoc.veterinariaapp

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.veterinariaapp.datos.CategoriaProducto
import cl.duoc.veterinariaapp.datos.ImportesLinea
import cl.duoc.veterinariaapp.datos.LineaPedido
import cl.duoc.veterinariaapp.utilidades.pesos
import cl.duoc.veterinariaapp.utilidades.rangoCantidadArticulos

@Composable
internal fun SeccionClientes(estado: EstadoClinica, modelo: ClinicaViewModel) {
    Titulo(R.string.registrar_cliente)
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var direccion by rememberSaveable { mutableStateOf("") }
    Campo(R.string.nombre_cliente, nombre, { nombre = it })
    Campo(R.string.correo_cliente, correo, { correo = it }, KeyboardType.Email)
    Campo(R.string.telefono_cliente, telefono, { telefono = it }, KeyboardType.Phone)
    Campo(R.string.direccion_cliente, direccion, { direccion = it })
    val foco = LocalFocusManager.current
    Button(onClick = {
        foco.clearFocus()
        modelo.registrarCliente(nombre, correo, telefono, direccion)
    }) {
        Text(stringResource(R.string.guardar_cliente))
    }
    estado.aviso?.let { Mensaje(it) }

    Titulo(R.string.elegir_cliente)
    val seleccionado = estado.clienteSeleccionado
    if (seleccionado != null) {
        Text(stringResource(R.string.cliente_seleccionado, seleccionado.nombre, seleccionado.correo))
    } else {
        Text(stringResource(if (estado.clientes.isEmpty()) R.string.sin_clientes else R.string.sin_seleccion))
    }
    Column(Modifier.selectableGroup()) {
        for (cliente in estado.clientes) {
            val activo = cliente == seleccionado
            Row(
                Modifier.fillMaxWidth().selectable(activo, role = Role.RadioButton) { modelo.seleccionarCliente(cliente) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RadioButton(selected = activo, onClick = null)
                Column {
                    Text(cliente.nombre, style = MaterialTheme.typography.titleSmall)
                    Text(cliente.correo, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
internal fun SeccionProductos(estado: EstadoClinica, modelo: ClinicaViewModel) {
    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { mostrarFormulario = !mostrarFormulario }) {
        Text(stringResource(if (mostrarFormulario) R.string.ocultar_formulario else R.string.registrar_producto))
    }
    val formulario = rememberSaveableStateHolder()
    if (mostrarFormulario) {
        formulario.SaveableStateProvider("producto") { FormularioProducto(modelo) }
    }
    estado.aviso?.let { Mensaje(it) }
    Titulo(R.string.catalogo)
    for (producto in estado.productos) {
        val disponibles = estado.existencias[producto] ?: 0L
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(producto.nombre, style = MaterialTheme.typography.titleMedium)
                Text(producto.categoria.etiqueta)
                Text(stringResource(R.string.precio_neto, pesos(producto.precio)))
                Text(stringResource(R.string.existencias, disponibles))
                val etiqueta = stringResource(R.string.agregar_articulo, producto.nombre)
                TextButton(
                    onClick = { modelo.agregarProducto(producto) },
                    enabled = !estado.confirmando && disponibles > 0L,
                    modifier = Modifier.semantics { contentDescription = etiqueta }
                ) {
                    Text(stringResource(R.string.agregar))
                }
            }
        }
    }

}

@Composable
private fun FormularioProducto(modelo: ClinicaViewModel) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf(CategoriaProducto.entries.first()) }
    var precio by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var categoriasAbiertas by remember { mutableStateOf(false) }
    Campo(R.string.nombre_producto, nombre, { nombre = it })
    Column {
        OutlinedButton(onClick = { categoriasAbiertas = true }) {
            Text(stringResource(R.string.categoria_actual, categoria.etiqueta))
        }
        DropdownMenu(expanded = categoriasAbiertas, onDismissRequest = { categoriasAbiertas = false }) {
            for (opcion in CategoriaProducto.entries) {
                DropdownMenuItem(text = { Text(opcion.etiqueta) }, onClick = {
                    categoria = opcion
                    categoriasAbiertas = false
                })
            }
        }
    }
    Campo(R.string.precio_producto, precio, { precio = it }, KeyboardType.Number)
    Campo(R.string.stock_producto, stock, { stock = it }, KeyboardType.Number)
    val foco = LocalFocusManager.current
    Button(onClick = {
        foco.clearFocus()
        modelo.registrarProducto(nombre, categoria, precio, stock)
    }) {
        Text(stringResource(R.string.guardar_producto))
    }
}

@Composable
internal fun SeccionCarrito(estado: EstadoClinica, modelo: ClinicaViewModel) {
    Titulo(R.string.revisar_pedido)
    Text(stringResource(R.string.compra_simulada))
    val cliente = estado.clienteSeleccionado
    Text(
        if (cliente == null) stringResource(R.string.elegir_cliente_en_clientes)
        else stringResource(R.string.cliente_seleccionado, cliente.nombre, cliente.correo)
    )
    if (estado.carrito.isEmpty()) {
        Text(stringResource(R.string.carrito_vacio))
        estado.aviso?.let { Mensaje(it) }
        return
    }

    for (linea in estado.carrito) {
        key(linea.producto.nombre, linea.producto.categoria) {
            val disponibles = estado.existencias[linea.producto] ?: 0L
            LineaCarrito(linea, disponibles, estado.importesPorProducto[linea.producto], estado.confirmando, modelo)
        }
    }
    val importes = estado.importes
    if (importes != null) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.subtotal, pesos(importes.subtotal)))
                Text(stringResource(R.string.descuento, pesos(importes.descuento)))
                Text(stringResource(R.string.iva, pesos(importes.impuesto)))
                Text(stringResource(R.string.total, pesos(importes.total)), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    estado.aviso?.let { Mensaje(it) }
    Button(onClick = modelo::confirmarPedido, enabled = !estado.confirmando && cliente != null && importes != null) {
        Text(stringResource(if (estado.confirmando) R.string.confirmando else R.string.confirmar))
    }
    OutlinedButton(onClick = modelo::vaciarCarrito, enabled = !estado.confirmando) {
        Text(stringResource(R.string.vaciar_carrito))
    }
}

@Composable
private fun LineaCarrito(
    linea: LineaPedido,
    disponibles: Long,
    importes: ImportesLinea?,
    confirmando: Boolean,
    modelo: ClinicaViewModel
) {
    val producto = linea.producto
    val cantidadAplicada = linea.cantidad.toString()
    var cantidad by rememberSaveable(linea.cantidad) { mutableStateOf(cantidadAplicada) }
    val foco = LocalFocusManager.current
    fun aplicarCantidad(solicitada: String) {
        cantidad = cantidadAplicada
        foco.clearFocus()
        modelo.modificarCantidad(producto, solicitada)
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(producto.nombre, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.precio_neto, pesos(producto.precio)))
            Text(stringResource(R.string.cantidad_en_carrito, linea.cantidad))
            Text(stringResource(R.string.existencias, disponibles))
            DesgloseLinea(importes)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val disminuir = stringResource(R.string.disminuir, producto.nombre)
                OutlinedButton(
                    onClick = { aplicarCantidad((linea.cantidad - 1).toString()) },
                    enabled = !confirmando && linea.cantidad > rangoCantidadArticulos.first,
                    modifier = Modifier.semantics { contentDescription = disminuir }
                ) { Text(stringResource(R.string.menos)) }
                Campo(
                    R.string.cantidad, cantidad, { cantidad = it }, KeyboardType.Number,
                    Modifier.weight(1f), enabled = !confirmando
                )
                val aumentar = stringResource(R.string.aumentar, producto.nombre)
                OutlinedButton(
                    onClick = { aplicarCantidad((linea.cantidad + 1).toString()) },
                    enabled = !confirmando && linea.cantidad < rangoCantidadArticulos.last &&
                        linea.cantidad.toLong() < disponibles,
                    modifier = Modifier.semantics { contentDescription = aumentar }
                ) { Text(stringResource(R.string.mas)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = { aplicarCantidad(cantidad) },
                    enabled = !confirmando && cantidad != cantidadAplicada
                ) { Text(stringResource(R.string.aplicar_cantidad)) }
                val quitar = stringResource(R.string.quitar_articulo, producto.nombre)
                TextButton(
                    onClick = { modelo.quitarProducto(producto) }, enabled = !confirmando,
                    modifier = Modifier.semantics { contentDescription = quitar }
                ) { Text(stringResource(R.string.quitar)) }
            }
        }
    }
}

@Composable
private fun DesgloseLinea(importes: ImportesLinea?) {
    if (importes == null) {
        Text(stringResource(R.string.desglose_no_disponible))
        return
    }
    val estadoDescuento = if (importes.aplicaDescuento) R.string.descuento_aplicado else R.string.descuento_no_aplica
    Text(stringResource(R.string.subtotal_linea, pesos(importes.subtotal)))
    Text(stringResource(R.string.descuento_por_mayor, stringResource(estadoDescuento)))
    Text(stringResource(R.string.importe_descuento_linea, pesos(importes.descuento)))
    Text(stringResource(R.string.neto_linea, pesos(importes.neto)))
}

@Composable
private fun Campo(
    @StringRes etiqueta: Int,
    valor: String,
    editar: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = valor, onValueChange = editar, modifier = modifier, enabled = enabled, singleLine = true,
        label = { Text(stringResource(etiqueta)) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado)
    )
}
