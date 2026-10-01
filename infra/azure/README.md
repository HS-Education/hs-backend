# Azure Students: implementación local y despliegue separado

Estado al 30/09/2026: **infraestructura aprovisionada en Azure for Students, Mexico Central; aplicación cloud todavía no desplegada ni validada**. El plan Linux B2, las dos Web Apps, PostgreSQL 16 B1ms, Blob privado, Service Bus Standard, Key Vault, observabilidad y presupuesto se crearon mediante Bicep. Las seis referencias de Key Vault están resueltas. OIDC usa una identidad administrada separada con seis asignaciones RBAC acotadas. Los environments de GitHub tienen destinos y secretos cifrados, y `AZURE_CD_ENABLED=true` está verificado en ambos repositorios por autorización del usuario; se conservan revisión humana, tags `v*`, sin autoaprobación ni bypass. No se iniciaron workflows de despliegue ni de smoke. Los recursos de pago ya consumen crédito. El presupuesto mensual de US$60 solo alerta al 80% y al 100%. No se cambiaron los contenedores, puertos, volúmenes ni datos locales.

Release 0.2.0: integrada a main en ambos repositorios, sincronizada hacia develop y publicada con los tags `v0.2.0`. Siguiente fase: ejecutar **Frontend Azure release** (`app-service`), después **Backend Azure CD** y finalmente **Azure browser smoke**, seleccionando `v0.2.0` como ref; los dos primeros también requieren ese valor en `release_tag`, el smoke no tiene ese input. El compañero inicia las ejecuciones y `sebaditas` aprueba. La corrección del aprovisionamiento OIDC afecta scripts operativos, documentación, pruebas y filtros de CI para ramas `fix/**`: no modifica el código de aplicación etiquetado ni mueve tags existentes. El CD debe completar Flyway y el empaquetado Java/Angular antes de aceptar login, documentos, colas, notificaciones o Sery en Azure. Los resultados del preflight y preparación de ramas documentados más abajo corresponden a fases anteriores.

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

## Aprovisionamiento futuro, tras integración GitFlow

Decisión inicial del 30/09/2026: se mantiene Linux B2 compartido y la arquitectura descrita arriba, con base estimada de US$55,79/mes en Mexico Central. No se aplica B1 ni apagado programado de tres horas. El flujo commits/ramas/release se completó antes del aprovisionamiento; posteriormente el usuario autorizó crear recursos y configurar OIDC. El budget de US$60 de la plantilla es una alerta, no un tope ni una garantía del costo final. Los costos variables y OpenRouter se mantienen separados; la estimación no equivale a una factura ni a un saldo de crédito verificado.

Copiar `parameters.example.json` a `parameters.local.json` (ignorado), completar valores fuertes distintos y proteger el archivo local. No compartirlo, no adjuntarlo a PR ni imprimirlo. API keys, passwords y JWT son secretos aunque coloquialmente se llamen llaves. Para repetir un despliegue reutilizar valores existentes; no rotar contraseñas/keys accidentalmente.

```powershell
./infra/azure/Provision-Students.ps1 -ParametersFile ./infra/azure/parameters.local.json -Mode WhatIf
# Solo después de revisión y aprobación de recursos pagos:
./infra/azure/Provision-Students.ps1 -ParametersFile ./infra/azure/parameters.local.json -Mode Create -ApprovePaidResources
```

What-if necesita sesión/RBAC/providers adecuados, pero no crea recursos por diseño. La plantilla es para un esquema cloud nuevo: migrar datos locales es una decisión separada. No tocar la BD de desarrollo ni restaurarla automáticamente. Si se necesita trasladar datos: respaldar PostgreSQL y MinIO, probar restore en una BD destino desechable, comprobar esquema/pgvector antes de establecer una baseline explícita, copiar objetos conservando keys, verificar checksums y recién después cambiar endpoints. Nunca activar baseline-on-migrate automáticamente sobre una BD poblada.

## CD automático por tags: siguiente release

La implementación nueva añade `push.tags: ['v*']` a **Frontend Azure release** y **Backend Azure CD**, conservando `workflow_dispatch` para recuperación manual. Se debe integrar primero esta feature a develop y preparar una nueva release coordinada en ambos repositorios. Los tags `v0.2.1` ya publicados no se modifican ni disparan retroactivamente este código.

