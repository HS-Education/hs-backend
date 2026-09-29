# Plan y ejecución de calidad local

Esta matriz describe qué se ejecuta, con qué datos y qué queda sin demostrar. Un test verde no significa cobertura total ni inmunidad frente a ataques. La llamada real a OpenRouter queda fuera del gate obligatorio; no se agrega HNSW y se mantiene la búsqueda por coseno `<=>`.

## Preparación y comandos

- Requisitos: Java 21, Maven, Docker Desktop con motor Linux operativo, Node/pnpm, Chromium de Playwright y `ai-service/.venv` con `requirements.txt` y `pip-audit` instalados. La auditoría consulta fuentes externas; requiere red y usa una imagen OSV Scanner fijada por digest.
- En `hs-tesis-front`: `pnpm install --frozen-lockfile`.
- En `hs-tesis`: `./scripts/quality-gate.ps1`. Requiere libres los puertos 4200, 8080, 15432, 15673 y 19000. Crea un proyecto Docker desechable con PostgreSQL/pgvector, RabbitMQ y MinIO y genera credenciales aleatorias en un directorio temporal.
- Si el proceso de Windows no permite a Java abrir el selector NIO (error `Unable to establish loopback connection`), ejecutar `./scripts/quality-gate.ps1 -DockerBackend`. Usa la imagen Linux `eclipse-temurin:21-jre` para el backend, mantiene los servicios/credenciales aislados y expone 8080 solo en loopback IPv4 e IPv6. Se puede combinar con `-RunLoad`.
- Sin navegador: `./scripts/quality-gate.ps1 -SkipSmoke`. No omite integración PostgreSQL; para unidad sin Docker ejecutar los comandos individuales de abajo.
- Si no hay red para la auditoría: `-SkipDependencyAudit` la omite de forma explícita; esa ejecución **no** cuenta como gate de seguridad completo.
- Perfil acotado de carga tras el smoke: `./scripts/quality-gate.ps1 -RunLoad`. Nunca apunta a hosts externos ni a producción.
- Java unidad: `mvn test`. Java unidad + integración descartable: `mvn -Plocal-gate verify`.
- Worker IA, desde `ai-service`: `.\.venv\Scripts\python.exe -m unittest discover -s tests -p 'test_*.py' -v`.
- Angular, desde `hs-tesis-front`: `pnpm test`, `node scripts/check-i18n.cjs`, `pnpm build`.
- Navegador dentro del entorno aislado: `pnpm exec playwright test` (el gate instala Chromium y aporta credenciales/servidores).
- OpenRouter opcional y con costo: desde `ai-service`, ejecutar `\.venv\Scripts\python.exe tests\live_prompt_injection.py --review-output $env:TEMP\prompt-review.jsonl` (hasta 16 llamadas) y `\.venv\Scripts\python.exe tests\live_worker_routes.py --review-output $env:TEMP\worker-review.jsonl` (3 llamadas a las rutas reales). Los archivos de revisión deben ser nuevos y contener solo datos sintéticos; revisar las salidas manualmente. No forman parte del gate obligatorio.
- Auditoría de dependencias (incluida por defecto en el gate): OSV Scanner sobre `pom.xml`, `pip-audit -r ai-service/requirements.txt` y `pnpm audit --prod --audit-level high` desde el frontend. Un hallazgo o fallo de consulta deja el gate rojo; repetir la auditoría antes de cada release.

## Matriz trazable

