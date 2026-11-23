package cl.duoc.veterinariaapp

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClinicaComposeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val antiparasitario = "Antiparasitario (gatos y perros)"

    @Test fun registraSeleccionaYConfirmaLaCompraDesdeElCatalogo() {
        compose.onNodeWithText("Todavía no hay clientes registrados.").assertIsDisplayed()
        registrarCliente()
        val filaCliente = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and
            hasText("Ana de prueba") and hasText("ana@example.cl")
        compose.onNode(filaCliente).performScrollTo().performClick()
        compose.onNodeWithText("Seleccionado: Ana de prueba (ana@example.cl)").performScrollTo().assertIsDisplayed()

        compose.onNodeWithText("Productos").performClick()
        compose.onNodeWithContentDescription("Agregar $antiparasitario al carrito")
            .performScrollTo().performClick()
        compose.onNodeWithText("Carrito").performClick()
        compose.onNodeWithText("Total: \$7.021 CLP").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Confirmar pedido").performScrollTo().performClick()
        compose.onNodeWithText("Pedidos confirmados: 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pedido PED-001 confirmado.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("El carrito está vacío. Agrega artículos desde Productos.").assertIsDisplayed()

        // Desplazar la fila como un usuario evita el bucle semántico de performScrollTo.
        val filaPestanas = hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
        compose.onNode(filaPestanas).performTouchInput {
            swipeLeft(startX = right * 0.85f, endX = right * 0.15f)
        }
        compose.onNodeWithText("Agenda").assertIsDisplayed().performClick()
        compose.onNodeWithText("En desarrollo").assertIsDisplayed()
    }

    @Test fun recrearLaActividadConservaRegistroYBorradorDelFormulario() {
        registrarCliente()
        editar("Teléfono (opcional)", "912345678")
        closeSoftKeyboard()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Clientes registrados: 1").performScrollTo().assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText("912345678"))
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun carritoRespetaExistenciasEnCatalogoYControlesDeCantidad() {
        compose.onNodeWithText("Productos").performClick()
        compose.onNodeWithText("Registrar producto").performScrollTo().performClick()
        editar("Nombre del producto", "Correa sin stock")
        editar("Precio neto en CLP", "1500")
        editar("Existencia inicial", "0")
        closeSoftKeyboard()
        compose.onNodeWithText("Guardar producto").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Agregar Correa sin stock al carrito")
            .performScrollTo().assertIsNotEnabled()

        compose.onNodeWithContentDescription("Agregar $antiparasitario al carrito")
            .performScrollTo().performClick()
        compose.onNodeWithText("Carrito").performClick()
        val cantidad = compose.onNode(hasSetTextAction() and hasText("Cantidad"))
        val aumentar = compose.onNodeWithContentDescription("Aumentar cantidad de $antiparasitario")
        val aplicar = compose.onNodeWithText("Aplicar cantidad")
        cantidad.performScrollTo().performTextReplacement("9")
        closeSoftKeyboard()
        aplicar.performScrollTo().performClick()
        aumentar.performScrollTo().performClick()
        aumentar.assertIsNotEnabled()
        compose.onNodeWithText("Unidades en el carrito: 10").performScrollTo().assertIsDisplayed()

        cantidad.performScrollTo().performTextReplacement("11")
        closeSoftKeyboard()
        aplicar.performScrollTo().performClick()
        compose.onNode(hasText("$antiparasitario — disponibles: 10, solicitadas: 11.") and hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        compose.onNodeWithText("Existencias insuficientes. El carrito no cambió.").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithText("Entendido").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("10")).assertExists()
        compose.onNodeWithText("Unidades en el carrito: 10").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Existencias disponibles: 10").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Total: \$63.189 CLP").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pedidos confirmados: 0").performScrollTo().assertIsDisplayed()
    }

    @Test fun desgloseMuestraLaCantidadAplicadaYSeConservaTrasUnRechazo() {
        compose.onNodeWithText("Productos").performClick()
        compose.onNodeWithContentDescription("Agregar $antiparasitario al carrito")
            .performScrollTo().performClick()
        compose.onNodeWithText("Carrito").performClick()
        val cantidad = compose.onNode(hasSetTextAction() and hasText("Cantidad"))
        val aplicar = compose.onNodeWithText("Aplicar cantidad")

        cantidad.performScrollTo().performTextReplacement("3")
        closeSoftKeyboard()
        aplicar.performScrollTo().performClick()
        verificarDesglose("17.700", "No aplica", "0", "17.700")

        cantidad.performScrollTo().performTextReplacement("4")
        closeSoftKeyboard()
        verificarDesglose("17.700", "No aplica", "0", "17.700")
        aplicar.performScrollTo().performClick()
        verificarDesglose("23.600", "Aplicado", "2.360", "21.240")
        compose.onNodeWithText("Total: \$25.276 CLP").performScrollTo().assertIsDisplayed()

        cantidad.performScrollTo().performTextReplacement("11")
        closeSoftKeyboard()
        aplicar.performScrollTo().performClick()
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithText("Entendido").performClick()
        compose.onNode(hasSetTextAction() and hasText("4")).assertExists()
        verificarDesglose("23.600", "Aplicado", "2.360", "21.240")

        compose.onNodeWithText("Productos").performClick()
        compose.onNodeWithText("Carrito").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("4")).assertExists()
    }

    private fun verificarDesglose(subtotal: String, estado: String, descuento: String, neto: String) {
        compose.onNodeWithText("Subtotal neto del artículo: \$$subtotal CLP").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Descuento por mayor 10% (desde 4 unidades): $estado").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Monto del descuento: \$$descuento CLP").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Neto después del descuento: \$$neto CLP").performScrollTo().assertIsDisplayed()
    }

    @Test fun errorDeClienteConservaElFormularioYNoIncrementaElResumen() {
        editar("Nombre", "María de prueba")
        editar("Correo electrónico", "sin-arroba")
        editar("Teléfono (opcional)", "912345678")
        editar("Dirección (opcional)", "Calle Ficticia 5")
        closeSoftKeyboard()
        val guardar = compose.onNodeWithText("Guardar cliente")
        guardar.performScrollTo().performClick()
        verificarError("El correo electrónico no es válido")
        compose.activityRule.scenario.recreate()
        verificarError("El correo electrónico no es válido")
        reconocerError()

        verificarCampo("Nombre", "María de prueba")
        verificarCampo("Correo electrónico", "sin-arroba")
        verificarCampo("Teléfono (opcional)", "912345678")
        verificarCampo("Dirección (opcional)", "Calle Ficticia 5")
        compose.onNodeWithText("Clientes registrados: 0").performScrollTo().assertIsDisplayed()

        compose.onNode(hasSetTextAction() and hasText("Correo electrónico"))
            .performScrollTo().performTextReplacement("maria@example.cl")
        closeSoftKeyboard()
        guardar.performScrollTo().performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithText("Cliente registrado.").performScrollTo().assertIsDisplayed()
        guardar.performScrollTo().performClick()
        verificarError("Ya hay un cliente con ese nombre y correo")
        reconocerError()

        compose.onNodeWithText("Productos").performClick()
        compose.onNodeWithText("Clientes").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        verificarCampo("Nombre", "María de prueba")
        verificarCampo("Correo electrónico", "maria@example.cl")
        compose.onNodeWithText("Clientes registrados: 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pedidos confirmados: 0").performScrollTo().assertIsDisplayed()
    }

    @Test fun errorDeProductoConservaElFormularioYNoAgregaAlCatalogo() {
        compose.onNodeWithText("Productos").performClick()
        compose.onNodeWithText("Registrar producto").performScrollTo().performClick()
        editar("Nombre del producto", "Correa de prueba")
        editar("Existencia inicial", "5")
        closeSoftKeyboard()
        val guardar = compose.onNodeWithText("Guardar producto")
        val botonesDeLaCorrea = compose.onAllNodesWithContentDescription("Agregar Correa de prueba al carrito")
        guardar.performScrollTo().performClick()
        verificarError("El precio y la existencia deben ser números enteros")
        reconocerError()
        botonesDeLaCorrea.assertCountEquals(0)
        verificarCampo("Nombre del producto", "Correa de prueba")
        verificarCampo("Existencia inicial", "5")

        editar("Precio neto en CLP", "1500")
        closeSoftKeyboard()
        guardar.performScrollTo().performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithText("Producto registrado.").performScrollTo().assertIsDisplayed()
        guardar.performScrollTo().performClick()
        verificarError("Ya hay un producto con ese nombre y categoría")
        reconocerError()

        compose.onNodeWithText("Clientes").performClick()
        compose.onNodeWithText("Productos").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        botonesDeLaCorrea.assertCountEquals(1)
        verificarCampo("Nombre del producto", "Correa de prueba")
        verificarCampo("Precio neto en CLP", "1500")
        verificarCampo("Existencia inicial", "5")
        compose.onNodeWithText("Pedidos confirmados: 0").performScrollTo().assertIsDisplayed()
    }

    @Test fun historialMuestraElPedidoYConservaElDetalleSinAfectarLaCompra() {
        val pedidos = compose.onNodeWithText("Pedidos")
        pedidos.performScrollTo().performClick()
        compose.onNodeWithText("Los pedidos se guardan en memoria durante esta sesión.").assertIsDisplayed()
        compose.onNodeWithText("Todavía no hay pedidos confirmados.").assertIsDisplayed()

        compose.onNodeWithText("Clientes").performScrollTo().performClick()
        registrarCliente()
        val filaCliente = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and
            hasText("Ana de prueba") and hasText("ana@example.cl")
        compose.onNode(filaCliente).performScrollTo().performClick()
        compose.onNodeWithText("Productos").performScrollTo().performClick()
        val agregar = compose.onNodeWithContentDescription("Agregar $antiparasitario al carrito")
        agregar.performScrollTo().performClick()
        compose.onNodeWithText("Carrito").performScrollTo().performClick()
        compose.onNode(hasSetTextAction() and hasText("Cantidad")).performScrollTo().performTextReplacement("4")
        closeSoftKeyboard()
        compose.onNodeWithText("Aplicar cantidad").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar pedido").performScrollTo().performClick()

        // Dejar una nueva línea sin confirmar para detectar cambios al consultar el historial.
        compose.onNodeWithText("Productos").performScrollTo().performClick()
        agregar.performScrollTo().performClick()
        pedidos.performScrollTo().performClick()
        compose.onNodeWithText("PED-001").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fecha:", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cliente: Ana de prueba").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Total confirmado: \$25.276 CLP").performScrollTo().assertIsDisplayed()
        val correo = compose.onNodeWithText("Correo: ana@example.cl")
        val expandir = compose.onNodeWithText("Ver detalle de PED-001")
        val contraer = compose.onNodeWithText("Ocultar detalle de PED-001")
        correo.assertDoesNotExist()

        expandir.performScrollTo().performClick()
        for (texto in listOf("Correo: ana@example.cl", antiparasitario, "Cantidad: 4",
            "Precio unitario neto: \$5.900 CLP", "Subtotal neto del artículo: \$23.600 CLP",
            "Descuento por cantidad: \$2.360 CLP", "Subtotal neto: \$23.600 CLP",
            "Descuento aplicado al pedido: \$2.360 CLP", "IVA (19%): \$4.036 CLP", "Total: \$25.276 CLP"
        )) {
            compose.onNodeWithText(texto).performScrollTo().assertIsDisplayed()
        }

        compose.activityRule.scenario.recreate()
        correo.performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Carrito").performScrollTo().performClick()
        compose.onNodeWithText("Seleccionado: Ana de prueba (ana@example.cl)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Unidades en el carrito: 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Existencias disponibles: 6").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pedidos confirmados: 1").performScrollTo().assertIsDisplayed()

        pedidos.performScrollTo().performClick()
        correo.performScrollTo().assertIsDisplayed()
        contraer.performScrollTo().performClick()
        correo.assertDoesNotExist()
        expandir.performScrollTo().assertIsDisplayed()
    }

    private fun verificarError(mensaje: String) {
        compose.onNode(hasText(mensaje, substring = true) and hasAnyAncestor(isDialog())).assertIsDisplayed()
    }

    private fun reconocerError() {
        compose.onNodeWithText("Entendido").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
    }

    private fun verificarCampo(etiqueta: String, valor: String) {
        compose.onNode(hasSetTextAction() and hasText(etiqueta) and hasText(valor)).performScrollTo().assertIsDisplayed()
    }

    private fun registrarCliente() {
        editar("Nombre", "Ana de prueba")
        editar("Correo electrónico", "ana@example.cl")
        closeSoftKeyboard()
        compose.onNodeWithText("Guardar cliente").performScrollTo().performClick()
    }

    private fun editar(etiqueta: String, texto: String) {
        compose.onNode(hasSetTextAction() and hasText(etiqueta)).performScrollTo().performTextInput(texto)
    }
}
