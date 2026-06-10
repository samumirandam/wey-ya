# Auditoría de código — ¡Wey Ya! (app completa)

**Fecha**: 2026-06-09
**Alcance**: toda la app — 38 archivos Kotlin (`app/src/main` + `app/src/test`), `AndroidManifest.xml`, recursos i18n, configuración Gradle.
**Método**: 6 auditorías AI en paralelo (QA, Seguridad, Análisis estático, Orden, Patrones de diseño, Cobertura de tests) + verificación manual de cada hallazgo contra el código fuente. Sin herramientas estáticas (semgrep/checkov no disponibles en el entorno).
**Hallazgos**: 10 accionables (0 críticos, 1 alto, 3 medios, 6 bajos). 6 reportes descartados como falsos positivos tras verificación.

> **Estado (2026-06-10)**: ✅ Los 10 hallazgos accionables fueron resueltos. `testDebugUnitTest` pasa con los tests nuevos. Cada hallazgo lleva su marca `[RESUELTO]`.

---

## Resumen ejecutivo

La app está en muy buen estado: seguridad limpia (permisos mínimos, sin INTERNET, backup deshabilitado, servicios protegidos, sin secrets versionados), i18n con paridad completa en los 5 idiomas (97 claves cada uno), threading correcto y manejo defensivo de errores en el camino crítico de screening (timeout con fail-open, DataStore corrupto degrada a defaults).

El hallazgo más relevante es arquitectónico: `ScheduleChecker` vive en `domain/` pero depende de Android (`android.util.Log`) y de la capa data (`ScheduleEntity`), contradiciendo la regla declarada en CLAUDE.md de que domain es lógica pura sin dependencias de Android. El resto son edge cases menores y brechas de tests.

---

## Auditorías sin hallazgos

- ✅ **Seguridad** — Permisos mínimos, `allowBackup="false"` + `data_extraction_rules.xml` excluyendo DBs y prefs, servicios exportados protegidos con permisos de sistema (`BIND_SCREENING_SERVICE`, `BIND_QUICK_SETTINGS_TILE`), DAOs con placeholders de Room (sin inyección SQL), logs sin datos sensibles, keystore correctamente git-ignored, validación de números con regex en Settings.

---

## Hallazgos accionables

### [ALTA] ✅ [RESUELTO] `ScheduleChecker` (domain) depende de Android y de la capa data

- **Archivo**: `app/src/main/kotlin/com/weyya/app/domain/ScheduleChecker.kt:3-4`
- **Código**:
  ```kotlin
  import android.util.Log
  import com.weyya.app.data.db.entity.ScheduleEntity
  ```
  y `Log.w(...)` en la línea 50; la firma pública recibe `List<ScheduleEntity>` (línea 25).
- **Problema**: CLAUDE.md declara «domain/ — Pure business logic, no Android dependencies». `ScheduleChecker` viola ambas direcciones: importa una entity de Room (data → domain invertido) y `android.util.Log` (dependencia de framework en lógica pura). Consecuencias concretas: los unit tests JVM dependen de que `Log` no explote (solo funciona si el path de horario malformado no se ejercita o con `returnDefaultValues`), y cualquier cambio en la entity de Room arrastra a la capa de dominio.
- **Fix**:
  1. Crear `domain/model/Schedule` (data class con `daysOfWeek`, `startTime`, `endTime`, `enabled`, `simSlot`) y mapear desde `ScheduleEntity` en el sitio de llamada (servicio/ViewModel).
  2. Eliminar `Log.w` del dominio — el horario malformado ya se ignora con `return@any false`; si se quiere observabilidad, recibir un callback opcional `onMalformed: (String) -> Unit = {}`.
- **Fuente**: AI-only (agente Patrones; severidad ajustada de CRÍTICA a ALTA — es deuda arquitectónica, no causa crash ni pérdida de datos)
- **✅ Resuelto**: nuevo `domain/model/Schedule.kt` (data class pura) + mapper `ScheduleEntity.toDomain()` en la capa data. `ScheduleChecker` ya no importa `android.util.Log` ni `ScheduleEntity`; el `Log.w` se eliminó (sin callback, nadie lo consumiría). Mapeo aplicado en los 2 call sites (`WeyYaScreeningService`, `MainViewModel`). El helper de `ScheduleCheckerTest` construye `Schedule` directamente.

