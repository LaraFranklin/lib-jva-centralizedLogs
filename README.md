# lib-jva-centralizedLogs

Librería Java para la captura centralizada de logs HTTP mediante interceptores Spring MVC y Apache ActiveMQ Artemis. Captura automáticamente los request y response de todos los endpoints de tu aplicación y los publica en colas de mensajería para su procesamiento centralizado.

---

## Requisitos

| Herramienta | Versión |
|---|---|
| Java | 17+ |
| Spring Boot | 3.x |
| Apache ActiveMQ Artemis | 2.x+ |

---

## Instalación

### 1. Agregar la dependencia en `pom.xml`

```xml
<dependency>
    <groupId>com.organization</groupId>
    <artifactId>lib-jva-centralizedLogs</artifactId>
    <version>1.0.0</version>
</dependency>
```

> ✅ **No se necesita `@ComponentScan`**. La librería usa auto-configuración de Spring Boot y se registra automáticamente.

### 2. Configurar `application.properties`

Agrega las siguientes propiedades en el `application.properties` de tu proyecto:

```properties
# ── Información del servicio ──────────────────────────────
centralized-logs.service-name=nombre-de-tu-servicio
centralized-logs.environment=dev

# ── Conexión a Artemis ────────────────────────────────────
centralized-logs.broker-url=tcp://localhost:61616
centralized-logs.user=artemis
centralized-logs.password=artemis

# ── Colas ─────────────────────────────────────────────────
centralized-logs.queue.logs=log.send
centralized-logs.queue.errors=log.errors

# ── Control de la librería ────────────────────────────────
centralized-logs.interceptor.enabled=true
centralized-logs.sender.enabled=true

# ── Opciones avanzadas ────────────────────────────────────
centralized-logs.body-max-size=10000
centralized-logs.interceptor.exclude-paths=/actuator/**,/health,/swagger-ui/**,/v3/api-docs/**
```

---

## Variables de configuración

| Propiedad | Obligatoria | Default | Descripción |
|---|---|---|---|
| `centralized-logs.service-name` | ✅ | `unknown-service` | Nombre del microservicio que usa la librería |
| `centralized-logs.environment` | ✅ | `unknown` | Ambiente de ejecución (`dev`, `qa`, `prod`) |
| `centralized-logs.broker-url` | ✅ | `tcp://localhost:61616` | URL del broker Artemis |
| `centralized-logs.user` | ✅ | `artemis` | Usuario del broker |
| `centralized-logs.password` | ✅ | `artemis` | Contraseña del broker |
| `centralized-logs.queue.logs` | ❌ | `log.send` | Cola donde se publican los logs exitosos |
| `centralized-logs.queue.errors` | ❌ | `log.errors` | Cola donde se publican los errores |
| `centralized-logs.interceptor.enabled` | ❌ | `true` | Activa o desactiva el interceptor completo |
| `centralized-logs.sender.enabled` | ❌ | `true` | Activa o desactiva el envío a Artemis |
| `centralized-logs.body-max-size` | ❌ | `10000` | Tamaño máximo (en caracteres) del body capturado |
| `centralized-logs.interceptor.exclude-paths` | ❌ | `/actuator/**,/health,...` | Rutas excluidas del interceptor |
| `centralized-logs.endpoint-mappings` | ❌ | `{}` | Mapa de endpoints a IDs descriptivos (ver sección abajo) |

---

## Control del interceptor

### `centralized-logs.interceptor.enabled`

Controla si el interceptor y el filter se registran en el ciclo de vida de Spring.

```
true  → Request → Interceptor → Artemis ✓
false → Request → Controller (el interceptor no se registra) ✗
```

### `centralized-logs.sender.enabled`

Controla si los mensajes se envían a Artemis. El interceptor sigue capturando y logueando en consola.

```
true  → Interceptor captura y envía a Artemis ✓
false → Interceptor captura y loguea en consola, pero NO envía a Artemis ✗
```

