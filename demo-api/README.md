# Demo API - Centralized Logs Library

Proyecto demo para probar la librería `lib-jva-centralizedLogs` v2.0.0.

## Requisitos

- Java 17+
- Docker & Docker Compose
- Maven (o usar el wrapper `./mvnw`)

## Pasos para ejecutar

### 1. Levantar Artemis con Docker

```bash
docker compose up -d
```

Verificar que Artemis está corriendo:
- **Web Console**: http://localhost:8161 (user: `artemis`, password: `artemis`)
- **JMS Port**: `tcp://localhost:61616`

### 2. Compilar y ejecutar el demo

```bash
./mvnw spring-boot:run
```

### 3. Probar los endpoints

```bash
# GET todos los usuarios
curl http://localhost:8080/api/users

# GET un usuario por ID
curl http://localhost:8080/api/users/1

# POST crear un usuario
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","role":"USER"}'

# PUT actualizar usuario
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Lara Updated","email":"lara-updated@example.com","role":"ADMIN"}'

# DELETE eliminar usuario
curl -X DELETE http://localhost:8080/api/users/3

# GET productos
curl http://localhost:8080/api/products

# POST crear producto
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Monitor","description":"Dell 27 4K","price":449.99}'

# Probar error logging
curl http://localhost:8080/api/users/error-test
```

### 4. Verificar los logs en Artemis

1. Abrir la consola web: http://localhost:8161
2. Ir a **Queues** → `log.send` (logs exitosos) o `log.errors` (errores)
3. Click en **Browse** para ver los mensajes

### 5. Probar compresión GZIP

Cambiar en `application.yml`:

```yaml
centralized-logs:
  compression:
    enabled: true
```

Reiniciar la app y los nuevos mensajes se enviarán comprimidos como `BytesMessage`.

### 6. Apagar todo

```bash
docker compose down
```
