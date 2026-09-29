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
- OpenRouter opcional y con costo: definir `OPENROUTER_API_KEY` y ejecutar `ai-service/.venv/Scripts/python.exe ai-service/tests/live_openrouter.py`.
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
| AULA-01 | Generación/eliminación; admin | Año no planificado, sin plan de estudios, repetición | Rechazo, idempotencia, orden al borrar matrículas | `ClassroomCommandServiceImplTest` |
| AULA-02 | `/classrooms?userId=...`; estudiante | ID de otro usuario; parámetro `1 OR 1=1` | 403/400, sin detalle SQL | `critical-routes.spec.ts` |
| EVAL-01 | Cuestionarios; docente/coordinador/estudiante | Curso no asignado, alumno no matriculado, instancia ajena | 403 antes de generar, corregir o notificar | `QuestionnaireCommandServiceImplTest` |
| EVAL-02 | Entrega de cuestionario; estudiante | Opción negativa/fuera de rango o ID de pregunta inventado | 400 antes de calificar o guardar | `QuestionnaireCommandServiceImplTest` |
| DOC-01 | Carga PDF; coordinador | MIME/archivo inválido, corrupto, cifrado, activo, >50 MiB | 400/413 antes de MinIO; ningún envío parcial por archivo inválido del lote | `PdfUploadValidatorTest`, `DocumentCommandServiceImplTest` |
| DOC-02 | Procesamiento de PDF y RabbitMQ | PDF roto o sin texto; error de broker; respuesta tardía | Evento de fallo saneado, estado `FAILED`, redelivery no resucita documento | `test_worker_security.py`, `DocumentLifecycleTest`, `DocumentCommandServiceImplTest` |
| DOC-03 | `/courses/{id}/documents/bulk` | JSON de metadata mal formado con dato sensible | 400 sin eco del cuerpo | `DocumentControllerTest`, `GlobalExceptionHandlerTest` |
| REPO-01 | PostgreSQL/pgvector | Rollback, destino curso/grado ajeno, SQLi como parámetro, estado no READY | Transacción revierte y la recuperación filtra/bindea | `DocumentChunkRepositoryIT` (Docker) |
| IA-01 | `/generate`, cuestionarios y feedback | `system` falso, instrucciones en PDF/título/pregunta, secreto en error | Roles privilegiados fijos, datos en rol no privilegiado, respuesta saneada | `test_worker_security.py`, `test_worker_validation.py` |
| IA-02 | Embeddings OpenRouter simulados | Índices perdidos, dimensión errónea, NaN | Rechazo antes de persistencia | `test_worker_security.py`, `test_openrouter_contract.py` |
| IA-03 | Worker concurrente | Quinta solicitud simultánea | 503 con `Retry-After`; sin cola ilimitada | `test_worker_security.py` |
| IA-04 | Streaming worker → Java | Fragmento con saltos de línea y falso `data: [DONE]` | Evento JSON único; el texto no termina el stream | `test_worker_security.py`, `AiServiceClientTest` |
| UI-01 | Rutas de estudiante, docente y coordinador | Navegación, logout y persistencia de inglés | Rutas esperadas, sesión revocada e idioma conservado | `critical-routes.spec.ts` |
| UI-02 | Markdown/matemática y estadísticas | HTML malicioso, cálculo académico | Sanitización y resultados numéricos esperados | `markdown-math.pipe.spec.ts`, `performance-statistics.spec.ts` |
| PERF-01 | `/auth/me`, `/notifications/unread` | 10 usuarios, 200 peticiones autenticadas, máximo 25/1000 | p95 ≤ 2000 ms; errores ≤ 1 % | `scripts/load-probe.mjs` con `-RunLoad`, optativo |
| DEP-01 | Frontend, backend y worker | Dependencias vulnerables | Cero avisos conocidos en Maven/Python y cero avisos de nivel alto o crítico en frontend productivo | OSV Scanner sobre `pom.xml`, `pip-audit -r requirements.txt`, `pnpm audit --prod --audit-level high` |

Los tests de navegador usan usuarios semilla con contraseñas aleatorias y una base nueva en cada gate. IAM-03 crea un estudiante adicional solo allí. Los tests de unidad usan dobles aislados; REPO-01 usa un contenedor PostgreSQL descartable con `db/init/01_init.sql`.

## Umbrales y mediciones

