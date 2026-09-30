# Azure Students: implementación local y despliegue separado

Estado: código, adaptadores, plantillas y workflows preparados localmente. **No hay recursos creados ni despliegue cloud validado.** No se cambiaron los contenedores, puertos, volúmenes ni datos del entorno de desarrollo. Revisar [costos](COSTS.md) antes de aprobar aprovisionamiento.

## Dos entornos, la misma aplicación

| Componente | Local, como antes | Azure, perfil explícito |
| --- | --- | --- |
| Frontend | Angular en 4200, API en 8080 | Angular y Java en la misma Web App HTTPS por defecto |
| API Java | `.env`, perfil `local` | App Service Linux Java 21, `SPRING_PROFILES_ACTIVE=azure` |
| Worker Python | Docker/RabbitMQ/MinIO | Segunda Web App Python 3.12 en el mismo plan B2, `APP_ENV=azure` |
| Base de datos/vectores | PostgreSQL/pgvector Docker | PostgreSQL Flexible Server 16 B1ms con VECTOR |
| Documentos | MinIO | Blob privado, Managed Identity, enlaces de lectura limitados |
| Mensajería | RabbitMQ y mensajes actuales | Service Bus Standard; resultados grandes en Blob y referencias en cola |
| Configuración sensible | `.env` ignorado | Key Vault, referencias por identidad; nunca en Angular |
| Esquema | Comportamiento local `ddl-auto=update` preservado | Flyway en CD con propietario; aplicación `thesis_app` valida sin DDL |
| Telemetría | Logs locales | Azure Monitor/App Insights, muestreo Python y Log Analytics limitado |

No hay VM, Container Apps, ACR, VNet, Private Endpoint ni staging duplicado. Los endpoints públicos conservan TLS y autorización. PostgreSQL usa `verify-full` con truststore del JDK para comprobar certificado/hostname; validar la cadena CA en App Service antes del primer despliegue, sin desactivar validación si falla. Solo permite las IPs salientes previstas de las apps y, durante una migración, una IP temporal exacta del runner que se elimina en `finally`.

## Sin dominio propio

Para el primer despliegue se propone `frontendHosting=app-service`: navegador → `https://hs-thesis-api-....azurewebsites.net` → `/api/v1`; Java → worker, Blob, Service Bus y PostgreSQL. Cookies HttpOnly/Secure/Strict, sin cookies de terceros ni proxy SWA para SSE.

