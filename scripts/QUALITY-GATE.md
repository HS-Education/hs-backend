# Pruebas locales

Desde PowerShell, con Docker Desktop, Java 21, Maven, Node y pnpm disponibles:

1. Instala las dependencias del frontend con `pnpm install --frozen-lockfile` dentro de `hs-tesis-front`.
2. Crea `ai-service/.venv` e instala `ai-service/requirements.txt` en ese entorno.
3. Ejecuta `./scripts/quality-gate.ps1` desde `hs-tesis` cuando estén libres los puertos 4200, 8080, 15432, 15673 y 19000.

El gate ejecuta pruebas unitarias Java y Angular, integración PostgreSQL con pgvector, contrato OpenRouter sin red y un smoke de navegador. Para el smoke crea un proyecto Docker independiente con credenciales temporales, inicia backend y frontend en frío, comprueba login y JWT en cookie HttpOnly, y elimina los contenedores y datos de ese proyecto al terminar. No toca los contenedores existentes del entorno de desarrollo.

`./scripts/quality-gate.ps1 -SkipSmoke` ejecuta las pruebas previas al navegador. Una prueba fallida produce un código de salida no exitoso y detiene el gate; «100 % verde» significa que pasan todas las pruebas obligatorias, no 100 % de cobertura de líneas.

La llamada real a OpenRouter queda fuera del gate. Para ejecutarla por separado, configura `OPENROUTER_API_KEY` y usa `ai-service/.venv/Scripts/python.exe ai-service/tests/live_openrouter.py`. Emplea una petición sintética breve; puede consumir créditos.

La búsqueda vectorial conserva el operador de coseno `<=>` y no incorpora índice HNSW. Las pruebas de base de datos utilizan el esquema de `db/init/01_init.sql` y un contenedor descartable `pgvector/pgvector:pg16`.
