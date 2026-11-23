# Evidencias de Semana 5

## Capturas pendientes

El paso 7 de las instrucciones Forma B requiere evidencia de la estructura modular, la pantalla inicial en ejecución y la actualización dinámica de datos. La pauta formativa agrupa esas mismas exigencias en el criterio «Entrega». La evidencia dinámica se presenta mediante clientes, productos y pedidos, según la adaptación académica descrita en el [informe de validación](../../validacion-semana-5.md).

| Archivo previsto | Carácter | Contenido que debe quedar visible |
| --- | --- | --- |
| `s5-01-modulos.png` | Obligatoria | Android Studio con `app`, `datos` y `utilidades` visibles en el árbol y el bloque `dependencies` de `app/build.gradle.kts` abierto. |
| `s5-02-resumen-pixel6.png` | Obligatoria | Aplicación en la pantalla inicial de Clientes en Pixel 6 API 35. Mostrar totales actualizados tras registrar una persona ficticia y confirmar un pedido, junto con el nombre del último cliente. |
| `s5-03-pedido-expandido-pixel6.png` | Obligatoria como evidencia de actualización dinámica | Ficha expandida con folio, cliente ficticio y detalle del pedido registrado. |
| `s5-04-carrito-pixel6.png` | Complementaria | Un producto elegible para descuento. Mostrar cantidad, subtotal neto, descuento, IVA y total. Si el desglose y los totales no caben juntos, obtener una segunda captura contigua. |
| `s5-05-dialogo-pixel6.png` | Complementaria | Diálogo sobre Carrito con producto, existencias, cantidad solicitada y botón **Entendido**. |

## Secuencia de captura

1. Abrir el proyecto final en Android Studio. Mostrar el árbol de módulos y las dependencias directas de `app`. Recortar a la ventana pertinente.
2. Ejecutar `app` en `Pixel_6_API_35`. Registrar «Ana de prueba» con `ana@example.cl` y seleccionarla. Agregar el antiparasitario y dejar cuatro unidades en Carrito. El subtotal de la línea es $23.600, el descuento $2.360, el IVA $4.036 y el total $25.276. Capturar el desglose y los totales.
3. Solicitar 11 unidades con stock 10. Capturar el diálogo y cerrarlo. Confirmar el pedido con las cuatro unidades aplicadas.
4. Abrir **Clientes** y capturar el resumen actualizado. Abrir **Pedidos**, expandir `PED-001` y capturar el detalle.
5. Guardar las imágenes en esta carpeta, verificar legibilidad al tamaño original y agregar al índice enlaces relativos a las capturas que se hayan incorporado. Mantener visibles el identificador del emulador cuando corresponda y solo datos ficticios.

No hay imágenes incorporadas en esta revisión. Las capturas deben corresponder a la implementación final.
