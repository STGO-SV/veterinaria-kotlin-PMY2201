package cl.duoc.veterinariaapp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.duoc.veterinariaapp.datos.Pedido
import cl.duoc.veterinariaapp.datos.calcularImportesLinea
import cl.duoc.veterinariaapp.utilidades.pesos
import java.time.format.DateTimeFormatter

private val formatoFechaPedido = DateTimeFormatter.ofPattern("dd/MM/uuuu")

@Composable
internal fun SeccionPedidos(pedidos: List<Pedido>) {
    Titulo(R.string.historial_pedidos)
    Text(stringResource(R.string.historial_en_memoria))
    if (pedidos.isEmpty()) {
        Text(stringResource(R.string.historial_vacio))
    }
    for (pedido in pedidos) {
        key(pedido.idPedido) {
            TarjetaPedido(pedido)
        }
    }
}

@Composable
private fun TarjetaPedido(pedido: Pedido) {
    var expandido by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                pedido.idPedido,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium
            )
            Text(stringResource(R.string.fecha_pedido, pedido.fecha.format(formatoFechaPedido)))
            Text(stringResource(R.string.cliente_pedido, pedido.cliente.nombre))
            Text(stringResource(R.string.total_confirmado, pesos(pedido.importes.total)))
            TextButton(onClick = { expandido = !expandido }) {
                val etiqueta = if (expandido) R.string.ocultar_detalle_pedido else R.string.ver_detalle_pedido
                Text(stringResource(etiqueta, pedido.idPedido))
            }
            if (expandido) {
                DetallePedido(pedido)
            }
        }
    }
}

@Composable
private fun DetallePedido(pedido: Pedido) {
    Text(stringResource(R.string.correo_pedido, pedido.cliente.correo))
    for (linea in pedido.productos) {
        val importesLinea = calcularImportesLinea(linea)
        HorizontalDivider()
        Text(linea.producto.nombre, style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.unidades_pedido, linea.cantidad))
        Text(stringResource(R.string.precio_neto, pesos(linea.producto.precio)))
        Text(stringResource(R.string.subtotal_linea, pesos(importesLinea.subtotal)))
        Text(stringResource(R.string.descuento_cantidad_pedido, pesos(importesLinea.descuento)))
    }
    HorizontalDivider()
    val importes = pedido.importes
    Text(stringResource(R.string.subtotal, pesos(importes.subtotal)))
    Text(stringResource(R.string.descuento_aplicado_pedido, pesos(importes.descuento)))
    Text(stringResource(R.string.iva, pesos(importes.impuesto)))
    Text(stringResource(R.string.total, pesos(importes.total)), style = MaterialTheme.typography.titleMedium)
}
