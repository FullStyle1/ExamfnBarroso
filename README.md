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
| Composite API | Propio | REST / JSON | `/api/v1/product-summary/{id}` | Agrega producto, inventario y conversión a MXN |

> **Estado actual:** catálogo e inventario están implementados con datos de ejemplo en H2. El cliente backend de Frankfurter ya está implementado; falta capturar la consulta externa en Postman y desarrollar la Composite API.

## 4. Detalles de implementación previstos

### Orquestación

El cliente de Frankfurter está implementado con `RestClient`, con límites de conexión y lectura. El agregador combinará las consultas independientes en paralelo con las herramientas asíncronas de Java/Spring. Así, el tiempo de respuesta no tiene que ser la suma de sus latencias, sino que se aproxima a la llamada más lenta, más el procesamiento del agregador.

### Conversión de moneda

Frankfurter devuelve el tipo de cambio en `rates.MXN`. La conversión se calculará como:

```text
precioMXN = precioUSD × tipoDeCambio
```

El cliente valida que Frankfurter devuelva el tipo de cambio MXN y propaga los errores de HTTP; no sustituye un error con una tasa ficticia. Para evitar errores de precisión monetaria se usará `BigDecimal`. El tipo de cambio y la fecha usados deben aparecer en la respuesta agregada o ser verificables desde ella.

### Resiliencia

Se definirán límites de tiempo y respuestas ante errores de cada dependencia. Un fallback solo se utilizará si queda explícitamente implementado y documentado; no se presentarán datos ficticios como si fueran datos actuales. La política final deberá aclarar si inventario y divisas son críticos para el resumen y qué información puede omitirse ante una falla.

### Stack detectado

- Java 21
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web, Spring Data JPA, Bean Validation y base de datos H2 configurados en el proyecto

La aplicación inicia desde `backend/src/main/java/com/fullstyle/examfnbarroso/ExamfnBarrosoApplication.java`. El catálogo y el inventario usan Spring Data JPA y H2; los datos de ejemplo con ID `1` se insertan al iniciar si todavía no existen.

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
  "priceUsd": 79.99
}
```

- Si el producto no existe, responde `404 Not Found`.
- **Evidencia 5.1:** guardar una captura de la petición y respuesta `200 OK` de `http://localhost:8080/microservices/products/1`.

### 5.2 Inventario

- Endpoint implementado: `GET /microservices/inventory/{id}`
- Producto de prueba: ID `1`, 25 unidades en `Almacen central - Ciudad de Mexico`.
- Respuesta esperada:

```json
{
  "productId": 1,
  "availableStock": 25,
  "warehouseLocation": "Almacen central - Ciudad de Mexico"
}
```

- Si el inventario no existe, responde `404 Not Found`.
- **Evidencia 5.2:** prueba `GET http://localhost:8080/microservices/inventory/1` en Postman y guarda una captura con la petición y respuesta `200 OK`. Después, detente antes de integrar la API de divisas.

### 5.3 Divisas (Frankfurter)

- Endpoint externo: `GET https://api.frankfurter.app/latest?from=USD&to=MXN`
- El backend ya cuenta con un cliente HTTP que consume esa ruta, sigue la redirección del dominio legado de Frankfurter, valida que la respuesta contenga la tasa MXN y establece límites de tiempo de conexión y lectura.
- **Pausa aquí:** en Postman crea una solicitud `GET` directa a `https://api.frankfurter.app/latest?from=USD&to=MXN` y pulsa **Send**. Guarda una captura que muestre `200 OK` y el JSON con `base`, `date` y `rates.MXN` para la evidencia 5.3. La tasa y la fecha cambian, así que no uses un valor copiado como evidencia de una consulta real.

### 5.4 Composite API

- Endpoint: `GET /api/v1/product-summary/{id}`
- Evidencia: **pendiente**. Capturar el JSON unificado después de comprobar que integra las fuentes y calcula el precio en MXN.

## 6. Conclusiones y lecciones aprendidas

**Pendiente de completar después de implementar y probar el flujo.** Documentar los resultados observados, el comportamiento ante fallos y la comparación entre las llamadas individuales y la petición agregada. No afirmar reducciones de latencia ni tolerancia a fallos sin mediciones o pruebas que lo respalden.

## 7. Repositorio de código fuente

- Repositorio: [ExamfnBarroso en GitHub](https://github.com/FullStyle1/ExamfnBarroso)
- Ramas publicadas: `main`, `developer`, `feat/catalogo` y `feat/inventario`
- Rama actual de divisas: `feat/divisas` (avance local pendiente de publicar)
- El código fuente del backend está en [`backend/`](./backend).

## Cómo ejecutar el proyecto

Se requiere un JDK compatible con Java 21. Desde PowerShell, en la carpeta `backend`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

La aplicación usa el puerto predeterminado de Spring Boot (`8080`). Los endpoints de catálogo e inventario están disponibles. El cliente de Frankfurter se usa desde el backend al conectar la Composite API; todavía no hay un endpoint local de divisas.

## Siguiente recorrido

1. **Catálogo implementado y validado:** guardar la evidencia 5.1.
2. **Ahora: guarda la evidencia 5.3 consultando directamente Frankfurter en Postman; detente aquí.**
3. Después de guardar la captura, implementar la orquestación y pausar para la evidencia 5.4.
4. Redactar las conclusiones con base en las pruebas observadas.