---

### [MEDIA] ✅ [RESUELTO] Número vacío (`""`) se trata como número válido en el motor de decisión

- **Archivo**: `app/src/main/kotlin/com/weyya/app/domain/CallDecisionEngine.kt:33` y `app/src/main/kotlin/com/weyya/app/service/WeyYaScreeningService.kt:64`
- **Código**:
  ```kotlin
  if (phoneNumber == null) {
      return CallDecision.Reject("Hidden number")
  }
  ```
- **Problema**: El servicio extrae `handle?.schemeSpecificPart`, que para un URI `tel:` degenerado puede ser `""` (no null). Un string vacío pasa el check de null y entonces: se consulta ContactsProvider y la whitelist con `""`, se registra en `CallAttemptTracker` bajo la clave `""`, y se puede insertar `BlockedCallEntity(phoneNumber = "")` — que en el Log de la UI se muestra distinto a un número oculto (null) siendo semánticamente lo mismo.
- **Fix**: Cambiar el check a `if (phoneNumber.isNullOrBlank()) return CallDecision.Reject("Hidden number")` y normalizar `number` en el servicio (`?.takeIf { it.isNotBlank() }`), para que vacío y oculto sigan el mismo camino.
- **Fuente**: AI-only (agente QA; severidad ajustada de CRÍTICA a MEDIA — no hay crash, es un edge case improbable con inconsistencia de datos)
- **✅ Resuelto**: `isNullOrBlank()` en `CallDecisionEngine` + `?.takeIf { it.isNotBlank() }` en el servicio. Tests `rejects empty/blank phone number as hidden` agregados.

---

### [MEDIA] ✅ [RESUELTO] Mapeo `BlockingMode → string de recurso` duplicado en 3 sitios

- **Archivos**:
  - `app/src/main/kotlin/com/weyya/app/service/WeyYaTileService.kt:50-54`
  - `app/src/main/kotlin/com/weyya/app/widget/WidgetDataHelper.kt:45-48`
  - `app/src/main/kotlin/com/weyya/app/ui/main/MainScreen.kt` (selector de modo)
- **Código** (TileService):
  ```kotlin
  tile.subtitle = when {
      !isActive -> getString(R.string.protection_off)
      mode == BlockingMode.ALL_CALLERS -> getString(R.string.mode_all)
      else -> getString(R.string.mode_unknown)
  }
  ```
- **Problema**: La traducción de modo a texto visible existe en tres lugares independientes. Agregar un tercer modo de bloqueo (o renombrar una clave) exige tocar los tres y es fácil olvidar uno (el widget y el tile no fallan en compilación si el `when` queda incompleto en uno de ellos... sí fallan con enum exhaustivo, pero el caso `!isActive` no).
- **Fix**: Función única, p. ej. `fun BlockingMode.labelRes(): Int = when (this) { UNKNOWN_CALLERS -> R.string.mode_unknown; ALL_CALLERS -> R.string.mode_all }` en `ui/` o junto al enum (devolviendo `@StringRes Int` para no meter `Context` en domain).
- **Fuente**: AI-only (agente Orden)
- **✅ Resuelto**: `BlockingMode.labelRes()` (`@StringRes`) en `ui/common/BlockingModeLabel.kt`, usado en `WeyYaTileService`, `WidgetDataHelper` y `MainScreen`. `when` exhaustivo sin `else` → un modo nuevo falla en compilación en un único sitio.

---

### [MEDIA] ✅ [RESUELTO] Boundaries de `ScheduleChecker` sin fijar por tests

- **Archivo**: `app/src/main/kotlin/com/weyya/app/domain/ScheduleChecker.kt:53-61` y `app/src/test/kotlin/com/weyya/app/domain/ScheduleCheckerTest.kt`
- **Código**:
  ```kotlin
  val crossesMidnight = endTime <= startTime
  ...
  todayIso in schedule.daysList() && currentTime in startTime..endTime
  ```