| Escenario | `interceptor.enabled` | `sender.enabled` |
|---|---|---|
| Producción completa | `true` | `true` |
| Sin Artemis disponible (desarrollo) | `true` | `false` |
| Deshabilitar completamente | `false` | `true` o `false` |

---

## Estructura del mensaje en cola

Cada petición HTTP interceptada genera el siguiente JSON en la cola:

```json
{
  "eventId": "c2c2f6a8-3d2d-4f8a-8b1f-5d4b2a3a9d10",
  "timestamp": "2026-02-18T11:35:58.123Z",
  "serviceName": "payments-api",
  "environment": "qa",
  "correlationId": "7f9c2a1b0e2d4a6c",
  "endpointId": "create-payment",
  "http": {
    "method": "POST",
    "path": "/v1/payments",
    "queryString": "",
    "statusCode": 201
  },
  "durationMs": 42,
  "client": {
    "ip": "10.10.10.10",
    "userAgent": "Mozilla/5.0"
  },
  "request": {
    "headers": {
      "content-type": "application/json",
      "x-correlation-id": "7f9c2a1b0e2d4a6c"
    },
    "body": { }
  },
  "response": {
    "headers": {
      "content-type": "application/json"
    },
    "body": { }
  },
  "result": "SUCCESS"
}
```

Cuando ocurre una excepción, el mensaje se publica en `log.errors` con el campo `error` adicional:

```json
{
  "...": "mismos campos base",
  "result": "ERROR",
  "error": {
    "exceptionType": "NullPointerException",
    "message": "Cannot invoke method getAmount()",
    "stackTrace": "com.example.PaymentService.process(PaymentService.java:45)\n...",
    "cause": null
  }
}
```

---

## Correlation ID

La librería busca automáticamente el header `x-correlation-id` en cada request. Si no existe, genera un UUID aleatorio.

```
Header presente  → usa el valor recibido
Header ausente   → genera UUID automáticamente
```

Para propagar el correlation ID desde tu cliente:

```
x-correlation-id: 7f9c2a1b0e2d4a6c
```

---

## Endpoint Mappings (identificadores descriptivos)

Para integraciones con bases de datos de logs centralizados donde necesitas un identificador descriptivo por endpoint, puedes configurar un mapa de patrones a IDs:

```yaml
centralized-logs:
  endpoint-mappings:
    "GET /api/users": "get-all-users"
    "POST /api/users": "create-user"
    "GET /api/users/*": "get-user-by-id"
    "PUT /api/users/*": "update-user"
    "DELETE /api/users/*": "delete-user"
    "POST /api/payments": "create-payment"
    "GET /api/payments/**": "get-payments"
```

### Reglas de resolución

| Prioridad | Tipo | Ejemplo patrón | Ejemplo request | Resultado |
|---|---|---|---|---|
| 1 | Exacto | `GET /api/users` | `GET /api/users` | ✅ Match |
| 2 | Wildcard `*` | `GET /api/users/*` | `GET /api/users/123` | ✅ Match (un segmento) |
| 3 | Wildcard `**` | `GET /api/reports/**` | `GET /api/reports/2026/02` | ✅ Match (múltiples segmentos) |
| 4 | Auto-generado | _(sin configurar)_ | `GET /api/health` | `GET_/api/health` |

El campo `endpointId` aparece en cada evento JSON publicado en la cola. Si no configuras ningún mapping, se genera automáticamente en el formato `METHOD_/path`.

---

## Flujo interno

```
HTTP Request
    ↓
CachingRequestResponseFilter   → envuelve request/response para permitir lectura múltiple
    ↓
RequestInterceptor.preHandle   → registra timestamp de inicio
    ↓
Controller                     → procesa la petición
    ↓
RequestInterceptor.afterCompletion
    ├── Sin excepción → LogEventBuilder → cola log.send   (result: SUCCESS)
    └── Con excepción → LogEventBuilder → cola log.errors (result: ERROR)
    ↓
CachingFilter                  → copyBodyToResponse() devuelve respuesta al cliente
```

