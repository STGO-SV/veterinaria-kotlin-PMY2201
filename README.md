# VeterinariaApp

Duoc UC · Desarrollo de Apps Móviles (PMY2201) · Experiencia 2 · Semana 5 · Forma B
Actividad formativa individual «Integrando Kotlin en Android Studio» · Santiago Andrés Sáez Villarroel

Aplicación Android de demostración para registrar clientes y productos, preparar pedidos y consultar las compras simuladas de la sesión. El identificador de aplicación y el espacio de nombres son `cl.duoc.veterinariaapp`.

## Estructura

| Módulo | Responsabilidad | Dependencias internas |
| --- | --- | --- |
| `app` | Actividad Android, interfaz Jetpack Compose y Material 3, `ClinicaViewModel`, LiveData y pruebas Compose. | `datos`, `utilidades` |
| `datos` | Clientes, catálogo, inventario, carrito, pedidos, descuentos e importes. | `utilidades` |
| `utilidades` | Validación y normalización de entradas, identidad y formato CLP. | Ninguna |

`MainActivity` obtiene el ViewModel y presenta la interfaz Compose. El ViewModel expone el estado mediante LiveData y coordina las operaciones con corrutinas. [Configuración de módulos](settings.gradle.kts) · [Dependencias de la aplicación](app/build.gradle.kts).

## Entorno

| Componente | Versión o configuración |
| --- | --- |
| Windows y Android Studio | Windows 11 x64, Android Studio compilación `262.10968.63.2622.16840690` |
| Compilación | Gradle Wrapper 9.6.0, Android Gradle Plugin 9.5.1, complemento Kotlin para Compose 2.2.10, JDK 25 para el daemon, destino JVM 11 |
| SDK | Compilación y destino Android API 37, mínimo declarado API 23. Build-Tools 36.0.0 disponible en el entorno local |
| Interfaz | Compose BOM 2026.02.01, UI y pruebas Compose 1.10.4, Material 3 1.4.0, Activity Compose 1.8.0 |
| Estado y corrutinas | ViewModel y LiveData 2.9.4, Lifecycle Runtime KTX 2.6.1 declarado, Coroutines Android 1.9.0 |
| Pruebas | JUnit 4.13.2, Kotlin Test 2.2.20, AndroidX JUnit 1.1.5 y Espresso 3.5.1 |
| Dispositivo verificado | Pixel 6 virtual, Android 15, API 35 |

El catálogo de versiones contiene las declaraciones del proyecto. [Catálogo de Gradle](gradle/libs.versions.toml).

## Apertura y ejecución

1. Abrir la carpeta que contiene `settings.gradle.kts` como proyecto en Android Studio. Configurar el SDK local en **SDK Manager** con la plataforma API 37 y confirmar que Gradle usa JDK 25. Permitir que Android Studio genere `local.properties` en el equipo.
2. Sincronizar Gradle. Iniciar el AVD `Pixel_6_API_35` o seleccionar un dispositivo disponible. Elegir la configuración `app` y pulsar **Run**.
3. Ejecutar las comprobaciones desde la raíz en Windows. La prueba conectada requiere un dispositivo en estado `device`.

```powershell
.\gradlew.bat :utilidades:test :datos:test :app:testDebugUnitTest --no-daemon
.\gradlew.bat :app:assembleDebug --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

Los informes de pruebas unitarias se generan bajo `build/test-results` de cada módulo. Los XML de instrumentación se generan bajo `app/build/outputs/androidTest-results`.

## Flujo y reglas

- **Clientes y Productos.** Se registran clientes, se selecciona el cliente del pedido y se administran altas de productos con precio neto y stock inicial. Se rechazan las identidades duplicadas de acuerdo con las reglas del dominio.
- **Carrito.** Se comprueban las existencias al agregar y al cambiar cantidades, con un máximo de 100 unidades por línea. Cotizar no reserva ni consume stock. Al confirmar se valida el pedido completo y se consumen existencias de forma atómica solo si la confirmación se realiza. En caso de rechazo se conserva el carrito con sus cantidades aplicadas.
- **Importes.** Desde cuatro unidades del mismo producto se aplica un descuento del 10 % al subtotal neto de esa línea. Cada descuento por cantidad se trunca al peso antes de sumar. Se usa el mayor entre el descuento total por cantidad y la promoción general por fecha, con prioridad para cantidad en caso de empate. El catálogo inicial carece de promoción activa. El IVA del 19 % se calcula sobre la base descontada con redondeo `HALF_UP` al peso. La interfaz muestra el desglose por artículo y los totales.
- **Validación.** Clientes, Productos y Carrito usan un diálogo compartido para avisos y operaciones rechazadas. El diálogo indica cómo corregir el problema y puede cerrarse con **Entendido**. Un error pendiente se conserva al recrear la actividad. Las confirmaciones satisfactorias se informan en la sección correspondiente.
- **Pedidos.** El historial de solo lectura muestra las compras simuladas de la más reciente a la más antigua. Cada ficha expandible consulta los datos y totales registrados al confirmar. El resumen muestra clientes, pedidos y último cliente. **Agenda** muestra «En desarrollo».

Clientes, catálogo agregado, carrito y pedidos se mantienen en memoria durante la sesión. El ViewModel conserva el estado durante una recreación de la actividad. Al finalizar el proceso se inicia una sesión nueva. La confirmación representa una compra académica simulada y se limita al inventario local.

## Validación

Los últimos XML de Gradle registran **57 pruebas unitarias aprobadas** y **7 instrumentadas aprobadas** en Pixel 6 con API 35. El informe distingue esos resultados de la ejecución directa de JUnit y de las observaciones manuales. Quedan pendientes las comprobaciones en API 23, dispositivos físicos y la variante `release`. [Informe de validación](docs/validacion-semana-5.md) · [Índice de evidencias](docs/evidencias/semana-5/README.md).