| ID | Módulo / ruta / rol | Dato o amenaza | Aserción | Automatización |
| --- | --- | --- | --- | --- |
| IAM-01 | Login `/sign-in`, usuario semilla | Contraseña inválida/válida | 401/200; JWT HttpOnly, SameSite Strict; `/auth/me` | `login.smoke.spec.ts` |
| IAM-02 | `/home`, `/classrooms`, `/repository`, `/metrics`, `/admin/*`; anónimo/estudiante/admin | Acceso por URL | Redirección por rol y 403 servidor en `/users` | `critical-routes.spec.ts`, `route-guards.spec.ts` |
| IAM-03 | `/admin/users`; admin | Alta de estudiante vía UI | POST 200 y usuario visible en GET | `critical-routes.spec.ts` |
| IAM-04 | Roles; admin | Admin no delegable, último admin, rol estudiante incompatible, coordinador con área | Rechazos sin mutación indebida | `UserCommandServiceImplTest` |
| IAM-05 | Interceptor Angular | Host externo que imita `/api/v1` | Sin cookie ni refresh; refresh local una sola vez | `auth-interceptor.spec.ts` |
| IAM-06 | `/auth/sign-in`; misma cuenta en paralelo | Cuatro inicios de sesión simultáneos | Todos responden 200; reemplazo del refresh token serializado por fila de usuario | `critical-routes.spec.ts` |
| IAM-07 | Navegación Angular, visitante anónimo | 13 rutas protegidas, incluidas rutas administrativas y detalle/cuestionario | Redirección a inicio de sesión | `authorization-security.spec.ts` |
| IAM-08 | API de cuestionarios, docente/estudiante | Generar remedial sin rol, generar evaluación como estudiante, leer instancia ajena | 403/404 sin diagnóstico interno | `authorization-security.spec.ts` |
| AULA-01 | Generación/eliminación; admin | Año no planificado, sin plan de estudios, repetición | Rechazo, idempotencia, orden al borrar matrículas | `ClassroomCommandServiceImplTest` |
| AULA-02 | `/classrooms?userId=...`; estudiante | ID de otro usuario; parámetro `1 OR 1=1` | 403/400, sin detalle SQL | `critical-routes.spec.ts` |
| EVAL-01 | Cuestionarios; docente/coordinador/estudiante | Curso no asignado, alumno no matriculado, instancia ajena | 403 antes de generar, corregir o notificar | `QuestionnaireCommandServiceImplTest` |
| EVAL-02 | Entrega de cuestionario; estudiante | Opción negativa/fuera de rango o ID de pregunta inventado | 400 antes de calificar o guardar | `QuestionnaireCommandServiceImplTest` |
| DOC-01 | Carga PDF; coordinador | MIME/archivo inválido, corrupto, cifrado, activo, >50 MiB | 400/413 antes de MinIO; ningún envío parcial por archivo inválido del lote | `PdfUploadValidatorTest`, `DocumentCommandServiceImplTest` |
| DOC-02 | Procesamiento de PDF y RabbitMQ | PDF roto o sin texto; error de broker; respuesta tardía | Evento de fallo saneado, estado `FAILED`, redelivery no resucita documento | `test_worker_security.py`, `DocumentLifecycleTest`, `DocumentCommandServiceImplTest` |
| DOC-03 | `/courses/{id}/documents/bulk` | JSON de metadata mal formado con dato sensible | 400 sin eco del cuerpo | `DocumentControllerTest`, `GlobalExceptionHandlerTest` |
| DOC-04 | Carga PDF, coordinador | MIME PDF con bytes truncados/corruptos | 400 sin eco y sin documento nuevo | `academic-security.spec.ts` |
| DOC-05 | ClamAV de prueba | PDF limpio, EICAR exacto, scanner indisponible | Limpio pasa, EICAR 400, caída 503; upload falla cerrado | `ClamAvMalwareScannerIT` (`-RunMalware`) |
| DOC-06 | Worker → RabbitMQ → listener → PostgreSQL | PDF corrupto ya almacenado y en `PROCESSING` | Evento `DOCUMENT_INVALID` con datos mínimos; consumo real del broker y estado persistido `FAILED` | `test_worker_security.py`, `DocumentProcessingFailureFlowIT` |
| REPO-01 | PostgreSQL/pgvector | Rollback, destino curso/grado ajeno, SQLi como parámetro, estado no READY | Transacción revierte y la recuperación filtra/bindea | `DocumentChunkRepositoryIT` (Docker) |
| SQL-01 | `/classrooms`, `/courses`; administrador | Filtros numéricos con `1 OR 1=1` | 400 sin SQLSTATE/stack trace | `authorization-security.spec.ts` |
| IA-01 | `/generate`, cuestionarios y feedback | `system`/`developer` falsos, inyección en PDF/contexto/historial/Unicode, exfiltración y prompt extraction | Datos nunca pasan a rol privilegiado; respuesta no sigue ataques de esta muestra; salidas estructuradas se validan | Unitarios Python + 16 sondas OpenRouter opt-in + 3 rutas reales |
| IA-02 | Embeddings OpenRouter simulados | Índices perdidos, dimensión errónea, NaN | Rechazo antes de persistencia | `test_worker_security.py`, `test_openrouter_contract.py` |
| IA-03 | Worker concurrente | Quinta solicitud simultánea | 503 con `Retry-After`; sin cola ilimitada | `test_worker_security.py` |
| IA-04 | Streaming worker → Java → persistencia de chat | Fragmento con saltos de línea, falso `data: [DONE]`, EOF o truncamiento del proveedor | El texto no termina el stream; solo `[DONE]` confirma término normal; EOF/truncamiento falla y el chat no persiste la respuesta parcial | `test_worker_security.py`, `AiServiceClientTest`, `ChatServiceImplTest` |
| UI-01 | Rutas de estudiante, docente y coordinador | Navegación, logout y persistencia de inglés | Rutas esperadas, sesión revocada e idioma conservado | `critical-routes.spec.ts` |
| UI-02 | Markdown/matemática y estadísticas | HTML malicioso, cálculo académico | Sanitización y resultados numéricos esperados | `markdown-math.pipe.spec.ts`, `performance-statistics.spec.ts` |
| PERF-01 | `/auth/me`, `/notifications/unread` | 10 usuarios, 200 peticiones autenticadas | p95 ≤ 2000 ms; errores ≤ 1 % | `scripts/load-probe.mjs` con `-RunLoad`, optativo |
| PERF-02 | Lecturas autenticadas y login | 50 VU de lectura + hasta 10 VU login por 45 s | p95 < 2 s, fallos < 1 %, checks > 99 %; login con 4 roles desechables y cookie HttpOnly | `scripts/k6-capacity.js` con `-RunCapacity`, optativo |
| DEP-01 | Frontend, backend y worker | Dependencias vulnerables | Cero avisos conocidos en Maven/Python y cero avisos de nivel alto o crítico en frontend productivo | OSV Scanner sobre `pom.xml`, `pip-audit -r requirements.txt`, `pnpm audit --prod --audit-level high` |

