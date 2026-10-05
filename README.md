# pbt-tareas — Property-Based Testing con jqwik

Laboratorio de Pruebas de Software (ICC735-1, UFRO): "Implementación de Property-based testing".

## Objetivo

Implementar un sistema de gestión simple de una entidad de dominio (**Tarea**) con operaciones CRUD en memoria y
validar su comportamiento mediante **pruebas basadas en propiedades**: en vez de escribir casos puntuales, se definen
**invariantes del sistema** y se verifican con cientos de entradas generadas automáticamente.

## Cómo ejecutar

Requisitos: Java 17 y Maven.

```bash
mvn test
```

## Estructura

```
pbt-tareas/
├── pom.xml
├── src/main/java/cl/ufro/tareas/
│   ├── Task.java                  # record Task(long id, String title, boolean completed)
│   ├── TaskNotFoundException.java # id inexistente
│   └── TaskService.java           # CRUD en memoria (LinkedHashMap)
└── src/test/java/cl/ufro/tareas/
    └── TaskPropertiesTest.java    # propiedades jqwik
```

## Reglas del dominio

- El título no puede ser nulo ni estar en blanco (`IllegalArgumentException`).
- Toda tarea nueva parte con `completed = false`.
- Los ids son incrementales, únicos y **nunca se reutilizan**, ni siquiera tras eliminar.
- `findAll()` devuelve una copia inmutable, en orden de creación.
- `update` y `delete` sobre un id inexistente lanzan `TaskNotFoundException`.
- `update` conserva el id de la tarea.

## Propiedades

| Operación | Propiedad | Tipo |
|-----------|-----------|------|
| Create | Lo creado se lee igual y parte no completado | Ida y vuelta |
| Create | Ids únicos y cada create suma exactamente 1 tarea | Estructural |
| Create | Un título en blanco se rechaza y no cambia el estado | De negocio |
| Read | `findAll` devuelve todo lo creado, en orden, sin perder ni duplicar | Estructural |
| Read | Un id inexistente da vacío y leer no modifica el estado | Idempotencia |
| Update | Cambia solo la tarea objetivo, conserva el id y no toca las demás | De negocio |
| Update | Aplicar el mismo update dos veces equivale a una | Idempotencia |
| Update | Un id inexistente lanza `TaskNotFoundException` | De negocio |
| Delete | La tarea desaparece, hay una menos y el resto queda intacto | Estructural |
| Delete | Eliminar dos veces falla la segunda | Idempotencia |
| Delete | Los ids no se reutilizan tras eliminar | De negocio |

## Generadores

- `validTitles`: strings de 1 a 40 caracteres (a-z, A-Z, 0-9, espacio, ñ, á, é), filtrados para que nunca sean en blanco.
- `blankTitles`: strings vacíos o solo con espacios/tabs.
- `titleLists`: listas de 1 a 10 títulos válidos.

## ¿Por qué jqwik?

- Es una librería de property-based testing nativa de la JVM, integrada con JUnit Platform (corre con `mvn test`).
- Generación de datos declarativa (`@Provide`, `Arbitraries`) y **shrinking**: ante un fallo reduce el contraejemplo al
  caso más pequeño que lo reproduce.
- Reproducibilidad: guarda las semillas de fallos anteriores (`.jqwik-database`).
- Menos código repetitivo que otras opciones en Java y buena documentación.

## Uso de herramientas de IA

Parte del código fue generado con asistencia de Claude (Anthropic) y revisado por el equipo.