1. Tras los PR/CI/review/release/main y sincronización hacia develop, el compañero publica el mismo tag nuevo en ambos repos, preferiblemente frontend primero. Publicar una rama o crear el tag solo localmente no inicia CD.
2. El frontend usa `app-service` automáticamente y prepara los assets; mantiene el environment protegido. El usuario revisa y aprueba esa ejecución.
3. El backend verifica su tag/main y sus tests. Espera hasta 30 minutos por **ambos** assets del frontend, incluso si los tags llegan en orden inverso. Si faltan o expira el plazo, falla sin desplegar: revisar tag, aprobación y build del front antes de reintentar.
4. Verifica el SHA/tag/main, checksum, same-origin y presencia del guard de smoke en el frontend antes de empaquetar. El job `deploy` depende de `verify` y sigue requiriendo aprobación de `azure-students` antes de obtener OIDC o ejecutar Flyway/Web App deploy.
5. Después de `deploy` exitoso, el job `browser-smoke` del **mismo workflow backend** obtiene el frontend etiquetado, comprueba su SHA contra el manifiesto empaquetado y ejecuta los tres escenarios existentes. Usa `contents: read`, sin OIDC, y conserva el environment protegido; si GitHub solicita otra revisión del environment, aprobarla explícitamente. Si el deploy falla, el smoke se omite; si el smoke falla, CD queda fallido y no se declara aceptación cloud ni se hace rollback automático.

Mantener `AZURE_CD_ENABLED=true` en ambos repos, revisión de `sebaditas`, `prevent_self_review=true`, sin bypass y solo tags `v*`. El **compañero** debe publicar los nuevos tags: si `sebaditas` publica el tag y es el único revisor, la autoaprobación sigue bloqueada. No quitar esa protección para desbloquear la ejecución. El mismo tag debe contener ambos cambios y no debe moverse después.

Para el smoke automático, el environment **del backend** necesita `SMOKE_USERNAME` y `SMOKE_PASSWORD` de una cuenta autorizada, además de su `AZURE_BACKEND_URL` existente. El CD comprueba que existen antes de las migraciones; no basta con tenerlos solo en el environment del frontend. Configurarlos cifrados mediante GitHub Secrets después de autorizar explícitamente ese destino, nunca como variables públicas, archivos versionados o valores de log. No se amplían roles Azure ni se añade PAT de escritura entre repos. Los repos públicos permiten leer el paquete frontend con el token estándar del backend.

El workflow independiente **Azure browser smoke** del frontend permanece manual para diagnóstico; no se dispara con tags porque podría correr antes del backend. SWA sigue siendo opcional y solo se permite por selección manual explícita, nunca por publicación de tag. Los workflows manuales seleccionan el tag como ref; `release_tag` debe coincidir para release/CD. Reintentar no permite sobrescribir assets (`--clobber`) ni tags.

Las esperas son acotadas y pueden consumir minutos de runner mientras se espera el frontend. Aprobar su ejecución pronto; si vence el plazo, reintentar el backend sobre el mismo tag cuando el paquete ya esté listo. La configuración no crea recursos ni inicia despliegues. La aceptación de documentos, Sery, notificaciones y recuperación sigue pendiente de pruebas funcionales cloud.

## GitFlow y CD inicial (histórico/manual)

1. Ambas ramas `feature/azure-students-deployment` → PR a `develop`, CI de tests/seguridad y revisión humana.
2. Validar conjuntamente; abrir nueva `release/<versión>` desde develop, PR a main en ambos repos. No reusar ni mover `v0.1.1`: ese tag no incluye esta implementación.
3. Después del merge de release a main, abrir PR de sincronización main → develop en ambos repos, para conservar también el commit de merge de main. Publicar el mismo nuevo tag semántico sobre el commit de release integrado a main en cada repo; no mover tags existentes.
4. Revisar el GitHub Environment `azure-students` ya preparado, sus revisores y su restricción a tags. En la fase posterior: validar parámetros privados/Students/capacidad/what-if, crear recursos mediante CLI+Bicep y configurar OIDC/RBAC/variables/secretos/fixtures. Activar `AZURE_CD_ENABLED=true` solo tras esas comprobaciones.
5. Ejecutar **Frontend Azure release** con el tag y modo app-service: tests/build → asset `frontend.zip` + manifiesto SHA256 en GitHub Release. No modifica la API.
6. Ejecutar **Backend Azure CD** con el mismo tag: verifica que ambos commits están en main, comprueba tag/SHA256 del frontend, ejecuta Java/Python/integración y empaqueta Angular dentro del JAR y el worker mediante allowlist.
7. Aprobación del environment → login OIDC → guard Students → Flyway con dueño DB y firewall temporal → worker → Java/Angular → health/deep-link/runtime checks.
8. Ejecutar **Azure browser smoke** en front para login, cookies Secure/HttpOnly y recarga de ruta. Antes de declarar cloud listo, completar el checklist siguiente.

