# Examen final: Composite API para consulta de productos

## 2. Introducción y caso de estudio

### Descripción del problema: Chatty I/O

En una arquitectura distribuida de microservicios, el patrón **Chatty I/O** aparece cuando un cliente debe hacer varias peticiones HTTP a servicios independientes para mostrar una sola pantalla. La latencia de esas llamadas se acumula, aumenta el tráfico de red y obliga al cliente a conocer detalles de la topología interna del backend.

Este examen propone una pantalla de resumen de producto que necesita combinar el catálogo, el inventario y el tipo de cambio de USD a MXN.

### Patrón Composite API / API Aggregator

Una **Composite API** (o API agregadora) es una capa de orquestación entre el cliente y los servicios que obtiene los datos. El cliente realiza una sola petición y recibe un resultado consolidado.

Beneficios que se evaluarán:

- **Menos round-trips del cliente:** una llamada para obtener el resumen.
- **Payload enfocado:** el agregador devuelve los datos necesarios para la vista.
- **Desacoplamiento:** el cliente no necesita conocer cada servicio de origen.
- **Resiliencia centralizada:** la capa agregadora puede definir cómo responder ante fallos parciales.

## 3. Especificación técnica

| Servicio | Origen | Protocolo | Endpoint previsto | Responsabilidad |
|---|---|---|---|---|
| Catálogo | Propio | REST / JSON | `/microservices/products/{id}` | Nombre, descripción y precio base en USD |
| Inventario | Propio | REST / JSON | `/microservices/inventory/{id}` | Existencias disponibles y ubicación del almacén |
| Divisas | Frankfurter | REST / JSON | `https://api.frankfurter.app/latest?from=USD&to=MXN` | Tipo de cambio de USD a MXN |
| Composite API | Propio | REST / JSON | `/api/v1/product-summary/{id}` o `/api/v1/products/{id}/summary` | Agrega producto, inventario y conversión a MXN |

Catálogo e inventario exponen endpoints REST dentro de la misma aplicación Spring Boot y persisten en H2. Para cumplir el flujo de consumo del ejercicio, el agregador llama esos endpoints por HTTP mediante clientes configurables; sus URL base se controlan con `CATALOG_SERVICE_URL` e `INVENTORY_SERVICE_URL` y, de forma predeterminada, usan `http://localhost:8080`. Frankfurter se consume como API externa.

## 4. Detalles de implementación previstos

### Orquestación

El agregador consulta por HTTP en paralelo los endpoints REST de catálogo e inventario y la API de Frankfurter mediante `CompletableFuture` y un executor acotado a tres hilos. Los tres clientes HTTP usan `RestClient`; las llamadas tienen límites de conexión de tres segundos y lectura de cinco segundos. Catálogo e Inventario están en la misma aplicación Spring Boot en esta entrega, por lo que las llamadas internas apuntan por defecto a `localhost:8080`; sus URL base se pueden configurar por separado para otros hosts o puertos.

### Conversión de moneda

Frankfurter devuelve el tipo de cambio en `rates.MXN`. La conversión se calculará como:

```text
price_mxn = price_usd × exchange_rate
```

El cliente valida que Frankfurter devuelva el tipo de cambio MXN y sigue la redirección del dominio legado; no sustituye errores con una tasa ficticia. El agregador calcula `price_usd × exchange_rate` usando `BigDecimal` y redondeo a dos decimales (`HALF_UP`), y devuelve el tipo de cambio y la fecha usados.

### Resiliencia

Las llamadas a las APIs tienen límites de tiempo. Si el producto o inventario solicitado no existe, el agregador responde `404`; ante un error de cualquiera de sus dependencias responde `502 Bad Gateway`. No se usa un tipo de cambio falso como fallback.

### Stack detectado

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web, Spring Data JPA, Bean Validation y base de datos H2 configurados en el proyecto

La aplicación inicia desde `backend/src/main/java/com/fullstyle/examfnbarroso/ExamfnBarrosoApplication.java`. El catálogo y el inventario usan Spring Data JPA y H2; los datos de ejemplo con ID `1` se insertan al iniciar si todavía no existen. El catálogo incluye la categoría `Electronica`; inventario incluye un tiempo estimado de entrega de dos días.

## 5. Evidencias de funcionamiento

Capturar las evidencias con Postman, Insomnia o una herramienta equivalente **después de implementar y probar cada endpoint**. No insertar capturas de respuestas simuladas como evidencia de la aplicación.

### 5.1 Catálogo

- Endpoint implementado: `GET /microservices/products/{id}`
- Producto de prueba: ID `1`, audífonos inalámbricos, precio de ejemplo `79.99 USD`.
- Respuesta esperada:

```json
{
  "id": 1,
  "name": "Audifonos inalambricos",
  "description": "Audifonos Bluetooth con cancelacion de ruido.",
  "category": "Electronica",
  "priceUsd": 79.99
}
```

- Si el producto no existe, responde `404 Not Found`.
- **Evidencia 5.1:** validada en Postman; respuesta `200 OK` para `http://localhost:8080/microservices/products/1`.

### 5.2 Inventario