- El build Angular verifica el presupuesto de error de 1,5 MB ya acordado. Tras actualizar Angular 21 dentro de su versión mayor, el bundle inicial midió **934,30 kB sin comprimir** y **217,39 kB de transferencia estimada**; permanece la advertencia frente al presupuesto orientativo de 500 kB. No equivale a una medición de navegador real.
- La carga es pequeña y local. El 29/09/2026, el gate final con `-DockerBackend -RunLoad` completó 200 solicitudes autenticadas con 10 usuarios, 0 errores, p95 de **34 ms** y 463,2 solicitudes/s sobre `/auth/me` y `/notifications/unread`. p95 ≤ 2000 ms y errores ≤ 1 % siguen siendo umbrales **provisionales**; esta medición no demuestra capacidad bajo tráfico de producción, carga de PDF, generación de IA ni rendimiento visual del navegador.
- Backend limita pool JDBC a 20 conexiones, hilos HTTP a 120, espera de conexión a 5 s, cliente IA a 45 s y consumidor RabbitMQ a 2–4. El worker limita concurrencia de proveedor a 4 y tamaño de PDF/texto. Son límites de contención, no resultados de capacidad.
- JUnit de Playwright va al temporal del gate; logs se conservan si falla. Maven genera reportes en `target/surefire-reports` y `target/failsafe-reports`. Un reporte antiguo no cuenta como ejecución reciente.

## Seguridad y límites conocidos

- Java valida nombre/MIME, bytes, estructura PDF y contenido activo; el worker rechaza PDF corrupto/cifrado, sin texto y fuera de límites. Esto no es antivirus. Cuarentena/escáner y límites de memoria de contenedor quedan pendientes para despliegue.
- Documentos y entradas del usuario se envían como datos no confiables, no como system prompt. El modelo puede seguir siendo influido por lenguaje adversarial: se necesitan evaluaciones contra un modelo real y revisión humana. No se afirma protección perfecta frente a prompt injection.
- La prueba de SQLi verifica parámetros bindeados y rutas concretas, no todos los endpoints. PostgreSQL ejercita SQL crítico, no todo el repositorio JPA en un contexto Spring completo.
- Playwright verifica rutas, roles, login/logout y un alta real. No recorre todavía formularios/datos reales de todas las aulas, cuestionarios, notas o documentos; hacen falta fixtures académicas deterministas y más escenarios antes de proclamar cobertura amplia.
- El esquema actual admite un solo refresh token vigente por usuario. Los inicios de sesión concurrentes ya no deben producir 500, pero el más reciente invalida el refresh token de la sesión anterior; la política de múltiples dispositivos requiere una decisión de producto y otro modelo de datos.
- Tras actualizar Angular y fijar `js-yaml` 4.3.2, `pnpm audit --prod --audit-level high` no encontró avisos conocidos. Tras actualizar `pydantic-settings` a 2.14.2, `pip-audit -r requirements.txt` tampoco encontró avisos conocidos. OSV Scanner detectó inicialmente 103 avisos en 28 paquetes Maven; se actualizaron Spring Boot 4.0.8, Spring AI 2.0.1, Springdoc 3.1.1, RabbitMQ, PostgreSQL, Commons Lang y los parches transitivos de Tomcat, Jackson y Bouncy Castle. El reescaneo final de `pom.xml` devolvió `No issues found`. Son resultados de las bases de avisos consultadas el 29/09/2026, no garantías futuras. Faltan pentest y SAST/DAST. OWASP Dependency-Check no pudo descargar NVD en este Windows (`Unable to establish loopback connection`); OSV aporta una auditoría alternativa, no equivale a certificar la seguridad del sistema.

## Estado y GitFlow

Los cambios se preparan en `feature/comprehensive-quality-and-security` sobre `develop` en ambos repositorios. Antes de PR feature → develop: ejecutar gate completo, revisar reportes y `git diff --check`, comprobar que no entren `.env`, contraseñas ni artefactos. Los workflows existentes exponen `Frontend / Unit + build + i18n`, `Backend / Java + PostgreSQL` y `Backend / AI offline contract`; no ejecutan todavía Playwright, carga ni auditorías. La API pública de GitHub devuelve cero rulesets visibles en ambos repositorios (29/09/2026); sin acceso autenticado no se pudo verificar la protección clásica ni fijar checks obligatorios. Es una tarea pendiente antes de exigir el PR gate. Automatización nueva de CI/CD, PR hacia `release/*`/`main` y Azure quedan para la siguiente fase acordada.

El 29/09/2026 pasó el gate completo final con `./scripts/quality-gate.ps1 -DockerBackend -RunLoad`: 37 pruebas unitarias Java, 4 de integración PostgreSQL, 17 del worker IA, 18 de Angular y 10 escenarios Playwright; comprobación i18n, build de producción, auditorías de dependencias y perfil de carga también pasaron. La llamada real a OpenRouter quedó fuera por decisión previa. Los cambios todavía requieren revisión del diff y del PR antes de integrar en `develop`; el gate local verde no implica que las protecciones de GitHub estén configuradas ni que exista cobertura total de seguridad/rendimiento.