Los tests de navegador usan usuarios semilla con contraseñas aleatorias y una base nueva en cada gate. IAM-03 crea un estudiante adicional solo allí. Los tests de unidad usan dobles aislados; REPO-01 usa un contenedor PostgreSQL descartable con `db/init/01_init.sql`.

## Umbrales y mediciones

- El build Angular verifica el presupuesto de error de 1,5 MB ya acordado. Tras actualizar Angular 21 dentro de su versión mayor, el bundle inicial midió **934,30 kB sin comprimir** y **217,39 kB de transferencia estimada**; permanece la advertencia frente al presupuesto orientativo de 500 kB. No equivale a una medición de navegador real.
- La carga es pequeña y local. En la última ejecución del 29/09/2026, `-RunLoad` completó 200 solicitudes autenticadas con 10 usuarios, 0 errores, p95 de **16,8 ms** y 899,1 solicitudes/s. `-RunCapacity` completó 4.178 solicitudes HTTP durante 45 s con hasta 50 lectores y 10 logins concurrentes (58 VU activos observados), 0 fallos, p95 de **8,54 ms** y 4.336 checks correctos. p95 ≤ 2.000 ms y errores ≤ 1 % siguen siendo umbrales provisionales; estas cifras no representan tráfico de producción, cargas de PDF/IA ni rendimiento visual.
- Backend limita pool JDBC a 20 conexiones, hilos HTTP a 120, espera de conexión a 5 s, cliente IA a 45 s y consumidor RabbitMQ a 2–4. El worker limita concurrencia de proveedor a 4 y tamaño de PDF/texto. Son límites de contención, no resultados de capacidad.
- JUnit de Playwright va al temporal del gate; logs se conservan si falla. Maven genera reportes en `target/surefire-reports` y `target/failsafe-reports`. Un reporte antiguo no cuenta como ejecución reciente.

## Seguridad y límites conocidos