Los workflows manuales deben ejecutarse **seleccionando el tag como ref**, no main ni una feature; el input `release_tag` de release/CD debe coincidir con esa ref. Los environments preparados con `Configure-GitHubEnvironment.ps1` exigen revisión de `sebaditas`, sin bypass administrativo y sin autoaprobación: el compañero inicia el workflow y `sebaditas` aprueba. El gate solo se habilita tras verificar infraestructura, OIDC, variables, secretos y credenciales autorizadas; habilitarlo no inicia un despliegue. No crear/mover un tag antiguo para hacer visible un workflow nuevo: primero integrarlo a main mediante PR.

### Primer despliegue manual de v0.2.0

La infraestructura y el gate ya están preparados. El compañero inicia cada ejecución desde su propia cuenta con acceso al repo; `sebaditas` aprueba el environment `azure-students` en GitHub cuando aparezca **Review deployments**. No ejecutar las tres etapas en paralelo: esperar el resultado satisfactorio de la anterior. El verify del backend corre antes de solicitar aprobación para el job deploy.

| Orden | Repositorio / workflow | Ref e inputs | Resultado |
| --- | --- | --- | --- |
| 1 | hs-front / Frontend Azure release | ref `v0.2.0`, `release_tag=v0.2.0`, `hosting=app-service` | GitHub Release con `frontend.zip` y manifiesto SHA256; no despliega sobre la API |
| 2 | hs-backend / Backend Azure CD | ref `v0.2.0`, `release_tag=v0.2.0` | Verificación, OIDC, Flyway, worker, Java/Angular y health checks |
| 3 | hs-front / Azure browser smoke | ref `v0.2.0`, sin inputs adicionales | Tres escenarios de navegación anónima, perfil protegido y login/cookies/recarga/logout |

Si GitHub no ofrece el tag en el selector de la interfaz, el compañero puede fijarlo explícitamente con GitHub CLI autenticado en su cuenta. Ejecutar cada comando solo cuando la etapa anterior haya pasado:

```powershell
gh auth status
gh workflow run azure-release.yml --repo HS-Education/hs-front --ref v0.2.0 -f release_tag=v0.2.0 -f hosting=app-service
```

```powershell
gh workflow run azure-cd.yml --repo HS-Education/hs-backend --ref v0.2.0 -f release_tag=v0.2.0
```

```powershell
gh workflow run azure-smoke.yml --repo HS-Education/hs-front --ref v0.2.0
```