La protección CSRF permanece activa tanto en local como en Azure, incluidos login, refresh, logout, uploads y streaming POST. Antes de escribir, el cliente consulta `GET /api/v1/auth/csrf` con credenciales: recibe un token enmascarado (handler XOR de Spring), `Cache-Control: no-store` y una cookie de sesión HttpOnly/Strict (Secure en Azure). Envía el token como `X-XSRF-TOKEN`; el token no sustituye autenticación ni permisos. Las herramientas API/carga deben conservar esa cookie y enviar el header. Publicar frontend y backend conjuntamente: el frontend anterior no implementa este contrato. No se desactiva CSRF ni se exceptúan los endpoints de autenticación para acomodar las pruebas. Referencia: [CSRF Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

SWA sigue siendo opcional. Su frontend por defecto y una API `azurewebsites.net` serían sitios distintos: `SameSite=None`/CORS no garantiza el login en navegadores que bloquean cookies de terceros. No darlo por validado sin probar. Para SWA, resolver dominio/certificados de `app.dominio` y `api.dominio`, configurar CORS/origin real y probar cookies, refresh y streaming. La API se llama directamente: no se usa el proxy `/api` de SWA, cuya duración máxima documentada es 45 segundos. El despliegue SWA está bloqueado hasta configurar `AZURE_SWA_LOGIN_VALIDATED=true` y su identidad/target Students.

## Requisitos y preflight, sin crear recursos

Suscripción única permitida: `86d9e5e6-b9bf-44b4-915a-106207e0bc02`, tenant `0e0cb060-09ad-49f5-a005-68b9b49aa1f6`. El guard comprueba oferta `AzureForStudents_*`, Enabled y spending limit On, no el nombre ni el default del CLI.

```powershell
az login --tenant 0e0cb060-09ad-49f5-a005-68b9b49aa1f6 --use-device-code
./infra/azure/Validate-Students.ps1
./infra/azure/Test-StudentsGuard.ps1
```

Azure CLI, Bicep, Java 21, Python 3.12 y Docker solo para tests locales. La compilación Bicep no prueba cuotas/región/RBAC. Revisar `az postgres flexible-server list-skus --location mexicocentral --subscription <Students-ID>` y permisos efectivos. El 29/09 se solicitó el registro explícito de Microsoft.Web, Storage, ServiceBus, KeyVault, DBforPostgreSQL, Insights y OperationalInsights en Students para permitir el preflight; Consumption ya estaba registrado. Este registro no crea recursos pagos. Comprobar que termine antes de provisionar y no cambiar de oferta ante restricciones.

### Región y política de esta suscripción

La consulta del 29/09/2026 a `sys.regionrestriction` permite solamente `westus`, `canadacentral`, `westus3`, `mexicocentral` y `northcentralus`: **East US y East US 2 están bloqueadas**, aunque el catálogo global anuncie sus SKU. Se autorizó evaluar otra región y se seleccionó **Mexico Central**: el catálogo consultado anuncia App Service Linux B2, PostgreSQL Standard_B1ms y Service Bus. Es una selección por política, disponibilidad publicada y costo, no una medición de latencia desde Lima ni una garantía de capacidad. La latencia del despliegue real se medirá antes de declarar cloud listo. Todos los servicios quedan en la misma región; sin VNet/subnets.

`Validate-Students.ps1 -Region mexicocentral` y `Provision-Students.ps1` comprueban la política vigente. `Preview-Students.ps1 -Region mexicocentral` ejecuta un what-if usando valores ficticios, sin leer OpenRouter ni credenciales reales y sin crear recursos. Una vista previa incompleta o con errores no equivale a un despliegue validado.

La API regional de usos devolvió un resumen `*` con 0/0; no se interpretó como prueba suficiente de bloqueo. La API específica `Microsoft.Web/validate` respondió **Success para B2 Linux, capacidad 1 en Mexico Central**, sin crear el grupo ni el plan. `Test-AppServiceCapacity.ps1` repite esa comprobación antes de cada Create: un what-if exitoso no reemplaza la validación del proveedor. La capacidad puede variar posteriormente. Se solicitó el registro de Microsoft.Quota para consultas, sin solicitar aumentos ni cambiar la oferta.

## Aprovisionamiento futuro, solo tras aprobar costo

Copiar `parameters.example.json` a `parameters.local.json` (ignorado), completar valores fuertes distintos y proteger el archivo local. No compartirlo, no adjuntarlo a PR ni imprimirlo. API keys, passwords y JWT son secretos aunque coloquialmente se llamen llaves. Para repetir un despliegue reutilizar valores existentes; no rotar contraseñas/keys accidentalmente.

```powershell
./infra/azure/Provision-Students.ps1 -ParametersFile ./infra/azure/parameters.local.json -Mode WhatIf
# Solo después de revisión y aprobación de recursos pagos:
./infra/azure/Provision-Students.ps1 -ParametersFile ./infra/azure/parameters.local.json -Mode Create -ApprovePaidResources
```

What-if necesita sesión/RBAC/providers adecuados, pero no crea recursos por diseño. La plantilla es para un esquema cloud nuevo: migrar datos locales es una decisión separada. No tocar la BD de desarrollo ni restaurarla automáticamente. Si se necesita trasladar datos: respaldar PostgreSQL y MinIO, probar restore en una BD destino desechable, comprobar esquema/pgvector antes de establecer una baseline explícita, copiar objetos conservando keys, verificar checksums y recién después cambiar endpoints. Nunca activar baseline-on-migrate automáticamente sobre una BD poblada.

## GitFlow y CD

1. Ambas ramas `feature/azure-students-deployment` → PR a `develop`, CI de tests/seguridad y revisión humana.
2. Validar conjuntamente; abrir nueva `release/<versión>` desde develop, PR a main en ambos repos. No reusar ni mover `v0.1.1`: ese tag no incluye esta implementación.
3. Integrar release de vuelta a develop y publicar el mismo nuevo tag semántico en ambos repositorios.
4. Crear el GitHub Environment `azure-students`, aprobar revisores y restringir despliegues a tags de release. Activar `AZURE_CD_ENABLED=true` solo tras provisionar/configurar.
5. Ejecutar **Frontend Azure release** con el tag y modo app-service: tests/build → asset `frontend.zip` + manifiesto SHA256 en GitHub Release. No modifica la API.
6. Ejecutar **Backend Azure CD** con el mismo tag: verifica que ambos commits están en main, comprueba tag/SHA256 del frontend, ejecuta Java/Python/integración y empaqueta Angular dentro del JAR y el worker mediante allowlist.
7. Aprobación del environment → login OIDC → guard Students → Flyway con dueño DB y firewall temporal → worker → Java/Angular → health/deep-link/runtime checks.
8. Ejecutar **Azure browser smoke** en front para login, cookies Secure/HttpOnly y recarga de ruta. Antes de declarar cloud listo, completar el checklist siguiente.

Los workflows manuales deben ejecutarse **seleccionando el nuevo tag como ref**, no main ni una feature; el input `release_tag` debe coincidir con esa ref. Los environments preparados con `Configure-GitHubEnvironment.ps1` exigen revisión de `sebaditas`, sin bypass administrativo y sin autoaprobación: el compañero inicia el workflow y `sebaditas` aprueba. La variable CD permanece false hasta que existan infraestructura, OIDC, variables, secretos y cuentas de prueba. No crear/mover un tag antiguo para hacer visible un workflow nuevo: primero integrarlo a main mediante PR.

Basic B2 no tiene deployment slots: puede haber interrupción breve, no se promete blue/green. Mantener el artefacto anterior; rollback de app solo si es compatible con el esquema vigente. No deshacer la BD con scripts destructivos. Para cambios incompatibles aplicar expand/contract y respaldo/restore probado.

## OIDC y variables de GitHub

Después de existir los recursos, `Configure-GitHubOidc.ps1 -BackendApp <nombre> -WorkerApp <nombre> -PostgresServer <nombre> -KeyVault <nombre> -ApproveIdentityChanges` crea/reutiliza una identidad sin client secret, federada a `repo:HS-Education/hs-backend:environment:azure-students`. Requiere permisos Entra/RBAC; no se ha ejecutado. Reader en Students para validar oferta, Website Contributor solo en las dos apps, rol limitado a firewall PostgreSQL y Secrets User solo en las dos credenciales de migración. No dar Owner/Contributor de toda la suscripción al CD.

Backend environment secrets: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`. Variables: `AZURE_RESOURCE_GROUP`, `AZURE_BACKEND_APP`, `AZURE_WORKER_APP`, `AZURE_POSTGRES_SERVER`, `AZURE_KEY_VAULT`, `AZURE_BACKEND_URL`, `AZURE_WORKER_URL`. Front: `AZURE_FRONTEND_URL`, `AZURE_API_BASE_URL` (URL cloud absoluta terminada en `/api/v1` para smoke), y secrets `SMOKE_USERNAME`/`SMOKE_PASSWORD` de cuenta de prueba autorizada. Los nombres/URLs salen de los outputs públicos de Bicep. App Service usa `/api/v1` relativo en el bundle.

Si front es privado, `FRONTEND_RELEASE_READ_TOKEN` de lectura mínima de contents para ese repo debe permitir download y clone; no incluir token en URLs/logs. SWA adicional requiere token de deployment propio y federación `repo:HS-Education/hs-front:environment:azure-students` con lectura del target Students; es configuración futura, no validada ahora.

## Reintentos y límites

Un job lleva ID + generación + key privada, no URL arbitraria. Worker publica primero Blob con hash y luego referencia <1 KB; solo confirma el job después de publicación durable. Referencias validan SHA256/tamaño/dimensiones y la generación vigente. Cinco entregas máximas, renovación de lock hasta 30 minutos; errores terminales seguros y DLQ para contratos inválidos. TTL de cola 7 días, blobs temporales 14 días: no reenviar referencias de DLQ fuera de retención; reprocesar documento. Vigilar DLQ y pendientes manualmente hasta configurar alertas operativas.

Una instancia y una suscripción de notificaciones son intencionadas para costo: **no escalar a múltiples réplicas** sin fan-out por instancia o un canal SSE compartido. Pool DB 5, Java heap 768 MB, provider concurrente 2, consumo de documentos uno a uno; son límites iniciales, no una capacidad garantizada para un número de usuarios. Medir la carga representativa en cloud antes de ampliarlos.

## Validación local realizada — 29/09/2026

- `mvn -Plocal-gate verify`: 80 pruebas unitarias Java sin fallos; 6 de integración ejecutadas sin fallos. Las 2 opt-in ClamAV se omitieron porque `RUN_MALWARE_E2E` no está habilitado en esta ejecución.
- Python: 53 pruebas offline aprobadas, incluidas validación de entorno, referencias Blob, reintentos/settlement y referencias Key Vault no resueltas. No se enviaron peticiones reales a OpenRouter.
- Empaquetado: 4 pruebas aprobadas; frontend ZIP/manifiesto comprobados e incluidos en el JAR; PropertiesLauncher verifica las migraciones del paquete sin conectarse a una BD. Se usó `v0.0.0` como fixture local de packaging, **no como tag Git**.
- Frontend: 45 pruebas Vitest, 3 de configuración de despliegue e i18n aprobadas; build production aprobado con la advertencia de tamaño ya existente. El smoke Playwright cloud se preparó y se comprobó su descubrimiento, **no se ejecutó contra Azure**.
- Bicep compiló, actionlint validó los workflows de ambos repos y los scripts PowerShell pasaron revisión sintáctica. Guard Students: 1 caso positivo y 5 negativos aprobados sin autenticación cloud.
- Sesión CLI Students verificada, spending limit On e inventario de grupos vacío al preflight. No se registraron providers, no se aprovisionó, no se configuró OIDC remoto y no hubo commit/push.

### Revisión adicional de publicación

- Se actualizaron ambas feature con las referencias remotas: partían del develop actual, sin divergencia.
- Se repitieron 80 pruebas unitarias Java y 6 integraciones (2 opt-in ClamAV omitidas), 53 Python, 4 packaging, 45 Vitest, 3 deployment y build/i18n. La advertencia de bundle sigue siendo la ya conocida.
- Guard regional: 1 positivo/4 negativos; parámetros privados: 1 positivo/7 negativos, además de los 6 casos de suscripción. `.env`, variantes `.env.*`, parámetros locales, claves privadas y artefactos generados quedan fuera de Git. El escaneo de firmas de credenciales no encontró coincidencias; no sustituye un escáner integral.
- Se prepararon ambos environments `azure-students` con `sebaditas` como revisor, tags `v*`, sin autoaprobación ni bypass; `AZURE_CD_ENABLED=false`. La identidad OIDC/RBAC no se crea antes de existir sus recursos destino. Antivirus cloud queda como backlog, no incluido.
- What-if en Mexico Central terminó `Succeeded`: 44 creaciones previstas, sin Delete/Ignore/Unsupported. Se usaron canaries ficticios y el inventario de grupos continuó vacío. No prueba conexión, cuotas de capacidad al crear, login ni despliegue efectivo. Web, ServiceBus, KeyVault y DBforPostgreSQL terminaron registrados.

## Plan de integración de esta entrega

Backend: commits separados para Java/adaptadores/perfiles/seguridad/migraciones; worker Python; Bicep/scripts/costos; CI/CD y packaging. Frontend: runtime/build público; release/packaging/smoke. Ambas ramas son `feature/azure-students-deployment` y sus PR deben apuntar a **develop**, nunca directamente a main.

Tras aprobar los dos PR y sus checks, validar develop conjuntamente, preparar una nueva `release/<versión>` (propuesta: 0.2.0 por incorporar despliegue cloud), abrir PR a main, sincronizar develop y publicar nuevos tags coincidentes. No crear la release desde el feature ni mover v0.1.1. Solo después completar parámetros privados, provisionar con costo aprobado, configurar OIDC/RBAC y variables/fixtures, habilitar CD y ejecutar los workflows seleccionando el tag. La aceptación cloud incluye login, Sery streaming, PDF/embeddings, notificaciones y recuperación. Hasta esa evidencia, cloud no está aceptado.

Son verificaciones locales/de contrato, no una demostración de funcionamiento de Managed Identity, red, cuotas, Key Vault, Service Bus o Blob en cloud. Tras publicar los commits de implementación, CI y CodeQL terminaron en verde en ambos repositorios; consultar los checks de cada nuevo commit y PR, no reutilizar ese resultado para una revisión posterior.

La lectura adicional de las alertas de CodeQL, independiente del resultado de sus jobs, detectó CSRF desactivado en Java y generación de contraseñas con `Math.random()` en Angular. Se corrigieron con protección CSRF real y Web Crypto con muestreo sin sesgo/Fisher-Yates (16 caracteres y cuatro clases). No se descartaron ni suprimieron alertas. La regresión local posterior aprobó 85 pruebas unitarias Java, 6 integraciones y 55 Vitest; tipado de tests, build, configuración de despliegue e i18n aprobados. Verificar también el listado de alertas de la revisión nueva, no solo el estado verde del workflow. El gate admite `-MavenRepository` y usa las CLI Node directamente; `-DockerBackend` evita depender de los sockets del JDK de Windows.

## Checklist cloud pendiente (no ejecutado)

- Cuotas, SKU regional, registro de providers y RBAC; saldo/beneficios Students efectivos; alertas de presupuesto.
- Aprovisionar, probar Key Vault refs y Managed Identity sin keys de Storage/Service Bus.
- Login/logout/refresh, aislamiento por rol y recarga de deep links en navegadores relevantes.
- PDF válido → Blob → Service Bus → embeddings Blob → PG/vector → READY; corrupto → FAILED; duplicado/retry/DLQ controlado.
- Sery: stream normal, retraso >45 s, timeout terminal, desconexión/reconexión, sin respuesta truncada silenciosa.
- Notificaciones SSE, privacidad, logs sin keys/prompts ni texto completo de documentos; límites/consumo de telemetría.
- Carga real pequeña y recuperación tras reinicio de app/worker, filas/vector consulta y respaldos.
- Las protecciones PDF/prompt existentes se conservan; ClamAV no tiene servidor cloud en esta arquitectura. No afirmar antivirus ni pentest autenticado cloud completo sin desplegarlo/probarlo.

Documentación: [Service Bus límites](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-quotas), [claim-check](https://learn.microsoft.com/en-us/azure/architecture/patterns/claim-check), [pgvector](https://learn.microsoft.com/en-us/azure/postgresql/extensions/how-to-use-pgvector), [Key Vault refs](https://learn.microsoft.com/en-us/azure/app-service/app-service-key-vault-references), [SWA API 45 s](https://learn.microsoft.com/en-us/azure/static-web-apps/apis-overview), [OIDC](https://github.com/Azure/login).
