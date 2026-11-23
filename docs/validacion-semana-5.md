# Validación final de Semana 5 — Forma B

PMY2201 · Experiencia 2 · VeterinariaApp · rama `semana-5` · 23 de noviembre de 2026

## Resultados ejecutados

Los intervalos de la tabla corresponden a los valores literales de `timestamp` de los XML más recientes de cada módulo. Los XML unitarios usan UTC. El XML instrumentado registra `21:31:35` sin sufijo de zona. El archivo se escribió a las `21:31:37.3556119Z` (18:31:37 en Chile). Las pruebas de los distintos módulos corresponden a sus últimas ejecuciones disponibles, con las fechas indicadas.

| Último resultado por módulo | Marca del XML | Casos | Aprobados | Fallos | Errores | Omitidos |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Gradle · `utilidades:test` | `2026-11-22T18:16:28.127Z` a `18:16:28.198Z` | 7 | 7 | 0 | 0 | 0 |
| Gradle · `datos:test` | `2026-11-23T20:43:18.709Z` a `20:43:18.998Z` | 39 | 39 | 0 | 0 | 0 |
| Gradle · `app:testDebugUnitTest` | `2026-11-23T21:02:25.839Z` | 11 | 11 | 0 | 0 | 0 |
| **Total unitario Gradle** | Último XML por módulo | **57** | **57** | **0** | **0** | **0** |
| Gradle · `app:connectedDebugAndroidTest` | `2026-11-23T21:31:35` | 7 | 7 | 0 | 0 | 0 |

La ejecución instrumentada final utilizó `Pixel_6_API_35`, Android 15 y `emulator-5554`. La suite `ClinicaComposeTest` informó 65,043 s. Se completaron las pruebas de compra y navegación a Agenda, recreación con borrador, límites de stock, desglose y rechazo, errores de cliente y producto, e historial con persistencia del detalle al recrear la actividad. El bloqueo observado en una ejecución previa durante `performScrollTo()` de Agenda quedó fuera del resultado final. La prueba final navega mediante un gesto horizontal y mantiene las comprobaciones de compra y de «En desarrollo».

Una ejecución directa anterior con JUnitCore informó 57 aprobadas de los mismos fuentes unitarios. La compilación directa de recursos, fuentes y siete pruebas Compose se registró por separado. Estos casos coinciden con la cobertura de Gradle y se cuentan una sola vez. Las pruebas instrumentadas de la columna anterior corresponden a ejecución real en el AVD.

## Observaciones manuales

En la revisión de la versión corregida se confirmó el funcionamiento de los límites de stock, el descuento por producto, los diálogos visibles, el desglose por artículo y el historial de pedidos, incluidas las comprobaciones finales del historial. La siguiente tabla separa las observaciones informadas de las verificaciones cubiertas por pruebas y de los pasos manuales que carecen de observación individual.

| Bloque | Evidencia y alcance |
| --- | --- |
| A. Inicio y Agenda | La suite instrumentada verifica el estado inicial sin clientes y que Agenda muestra «En desarrollo». Sin registro manual individual de todas las comprobaciones de inicio. |
| B. Clientes | Pruebas de registro, selección, correo inválido, duplicados, borradores y contador. La normalización y los valores opcionales tienen cobertura unitaria. Sin registro manual individual de B1–B2. |
| C. Productos | Pruebas de alta, duplicados, números inválidos y conservación del formulario. Sin registro manual individual de las cinco categorías ni de cada entrada extrema de C1–C2. |
| D. Carrito y pedidos | Se observaron manualmente los límites de stock, el descuento por producto y los importes por artículo. El historial se comprobó tras confirmar un pedido. Las pruebas unitarias cubren agrupación, límites y confirmación sin duplicados. Las instrumentadas comprueban stock, diálogos y restauración de cantidades. La observación manual disponible no identifica cada combinación de D1–D4. |
| E. Recreación | La suite verifica el registro y borrador del cliente, un diálogo pendiente y la expansión del pedido. Se informó el cierre de las comprobaciones del historial. No consta una sola ejecución manual que inventaríe todos los campos y borradores de E. |

## Correcciones y trazabilidad

Quedaron obsoletas tres expectativas anteriores. Una edición inválida ya no exige buscar el mensaje al pie del carrito. El inventario limita las cantidades desde la edición, de modo que un faltante reproducible en el carrito se rechaza antes de confirmar. El umbral de cuatro unidades se comprueba por producto y el descuento afecta solo a su línea. Se mantiene la prueba de rechazo atómico directamente en inventario.

La adaptación del dominio de `pizzeria-kotlin`, rama `semana-4`, conserva la separación de pedidos, líneas, promoción, validaciones y moneda. En VeterinariaApp esas responsabilidades se distribuyen entre `datos` y `utilidades`, y se integran en `app` con Compose. `CalculosHistoricosTest`, `PromocionTest` y `ValidacionSemana3Test` conservan comprobaciones de las reglas reutilizadas. Los casos de carrito mixto se actualizaron para la nueva regla de descuento por producto. [Descripción de los módulos y ejecución](../README.md).

## Pauta y evidencias

El paso 7 de las instrucciones Forma B y el criterio «Entrega» de la pauta requieren capturas de la estructura modular, la pantalla inicial funcionando con el resumen y los datos actualizados dinámicamente. El paso 7 menciona mascotas, dueños y consultas al describir la evidencia dinámica. Según la aclaración docente utilizada durante la implementación, la demostración se centra en clientes, productos y pedidos de VeterinariaApp. Agenda permanece «En desarrollo». El formato admitido es RAR o ZIP. Las instrucciones no indican un nombre obligatorio. [Instrucciones específicas](https://chatgpt.com/library?file_id=libfile_eb45980842cc81918d2545e2a7a0dfa3&account_id=personal) · [Pauta formativa](https://chatgpt.com/library?file_id=libfile_8c03a412acd48191882d423676102bc3&account_id=personal).

| Criterio | Evidencia disponible |
| --- | --- |
| Modularización y Gradle | Tres módulos, dependencias declaradas y captura de Android Studio con el bloque de dependencias de `app`. |
| Interfaz y conexión con Kotlin | Actividad y ViewModel integrados. Siete casos instrumentados completados en Pixel 6 con API 35. |
| Resumen y actualización dinámica | Casos instrumentados de cliente y compra, resumen con un cliente y dos pedidos, y detalle expandido de `PED-002`. |
| Entrega | Cinco capturas incorporadas. El índice enlaza la estructura modular, el resumen, el pedido expandido, el carrito y el diálogo de existencias. |

[Índice de capturas](evidencias/semana-5/README.md).

## Alcance de la verificación

Las capturas documentan la demostración local y complementan los XML de pruebas. Quedan sin verificar en ejecución API 23, la variante `release` y dispositivos físicos.