[`gh workflow run --ref`](https://cli.github.com/manual/gh_workflow_run) admite una rama o un tag. No añadir `release_tag` al smoke: ese workflow no define inputs. No sobrescribir assets ni recrear tags para reintentos. Una ejecución fallida debe diagnosticarse antes de continuar a la siguiente etapa.

La corrección operativa sigue el flujo `fix/azure-cd-managed-identity` → PR a `develop` → CI/revisión → futura release → `main` → sincronización hacia `develop` → nuevo tag si se publica esa release. No se integra directamente a una rama protegida. El primer CD de `v0.2.0` puede usar la identidad ya aprovisionada: sus workflows no llaman al script de configuración OIDC y el código de aplicación no cambió.

El smoke usa el administrador inicial autorizado y no sustituye la aceptación de documentos, colas, Sery, notificaciones o recuperación. Esos flujos requieren cuentas/roles y datos de prueba preparados deliberadamente en cloud; no importar automáticamente la base local ni usar la cuenta admin para probar funciones de docente/coordinador.

### Handoff de los PR de implementación (integrados antes de la release)

Las ramas publicadas se llaman `feature/azure-students-deployment` en ambos repositorios. El autor/compañero crea manualmente los dos PR con **base `develop`**, usando un título en inglés como `feat(azure): prepare Students deployment and gated CD`:

- Backend: https://github.com/HS-Education/hs-backend/compare/develop...feature/azure-students-deployment?expand=1
- Frontend: https://github.com/HS-Education/hs-front/compare/develop...feature/azure-students-deployment?expand=1

Revisar el SHA actual y los checks del propio PR, no reutilizar el verde de un push anterior. Mantener los commits de implementación por responsabilidad. Aprobar/fusionar los dos PR y actualizar ambas copias locales con `git fetch origin`, `git switch develop`, `git pull --ff-only origin develop` (solo con árbol limpio). No mezclar frontend antiguo con el nuevo contrato CSRF del backend.

Tras integrar esas features se seleccionó **0.2.0** y se prepararon ambas ramas desde develop, actualizando la versión de cada paquete y sus notas en un commit `chore(release): prepare 0.2.0`. El main vigente se incorporó por merge, sin rebase/force-push ni cambio directo en main. El usuario/compañero abre los PR release → main; esperar checks y aprobación antes de continuar con sincronización/tags y la fase Azure. Si main cambia después de preparar la release, actualizar de nuevo la rama release y esperar los checks del nuevo SHA.

Basic B2 no tiene deployment slots: puede haber interrupción breve, no se promete blue/green. Mantener el artefacto anterior; rollback de app solo si es compatible con el esquema vigente. No deshacer la BD con scripts destructivos. Para cambios incompatibles aplicar expand/contract y respaldo/restore probado.

## OIDC y variables de GitHub

Después de existir los recursos, `Configure-GitHubOidc.ps1 -BackendApp <nombre> -WorkerApp <nombre> -PostgresServer <nombre> -KeyVault <nombre> -ApproveIdentityChanges` crea/reutiliza la identidad administrada de usuario `hs-thesis-github-cd` mediante `github-oidc.bicep`, sin client secret, federada exclusivamente a `repo:HS-Education@334800057/hs-backend@1170255521:environment:azure-students`. Los IDs corresponden al propietario y repositorio actuales; deben coincidir exactamente, incluidas mayúsculas, con el `subject` del token GitHub. Este repositorio usa el formato de [claims inmutables de GitHub](https://docs.github.com/en/actions/reference/security/oidc#immutable-subject-claims), no el formato anterior basado solo en nombres. Se administra por ARM/RBAC en la suscripción Students; no necesita listar ni crear aplicaciones del directorio mediante Microsoft Graph. No requiere VM ni runner propio: `azure/login` conserva el intercambio OIDC con `client-id`, `tenant-id` y `subscription-id`. Antes de actualizar, rechaza credenciales federadas preexistentes ajenas o incompatibles. Reader en Students para validar oferta, Website Contributor solo en las dos apps, rol limitado a firewall PostgreSQL y Secrets User solo en las dos credenciales de migración. No dar Owner/Contributor de toda la suscripción al CD. [Referencia Microsoft](https://learn.microsoft.com/en-us/entra/workload-id/workload-identity-federation-create-trust-user-assigned-managed-identity).

### Recuperación de AADSTS700213 por el subject anterior

Si la identidad ya existe y la única discrepancia verificada es el formato anterior del subject, el script general rechaza esa confianza: no crea una segunda federación ni amplía permisos para repararla. Revisar primero la suscripción Students habilitada con spending limit On, la identidad/tenant, la única federación `hs-backend-azure-students`, su issuer/audience y el subject real en el job fallido. Verificar también los IDs contra los metadatos actuales del repositorio. Con autorización operativa explícita, actualizar **solo** ese subject mediante [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/identity/federated-credential#az-identity-federated-credential-update):

```powershell
az identity federated-credential update `
  --subscription 86d9e5e6-b9bf-44b4-915a-106207e0bc02 `
  --resource-group rg-hs-thesis-azure `
  --identity-name hs-thesis-github-cd `
  --name hs-backend-azure-students `
  --issuer 'https://token.actions.githubusercontent.com' `
  --subject 'repo:HS-Education@334800057/hs-backend@1170255521:environment:azure-students' `
  --audiences 'api://AzureADTokenExchange' `
  --only-show-errors --output none
```

Leer nuevamente la federación y comprobar subject, issuer, audience, client ID y asignaciones RBAC sin cambios adicionales. No ejecutar de nuevo el setup completo, cambiar secretos, desactivar las claims inmutables ni relajar las aprobaciones/tags del environment. La lectura confirma configuración, **no** demuestra un intercambio OIDC exitoso. El compañero que inicia el CD puede usar **Re-run failed jobs** en el run existente; el revisor autoriza cuando GitHub lo solicite. Para este fallo previo a migraciones/despliegue se conserva `v0.2.2`: no mover el tag ni volver a publicar el artefacto frontend ya exitoso. Integrar también esta corrección de IaC mediante PR a `develop` para evitar que futuros setups restauren el subject anterior.

Backend environment secrets: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`. Variables: `AZURE_RESOURCE_GROUP`, `AZURE_BACKEND_APP`, `AZURE_WORKER_APP`, `AZURE_POSTGRES_SERVER`, `AZURE_KEY_VAULT`, `AZURE_BACKEND_URL`, `AZURE_WORKER_URL`. Front: `AZURE_FRONTEND_URL`, `AZURE_API_BASE_URL` (URL cloud absoluta terminada en `/api/v1` para smoke), y secrets `SMOKE_USERNAME`/`SMOKE_PASSWORD` de cuenta de prueba autorizada. Los nombres/URLs salen de los outputs públicos de Bicep. App Service usa `/api/v1` relativo en el bundle.

Si front es privado, `FRONTEND_RELEASE_READ_TOKEN` de lectura mínima de contents para ese repo debe permitir download y clone; no incluir token en URLs/logs. SWA adicional requiere token de deployment propio y federación con el subject real del repositorio frontend para `azure-students`, incluidos los IDs inmutables cuando corresponda, con lectura del target Students; es configuración futura, no validada ahora.

## Migraciones y compatibilidad de Azure CLI

### Esperar la aplicación real después del despliegue

En `v0.2.4`, el chequeo llegó mientras Azure servía una página HTML temporal con HTTP 200, incluso en `/actuator/health` y `/runtime-config.json`. Eso no demuestra disponibilidad de Spring Boot. Más tarde el mismo despliegue devolvió salud `UP`, configuración `/api/v1` y el login Angular; no fue necesario cambiar URLs, secretos ni deshacer migraciones.

`Test-CloudHealth.ps1` comprueba las cuatro respuestas en un mismo ciclo: JSON de salud Java con `status: UP`, JSON del worker con `status: ok`, HTML del login con el root y scripts Angular, y JSON público con **solo** `apiBaseUrl: /api/v1`. Rechaza redirects, páginas temporales, estados no saludables, JSON inválido y configuración distinta. Acepta el media type JSON del Actuator y decodifica contenido de bytes. Usa 30 intentos como máximo y un presupuesto compartido de 300 segundos; los requests y pausas respetan el tiempo restante. No imprime cuerpos ni mensajes de excepciones HTTP.

Las pruebas de `scripts/tests/test_cloud_readiness.py` ejecutan el código del script con HTTP y reloj simulados, sin llamar a Azure: arranque temporal, errores transitorios, contrato JSON/HTML, límites y destinos inválidos. El harness sustituye únicamente la construcción del cronómetro, comprobando que aparece una sola vez; el script de producción conserva `Stopwatch` y sus límites originales. Esto evita que la carga inicial de módulos PowerShell en el runner consuma el segundo de presupuesto ficticio del test antes de llegar al endpoint esperado. Los dos casos de límite se repiten tres veces sin pausas reales. CI y CD las descubren automáticamente. Un chequeo HTTP válido no sustituye el smoke de login/cookies ni la aceptación funcional de documentos, Sery y notificaciones. La corrección debe integrarse mediante PR y nuevo tag; no mover `v0.2.4` ni cambiar las aprobaciones del environment.

### Salida CLI y autenticación de las acciones de despliegue

El run de `v0.2.3` superó OIDC y migraciones, pero `Azure/webapps-deploy@v3` falló al inicializar su autenticación: su authorizer interpreta como JSON la salida de `az account show` y `az cloud show` sin especificar `--output`. El valor global `AZURE_CORE_OUTPUT: none` suprime esa salida y provoca el mensaje genérico "No credentials found" aunque exista sesión. El [bundle de la acción](https://github.com/Azure/webapps-deploy/blob/v3/dist/index.js) contiene ese contrato; las advertencias de Node no son la causa de este fallo.

Conservar `none` como valor del job para los scripts y establecer `AZURE_CORE_OUTPUT: json` **solo** en el `env` de las dos acciones de despliegue Python/Java. La acción captura esas respuestas internamente; no imprimir tokens, cuentas completas ni credenciales de publicación como diagnóstico. Mantener OIDC, los permisos acotados y los pasos de enmascaramiento existentes. Las pruebas de `test_tag_driven_cd.py` comprueban los dos overrides y el contrato de lectura JSON con fixtures offline; no prueban un despliegue real.

Este cambio requiere una nueva release/tag integrado en main; otro rerun de `v0.2.3` conserva el workflow anterior. No mover tags existentes ni deshacer las migraciones que ya terminaron correctamente. Crear PRs y esperar checks/revisión independiente antes de fusionar. La sesión actual y el actor de las ejecuciones siguen sujetos a las restricciones de autoaprobación; no cambiar reglas, usar bypass ni suplantar otra cuenta para continuar.

`Migrate-Database.ps1` consulta la ayuda local de `firewall-rule create` antes de abrir acceso o leer credenciales. Admite el contrato actual (`--server-name` para el servidor, `--name` para la regla) y el anterior (`--name` para el servidor, `--rule-name` para la regla); una sintaxis desconocida bloquea el proceso sin mutaciones. Ver [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/postgres/flexible-server/firewall-rule). No usar un cambio de versión a ciegas como sustituto de comprobar el contrato.

La regla permite una sola IPv4 del runner: inicio y fin iguales; se rechazan IPv6 y `0.0.0.0`. La limpieza usa `delete --ids` con el ID completo de la regla `cd-<GUID>` de esa ejecución, sin tocar reglas de las apps ni ampliar RBAC. Incluso ante un fallo de creación o migración, se intenta limpiar y se consulta el inventario filtrado por ese nombre. Solo un resultado JSON de array vacío con consulta exitosa demuestra ausencia. Si no se puede confirmarla, CD falla y muestra el ID exacto para revisión; si también hubo fallo de migración, conserva ambos errores. Las variables de credenciales de migración se borran del proceso en `finally`.

Las 18 pruebas de regresión de `scripts/tests/test_migration_firewall_cli.py` ejecutan el script real con CLI/Java/Key Vault/IP simulados: ambas sintaxis, fallos de creación/respuesta/credenciales/migración, excepciones, limpieza fallida y respuestas de inventario inválidas. Se incluyen automáticamente en los jobs existentes de CI y verificación CD. No demuestran una migración real en Azure.

El reintento de `v0.2.2` superó OIDC, pero falló antes de ejecutar Flyway por la sintaxis del script etiquetado; no quedaron reglas `cd-*` en la revisión de Azure. A diferencia de corregir la confianza externa OIDC, este arreglo cambia código que el CD obtiene del tag. Integrar `fix/azure-migration-firewall-cli` mediante PR a `develop`, comprobar CI y preparar la siguiente release con PR a `main` y sincronización hacia `develop`. Publicar un **nuevo tag coincidente en ambos repositorios**: el backend espera el paquete/manifiesto frontend con el mismo tag, aunque Angular no requiera un cambio funcional. Conservar `v0.2.2` intacto; otro rerun de ese tag no incorpora este script. Mantener las aprobaciones del environment antes de migrar/desplegar y comprobar salud y smoke tras el despliegue.

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

El gate con Java en Docker finalizó satisfactoriamente: **17 escenarios Playwright aprobados** contra servicios/base desechables; carga local autenticada de lectura con 10 usuarios y 200 solicitudes, 0 errores, p95 29,1 ms. Los servicios y datos desechables se retiraron al finalizar; la infraestructura/datos de desarrollo se conservaron. No es una medición de capacidad de Azure, carga representativa de producción ni prueba de OpenRouter real. El primer intento con Java nativo de Windows falló al crear el socket loopback del JDK, antes de poder ejecutar el smoke; no se atribuyó a la aplicación. Los audits de dependencias, ClamAV y DAST no se repitieron en este gate (`-SkipDependencyAudit`, sin opt-in de malware/DAST); no declararlos aprobados en esta ejecución.

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