- **Problema**: Dos comportamientos de borde no están verificados por tests:
  1. La hora final exacta es inclusiva (`in startTime..endTime` incluye `endTime`): con horario 09:00–17:00, una llamada a las 17:00:00 en punto se bloquea. Es razonable, pero al no estar fijado por un test, una refactorización a `<` lo cambiaría silenciosamente.
  2. `startTime == endTime` (p. ej. "09:00–09:00") se clasifica como `crossesMidnight` y cubre efectivamente las 24 horas. Si es intencional, merece un test que lo documente; si no, es una sorpresa para el usuario que lo configure.
- **Fix**: Agregar a `ScheduleCheckerTest`: caso `currentTime == endTime` exacto (bloquea), `currentTime == endTime + 1 min` (no bloquea), y caso `start == end` documentando el comportamiento esperado.
- **Fuente**: AI-only (agente Tests)
- **✅ Resuelto**: tests `endTime is inclusive...` y `one minute past endTime is not blocked` agregados. El caso `start == end` ya estaba fijado por `startTime equals endTime crosses midnight path`.

---

### [BAJA] ✅ [RESUELTO] `WeyYaTileService.onClick()` hace dos `runBlocking` consecutivos

- **Archivo**: `app/src/main/kotlin/com/weyya/app/service/WeyYaTileService.kt:32-46`
- **Código**: `onClick()` ejecuta un `runBlocking` para el toggle y luego `updateTile()` abre otro `runBlocking` para releer el mismo estado.
- **Problema**: Dos bloqueos de main thread donde basta uno; el estado recién escrito se relee de DataStore.
- **Fix**: En `onClick()`, calcular el nuevo estado dentro del mismo bloque y pasar `(isActive, mode)` a un `updateTile(state)` que solo pinte; `onStartListening()` puede conservar la lectura.
- **Fuente**: AI-only (agente Orden)
- **✅ Resuelto**: `onClick()` togglea y lee el modo en un único `runBlocking`; `updateTile(isActive, mode)` solo pinta. `onStartListening()` conserva su lectura.

### [BAJA] ✅ [RESUELTO] Construcción de `BlockedCallEntity` duplicada en el servicio

- **Archivo**: `app/src/main/kotlin/com/weyya/app/service/WeyYaScreeningService.kt:119-126` y `142-149`
- **Problema**: Dos bloques idénticos (insert + try/catch + log) que solo difieren en `wasEventuallyAllowed`. Cambios en la entity o en el manejo de error deben hacerse dos veces.
- **Fix**: Helper privado `recordCall(number: String, attemptCount: Int, wasAllowed: Boolean)` que encapsule el `runBlocking` + try/catch.
- **Fuente**: AI-only (agente Orden)
- **✅ Resuelto**: helper privado `recordCall(dao, number, attemptCount, wasAllowed)` reemplaza ambos bloques.

### [BAJA] ✅ [RESUELTO] `csvField()` sin tests de regresión

- **Archivo**: `app/src/main/kotlin/com/weyya/app/ui/log/LogViewModel.kt:86-87`
- **Problema**: El escaping CSV (comas, comillas, saltos de línea) es correcto hoy (verificado manualmente), pero es exactamente el tipo de lógica que se rompe en una edición rápida y nadie nota hasta que un export sale corrupto.
- **Fix**: Hacerla `internal` (o moverla a un objeto `CsvUtils`) y testear: valor simple sin cambios, valor con coma, valor con comillas (`"` → `""`), valor con `\n`.
- **Fuente**: AI-only (agente Tests)
- **✅ Resuelto**: movida a `util/CsvUtils.kt`; `CsvUtilsTest` cubre simple, coma, comilla, `\n` y `\r`.

### [BAJA] ✅ [RESUELTO] `BlockingMode.fromString/toStorageString` sin test de round-trip