---

## Componentes de la librería

| Clase | Paquete | Descripción |
|---|---|---|
| `CentralizedLogsProperties` | `config` | Propiedades centralizadas con `@ConfigurationProperties` |
| `CentralizedLogsAutoConfiguration` | `config` | Auto-configuración de Spring Boot (no requiere `@ComponentScan`) |
| `CachingRequestResponseFilter` | `interceptor` | Filter que envuelve request/response para lectura múltiple del body |
| `RequestInterceptor` | `interceptor` | Interceptor principal que captura y publica los logs |
| `LogEventBuilder` | `service` | Construye el JSON estructurado del evento de log |
| `InterceptorConfig` | `config` | Registra el interceptor en Spring MVC con control de activación |
| `ArtemisConfig` | `config` | Configuración de la conexión JMS con Artemis |
| `ArtemisMessageSender` | `service` | Interfaz para el envío de mensajes |
| `ArtemisMessageSenderImpl` | `service` | Implementación del envío a colas Artemis |

---

## Variables de entorno (alternativa a properties)

Las propiedades también pueden configurarse como variables de entorno en Docker o Kubernetes:

```bash
# Docker / Linux
CENTRALIZED_LOGS_SERVICE_NAME=payments-api
CENTRALIZED_LOGS_ENVIRONMENT=prod
CENTRALIZED_LOGS_BROKER_URL=tcp://artemis-host:61616
CENTRALIZED_LOGS_USER=artemis
CENTRALIZED_LOGS_PASSWORD=artemis
CENTRALIZED_LOGS_QUEUE_LOGS=log.send
CENTRALIZED_LOGS_QUEUE_ERRORS=log.errors
CENTRALIZED_LOGS_INTERCEPTOR_ENABLED=true
CENTRALIZED_LOGS_SENDER_ENABLED=true
CENTRALIZED_LOGS_BODY_MAX_SIZE=10000
```

```yaml
# Docker Compose
environment:
  CENTRALIZED_LOGS_SERVICE_NAME: "payments-api"
  CENTRALIZED_LOGS_ENVIRONMENT: "prod"
  CENTRALIZED_LOGS_BROKER_URL: "tcp://artemis-host:61616"
  CENTRALIZED_LOGS_INTERCEPTOR_ENABLED: "true"
  CENTRALIZED_LOGS_SENDER_ENABLED: "true"
```

```yaml
# Kubernetes
env:
  - name: CENTRALIZED_LOGS_INTERCEPTOR_ENABLED
    value: "false"
  - name: CENTRALIZED_LOGS_SENDER_ENABLED
    value: "true"
```

---

## Migración desde v1.x

### Cambios de propiedades

| v1.x | v2.x |
|---|---|
| `spring.activemq.service.name` | `centralized-logs.service-name` |
| `spring.artemis.enviroment` | `centralized-logs.environment` |
| `spring.artemis.broker-url` | `centralized-logs.broker-url` |
| `spring.artemis.user` | `centralized-logs.user` |
| `spring.artemis.password` | `centralized-logs.password` |
| `spring.artemis.queue.logs` | `centralized-logs.queue.logs` |
| `spring.artemis.queue.errors` | `centralized-logs.queue.errors` |
| `spring.artemis.interceptor.enabled` | `centralized-logs.interceptor.enabled` |
| `spring.artemis.sender.enabled` | `centralized-logs.sender.enabled` |

### Otros cambios

- **Auto-configuración**: Ya no necesitas `@ComponentScan`. Elimina la referencia a `com.organization.lib` de tu `@ComponentScan`.
- **Typo corregido**: `enviroment` → `environment`
- **Nuevas propiedades**: `centralized-logs.body-max-size`, `centralized-logs.interceptor.exclude-paths`
- **Comportamiento del sender**: Ya no lanza `RuntimeException` si Artemis no está disponible. Los errores se loguean pero no interrumpen la respuesta HTTP.
