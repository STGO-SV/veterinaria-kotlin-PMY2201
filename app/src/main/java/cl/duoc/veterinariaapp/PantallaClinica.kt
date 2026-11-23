package cl.duoc.veterinariaapp

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import cl.duoc.veterinariaapp.datos.ResumenClinica

private enum class Seccion(@param:StringRes val titulo: Int) {
    CLIENTES(R.string.clientes),
    PRODUCTOS(R.string.productos),
    CARRITO(R.string.carrito),
    PEDIDOS(R.string.pedidos),
    AGENDA(R.string.agenda_titulo)
}

private val coloresClaros = lightColorScheme(
    primary = Color(0xFF1B614D), onPrimary = Color.White,
    primaryContainer = Color(0xFFD5ECE2), onPrimaryContainer = Color(0xFF123F32),
    background = Color(0xFFF7FAF8), surface = Color(0xFFF7FAF8), onSurface = Color(0xFF172C25)
)

private val coloresOscuros = darkColorScheme(
    primary = Color(0xFFA3D8C3), onPrimary = Color(0xFF00392B),
    primaryContainer = Color(0xFF245143), onPrimaryContainer = Color(0xFFE0F3EA),
    background = Color(0xFF101C17), surface = Color(0xFF101C17), onSurface = Color(0xFFE4EFE9)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaClinica(modelo: ClinicaViewModel, duenio: LifecycleOwner) {
    var estado by remember(modelo) { mutableStateOf(checkNotNull(modelo.estado.value)) }
    DisposableEffect(modelo, duenio) {
        val observador = Observer<EstadoClinica> { estado = it }
        modelo.estado.observe(duenio, observador)
        onDispose { modelo.estado.removeObserver(observador) }
    }

    var seccion by rememberSaveable { mutableStateOf(Seccion.CLIENTES) }
    val formularios = rememberSaveableStateHolder()
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) coloresOscuros else coloresClaros) {
        estado.errorPendiente?.let { DialogoError(it, modelo::reconocerError) }
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                Surface(tonalElevation = 2.dp) {
                    Column(Modifier.statusBarsPadding()) {
                        Text(
                            stringResource(R.string.app_name),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).semantics { heading() },
                            style = MaterialTheme.typography.titleLarge
                        )
                        PrimaryScrollableTabRow(selectedTabIndex = seccion.ordinal, edgePadding = 0.dp) {
                            Seccion.entries.forEach { opcion ->
                                Tab(
                                    selected = seccion == opcion,
                                    onClick = { seccion = opcion },
                                    text = { Text(stringResource(opcion.titulo), maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            }
        ) { espacio ->
            Box(
                modifier = Modifier.fillMaxSize().padding(espacio),
                contentAlignment = Alignment.TopCenter
            ) {
                formularios.SaveableStateProvider(seccion.name) {
                    Column(
                        Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Resumen(estado.resumen)
                        when (seccion) {
                            Seccion.CLIENTES -> SeccionClientes(estado, modelo)
                            Seccion.PRODUCTOS -> SeccionProductos(estado, modelo)
                            Seccion.CARRITO -> SeccionCarrito(estado, modelo)
                            Seccion.PEDIDOS -> SeccionPedidos(estado.pedidos)
                            Seccion.AGENDA -> {
                                Titulo(R.string.agenda_titulo)
                                Text(stringResource(R.string.agenda_estado))
                                Text(stringResource(R.string.agenda_futura))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoError(aviso: Aviso, reconocer: () -> Unit) {
    AlertDialog(
        onDismissRequest = reconocer,
        title = { Titulo(R.string.titulo_error) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(aviso.recurso))
                for (faltante in aviso.faltantes) {
                    Text(stringResource(R.string.detalle_faltante, faltante.producto.nombre, faltante.disponible, faltante.requerido))
                }
                if (aviso.faltantes.isNotEmpty()) {
                    Text(stringResource(R.string.corregir_stock))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = reconocer) { Text(stringResource(R.string.entendido)) }
        }
    )
}

@Composable
private fun Resumen(resumen: ResumenClinica) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Titulo(R.string.resumen)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.resumen_clientes, resumen.totalClientes),
                    Modifier.weight(1f), style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(R.string.resumen_pedidos, resumen.totalPedidos),
                    Modifier.weight(1f), style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                resumen.ultimoCliente?.let { stringResource(R.string.resumen_ultimo, it.nombre) }
                    ?: stringResource(R.string.resumen_sin_cliente)
            )
        }
    }
}

@Composable
internal fun Titulo(@StringRes titulo: Int) {
    Text(
        stringResource(titulo),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
internal fun Mensaje(aviso: Aviso) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val argumento = aviso.argumento
            Text(if (argumento == null) stringResource(aviso.recurso) else stringResource(aviso.recurso, argumento))

        }
    }
}