- Java valida nombre/MIME, bytes, estructura PDF, páginas y contenido activo. ClamAV opcional analiza los bytes exactos previos al almacenamiento; EICAR y caída fail-closed pasan localmente. El worker rechaza PDF corrupto/cifrado, sin texto y fuera de límites. Esto no demuestra detección de todas las amenazas, ni hay cuarentena/operación administrada de reintento. La integración está desactivada por defecto y no se debe desplegar el transporte TCP claro actual en Azure.
- Documentos y entradas del usuario se envían como datos no confiables, no como system prompt. El 29/09 se revisaron manualmente 16 sondas sintéticas directas y 3 respuestas de rutas reales del modelo configurado: no se observó obediencia al marcador ni divulgación de los fragmentos buscados. Es una muestra finita y no demuestra inmunidad a prompt injection; hace falta ampliar el corpus y una evaluación independiente.
- La prueba de SQLi verifica parámetros bindeados y rutas concretas, no todos los endpoints. PostgreSQL ejercita SQL crítico, no todo el repositorio JPA en un contexto Spring completo.
- Playwright ejecutó 15 escenarios: login, rutas/roles, logout, idioma, alta de usuario/área/curso, autorización de cuestionarios, SQLi de filtros y rechazo de PDF corrupto. No recorre todavía todos los formularios/estados de aulas, cuestionarios, notas, repositorio, métricas y notificaciones.
- El esquema actual admite un solo refresh token vigente por usuario. Los inicios de sesión concurrentes ya no deben producir 500, pero el más reciente invalida el refresh token de la sesión anterior; la política de múltiples dispositivos requiere una decisión de producto y otro modelo de datos.
- Tras actualizar Angular y fijar `js-yaml` 4.3.2, `pnpm audit --prod --audit-level high` no encontró avisos conocidos. Tras actualizar `pydantic-settings` a 2.14.2, `pip-audit -r requirements.txt` tampoco encontró avisos conocidos. OSV Scanner detectó inicialmente 103 avisos en 28 paquetes Maven; se actualizaron Spring Boot 4.0.8, Spring AI 2.0.1, Springdoc 3.1.1, RabbitMQ, PostgreSQL, Commons Lang y los parches transitivos de Tomcat, Jackson y Bouncy Castle. El reescaneo final de `pom.xml` devolvió `No issues found`. Son resultados de las bases de avisos consultadas el 29/09/2026, no garantías futuras. Faltan pentest y SAST/DAST. OWASP Dependency-Check no pudo descargar NVD en este Windows (`Unable to establish loopback connection`); OSV aporta una auditoría alternativa, no equivale a certificar la seguridad del sistema.

## Estado y GitFlow

La rama `feature/comprehensive-quality-and-security` existe en ambos repositorios y esta ejecución se hizo sobre ella. Las modificaciones de este turno siguen solo en el working tree: sin commit, push ni PR. Los workflows CodeQL (`security-analysis.yml`) están creados localmente pero requieren commit/push para ejecutar el análisis en GitHub; no afirmar que están verdes ni configurarlos como check obligatorio todavía. La API pública mostró cero rulesets visibles; la protección clásica no está verificada sin acceso autenticado. CD, Azure y la promoción a `release/*`/`main` siguen para la fase posterior.

### Protección pendiente en GitHub

En **cada repositorio**, revisar primero `Settings → Branches` y `Settings → Rules → Rulesets` para no duplicar una protección clásica existente. Crear reglas **a nivel del repositorio** (no de la organización) para `develop` y `main`, con enforcement activo, sin borrado ni force-push, PR obligatorio, una aprobación de alguien distinto al autor y checks exitosos antes del merge. No exigir despliegues hasta implementar CD. Para `hs-backend`, fijar como required checks `Backend / Java + PostgreSQL` y `Backend / AI offline contract`; para `hs-front`, `Frontend / Unit + build + i18n`. Confirmar la configuración con un PR de prueba antes de considerar la protección operativa. Si solo se habilita en `develop`, repetirla para `main` antes del primer release.

El 29/09/2026 pasó el gate local completo con `./scripts/quality-gate.ps1 -DockerBackend -RunLoad -RunCapacity -RunMalware -RunDast` (código 0, auditorías incluidas): 45 Java unitarias, 5 integraciones PostgreSQL/pgvector/RabbitMQ, 2 ClamAV, 32 Python, 18 Vitest y 15 Playwright; build/i18n, auditorías de dependencias, carga, capacidad y ZAP pasivo también pasaron. ZAP informó 56 PASS, 11 WARN y 0 FAIL; las advertencias son no bloqueantes en el baseline y requieren triage con configuración de preproducción. Las pruebas OpenRouter reales fueron opt-in y separadas del gate. El gate local verde no demuestra inmunidad, capacidad de Azure, protección de GitHub ni cobertura total.