- Endpoint implementado: `GET /microservices/inventory/{id}`
- Producto de prueba: ID `1`, 25 unidades en `Almacen central - Ciudad de Mexico`.
- Respuesta esperada:

```json
{
  "productId": 1,
  "availableStock": 25,
  "warehouseLocation": "Almacen central - Ciudad de Mexico",
  "estimatedDeliveryDays": 2
}
```

- Si el inventario no existe, responde `404 Not Found`.
- **Evidencia 5.2:** endpoint validado en Postman con respuesta `200 OK` para `http://localhost:8080/microservices/inventory/1`.

### 5.3 Divisas (Frankfurter)

- Endpoint externo: `GET https://api.frankfurter.app/latest?from=USD&to=MXN`
- El backend ya cuenta con un cliente HTTP que consume esa ruta, sigue la redirección del dominio legado de Frankfurter, valida que la respuesta contenga la tasa MXN y establece límites de tiempo de conexión y lectura.
- **Evidencia 5.3:** consulta directa a Frankfurter confirmada en Postman. La tasa y la fecha cambian; se conserva en la captura el valor que devolvió el servicio al momento de la prueba.

### 5.4 Composite API

- Endpoint: `GET /api/v1/product-summary/{id}`
- Ruta alternativa solicitada en el planteamiento: `GET /api/v1/products/{id}/summary`
- Las dos rutas devuelven el mismo contrato consolidado: `product_id`, `title`, `description`, `category`, `price_usd`, `price_mxn`, `stock_status`, `available_units`, `warehouse_location`, `estimated_delivery_days`, `currency_conversion_source`, `exchange_rate`, `exchange_rate_date` y `status`.
- `stock_status` es `IN_STOCK` cuando hay unidades disponibles y `OUT_OF_STOCK` cuando el inventario es cero.
- El precio de ejemplo `79.99 USD` con la tasa consultada al momento se devuelve redondeado a dos decimales en `price_mxn`.
- Respuesta representativa (la tasa y fecha son dinámicas):

```json
{
  "product_id": 1,
  "title": "Audifonos inalambricos",
  "description": "Audifonos Bluetooth con cancelacion de ruido.",
  "category": "Electronica",
  "price_usd": 79.99,
  "price_mxn": 1490.30,
  "stock_status": "IN_STOCK",
  "available_units": 25,
  "warehouse_location": "Almacen central - Ciudad de Mexico",
  "estimated_delivery_days": 2,
  "currency_conversion_source": "Frankfurter External API",
  "exchange_rate": 18.6311,
  "exchange_rate_date": "2026-10-28",
  "status": "SUCCESS"
}
```

- **Evidencia 5.4 anterior:** validada en Postman con `200 OK` para `GET http://localhost:8080/api/v1/product-summary/1`. Esa captura corresponde al contrato previo, con datos anidados. Tras alinear la respuesta con el contrato de la rúbrica, vuelve a probar la Composite API y reemplaza la captura por una que muestre los campos consolidados.

## 6. Conclusiones y lecciones aprendidas

La prueba de integración confirmó que la Composite API reúne en una sola respuesta los datos del catálogo, el inventario y el tipo de cambio externo. Desde el cliente, el flujo de resumen requiere una petición al agregador en lugar de consultar cada fuente por separado. La conversión utiliza `BigDecimal` y redondeo monetario; en la captura, `79.99 USD` a `18.6311 MXN/USD` resultó en `1490.30 MXN`.

La respuesta observada fue `200 OK` y Postman reportó `970 ms` para esa petición. Ese tiempo corresponde a una única ejecución y no basta para concluir que hubo una reducción de latencia; para comparar rendimiento habría que medir varias veces las llamadas individuales y la agregada bajo condiciones equivalentes. Las pruebas automatizadas también verifican respuestas `404` para producto/inventario inexistentes y `502 Bad Gateway` cuando falla la consulta de divisas; no se presenta una tasa ficticia como fallback.

## 7. Repositorio de código fuente

- Repositorio: [ExamfnBarroso en GitHub](https://github.com/FullStyle1/ExamfnBarroso)
- Rama de integración y trabajo actual: `developer`
- Ramas de funcionalidad conservadas: `feat/catalogo`, `feat/inventario`, `feat/divisas` y `feat/composite`
- El código fuente del backend está en [`backend/`](./backend).

## Cómo ejecutar el proyecto

Se requiere un JDK compatible con Java 21. Desde PowerShell, en la carpeta `backend`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

La aplicación usa el puerto predeterminado de Spring Boot (`8080`). Los endpoints de catálogo, inventario y Composite API están disponibles. La integración usa llamadas HTTP a los dos endpoints propios y a Frankfurter. Las URL base internas se pueden cambiar con las variables de entorno `CATALOG_SERVICE_URL` e `INVENTORY_SERVICE_URL`.

## Siguiente recorrido

El flujo de implementación y las evidencias de catálogo, inventario y Frankfurter se conservaron. La Composite API ahora invoca los endpoints internos por HTTP y usa el contrato de salida solicitado; actualiza la captura 5.4 para reflejar esa respuesta final.