- **Archivo**: `app/src/main/kotlin/com/weyya/app/domain/model/BlockingMode.kt:8-19`
- **Problema**: La serialización de la preferencia central de la app no tiene test. Un valor desconocido cae silenciosamente a `UNKNOWN_CALLERS` (default razonable, pero no documentado por test).
- **Fix**: Test pequeño: round-trip de ambos modos + `fromString("garbage") == UNKNOWN_CALLERS`.
- **Fuente**: AI-only (agente Tests)
- **✅ Resuelto**: `BlockingModeTest` con round-trip de ambos modos + fallback de valor desconocido.

### [BAJA] ✅ [RESUELTO] `daysSinceFirstActivation` calcula con `System.currentTimeMillis()` inline — no testeable

- **Archivo**: `app/src/main/kotlin/com/weyya/app/ui/privacy/PrivacyDashboardViewModel.kt:32-37`
- **Problema**: La aritmética de días (truncamiento, `coerceAtLeast(0)`) está embebida en el ViewModel con reloj real, así que no se puede verificar el redondeo (¿1.9 días muestra 1?). La lógica actual es correcta, pero queda sin red.
- **Fix**: Extraer `fun daysBetween(firstMillis: Long, nowMillis: Long): Int` a `TimeUtils` y testear los bordes (0 ms → 0, un día menos 1 ms → 0, un día exacto → 1).
- **Fuente**: AI-only (agente Tests)
- **✅ Resuelto**: `TimeUtils.daysBetween()` extraído; `PrivacyDashboardViewModel` delega. Tests de borde (0, día−1ms, día exacto, span negativo).

### [BAJA] ✅ [RESUELTO] `TimeUtils.daysAgoStartMillis()` sin tests directos

- **Archivo**: `app/src/main/kotlin/com/weyya/app/util/TimeUtils.kt:9-16`
- **Problema**: `TimeUtilsTest` solo cubre el caso `days = 0`. Los filtros del Log (7 y 30 días) dependen de esta función sin verificación.
- **Fix**: Extender `TimeUtilsTest` con `daysAgoStartMillis(1)` y `daysAgoStartMillis(7)`.
- **Fuente**: AI-only (agente Tests)
- **✅ Resuelto**: tests para `daysAgoStartMillis(1)` y `(7)` agregados, DST-safe (verifican medianoche + día calendario, no un offset fijo en millis).

---

## Descartados tras verificación (falsos positivos)

- **Cast `as Activity` en `Theme.kt:49`** — es el patrón estándar del template de Android Studio, protegido por `if (!view.isInEditMode)`; el único host del theme es `MainActivity`.
- **"Cursor leak" en `SettingsScreen.kt:138-154`** — `phoneCursor?.use { }` se invoca inmediatamente tras el `query()` sin código intermedio; `.use` cierra el cursor incluso con excepción.
- **Null-check de `RoleManager`/`PowerManager` en `MainScreen.kt:88-100`** — esos system services existen siempre en teléfonos API 29+; el riesgo es teórico.
- **`WidgetDataHelper.readState` sin try-catch** — `UserPreferences` ya degrada `IOException` a defaults (`UserPreferences.kt:48-56`); el flujo no propaga el error.
- **Timeout permite llamadas en modo ALL_CALLERS** — fail-open intencional y documentado en el comentario de `WeyYaScreeningService.kt:67-68` (mejor dejar pasar una llamada que colgar el screening).
- **`getString` defensivo con try-catch en cursores** — `getColumnIndexOrThrow` ya cubre el caso; el try-catch sugerido sería ruido.

---

## Orden de ataque sugerido

1. **ALTA — pureza de domain** (`Schedule` model + quitar `Log`): es la única deuda estructural; mientras más código se apoye en `ScheduleEntity`, más cara se vuelve.
2. **MEDIA — `isNullOrBlank` en el motor de decisión**: fix de 2 líneas + 1 test.
3. **MEDIA — tests de boundary de `ScheduleChecker`**: baratos y protegen el corazón de la app.
4. **MEDIA — unificar mapeo de `BlockingMode`**: hacerlo antes de agregar cualquier modo nuevo.
5. **BAJAs**: en cualquier ventana de mantenimiento; ninguna es urgente.
