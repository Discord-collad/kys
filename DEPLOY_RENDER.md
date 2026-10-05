# Desplegar KYS en Render

Runbook paso a paso. El proyecto ya está preparado (ver `render.yaml` y los
cambios en `application.properties`). Solo queda ejecutar.

---

## Pre-requisitos verificados
- [x] Git instalado: `C:\Program Files\Git\bin\git.exe`
- [x] `pg_dump` / `pg_restore` / `psql`: `C:\Program Files\PostgreSQL\12\bin\` (Postgres 12.22)
- [x] Repositorio Git inicializado con 76 archivos en staging
- [x] `render.yaml` creado (Blueprint con Web Service + Postgres)
- [x] `application.properties` con DB URL/username/password y SQL init mode externalizados
- [x] Notas locales de desarrollo excluidas del repo (`PROMPT_*.txt`, `LOG_*.txt`, etc.)
- [x] `mvnw` marcado como ejecutable en el índice de Git

---

## PASO 1 — Commit + Push a GitHub

1. Configurá tu identidad de Git (solo la primera vez en esta PC):
   ```powershell
   git config --global user.name "Tu Nombre"
   git config --global user.email "tu@email.com"
   ```

2. Hacé el commit inicial:
   ```powershell
   cd C:\Users\pablo\OneDrive\Documents\NetBeansProjects\kys
   git commit -m "Deploy: render.yaml + externalize DB config"
   ```

3. Creá el repo en GitHub:
   - Andá a https://github.com/new
   - **Repository name**: `kys` (o el nombre que quieras)
   - **Visibilidad**: Private recomendado (tiene datos académicos)
   - **NO** inicialices con README, .gitignore ni license (ya están acá)
   - Click **Create repository**

4. Conectá y subí:
   ```powershell
   git remote add origin https://github.com/<TU_USUARIO>/<TU_REPO>.git
   git branch -M main
   git push -u origin main
   ```

---

## PASO 2 — Crear el Blueprint en Render

1. Andá a https://dashboard.render.com/
2. Click **New +** → **Blueprint**
3. Click **Connect account** si nunca vinculaste GitHub
4. Elegí el repo `kys` (o como lo hayas llamado)
5. Render detecta automáticamente `render.yaml` y muestra los 2 servicios:
   - `kys` (web service, Java)
   - `kys-db` (PostgreSQL, free)
6. Click **Apply**
7. Esperá ~5–10 min. Render va a:
   - Crear la DB `kys-db` (plan Free, region Oregon)
   - Clonar el repo, correr `./mvnw -DskipTests package`
   - Levantar el JAR en `java -jar target/kys-0.0.1-SNAPSHOT.jar`
8. Cuando el Web Service diga **"Live"**, ya está arriba (vacío, sin datos).

---

## PASO 3 — Dump de tu Postgres local

El proyecto usa DB local `Mio.1` con password `12345678` (ver `application.properties`).
Ajustá si tu password es distinto.

```powershell
$env:PGPASSWORD = "12345678"
& "C:\Program Files\PostgreSQL\12\bin\pg_dump.exe" `
    -h localhost `
    -U postgres `
    -F c `
    -b `
    -v `
    -f "$env:USERPROFILE\Desktop\kys.dump" `
    "Mio.1"
```

Resultado: `C:\Users\pablo\Desktop\kys.dump` (~comprimido).

> Si tu DB local no se llama exactamente `Mio.1` o tu Postgres y carga con otro puerto,
> ajustá los flags. Para listar tus DBs:
> ```powershell
> & "C:\Program Files\PostgreSQL\12\bin\psql.exe" -h localhost -U postgres -l
> ```

---

## PASO 4 — Permitir tu IP en Render y restaurar

1. En Render → `kys-db` → **Access** → **Allow** → pegá tu IP pública actual
   (sacala de https://ifconfig.me) o, más fácil para una sola vez, agregá `0.0.0.0/0`
   (¡recordá sacarlo después!).

2. Copiá la **External Connection String** de la DB. Formato:
   ```
   postgresql://kys:<PASSWORD>@dpg-xxxxx-a.oregon-postgres.render.com:5432/kys
   ```

3. Restaurá el dump:
   ```powershell
   # Sacá host, password y usuario del connection string de Render
   $renderHost = "dpg-xxxxx-a.oregon-postgres.render.com"
   $renderUser = "kys"
   $renderPass = "<PEGAR_AQUI>"
   $env:PGPASSWORD = $renderPass

   & "C:\Program Files\PostgreSQL\12\bin\pg_restore.exe" `
       -h $renderHost `
       -p 5432 `
       -U $renderUser `
       -d kys `
       -v `
       --no-owner `
       --no-privileges `
       --clean `
       --if-exists `
       "$env:USERPROFILE\Desktop\kys.dump" `
       2>&1 | Tee-Object -FilePath "$env:USERPROFILE\Desktop\pg_restore.log"
   ```
   Si la DB en Render ya tiene algo (tablas creadas por la primera ejecución),
   `--clean --if-exists` dropea y recrea limpio.

---

## PASO 5 — Verificar

1. Abrí la URL del Web Service en Render (algo como `https://kys-xxxx.onrender.com`)
2. Debería mostrar la pantalla de login
3. Probá login con un usuario de tu DB original

> **Nota sobre free tier**: Si nadie visita el sitio por 15 min, Render duerme el
> servicio. La primera request tarda ~30s extra mientras levanta.

---

## Problemas comunes

**Build falla con "invalid target release: 21"**
Tu `pom.xml` pide Java 21. Render lo configura con `JAVA_VERSION=21` desde
`render.yaml`. Si aún así falla, en Render → Environment agregá:
`JAVA_VERSION=21` (Render usa Eclipse Temurin).

**Health check falla / la app no responde**
- Ver `render logs` en el dashboard
- Buscar errores de conexión a Postgres
- Confirmar que la DB esté restaurada con `psql -h <render-host> -U kys -d kys -c "\dt"`

**Login no funciona tras restaurar**
Los password_hash son BCrypt — deberían restaurarse bien. Si no, regenéralos
desde la pantalla de director (si la tenés) o resetealos manualmente.

---

## Variables de entorno en producción (resumen)

Render inyecta automáticamente desde la DB (definido en `render.yaml`):
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Manuales (también en `render.yaml`):
- `JAVA_VERSION=21`
- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_SQL_INIT_MODE=never` (clave: NO correr migration.sql porque ya restauramos datos)