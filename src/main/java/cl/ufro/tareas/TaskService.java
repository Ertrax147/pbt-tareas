package cl.ufro.tareas;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio CRUD de tareas con almacenamiento en memoria.
 * Usa un LinkedHashMap para conservar el orden de creación.
 * Los ids son incrementales y nunca se reutilizan, aunque se elimine una tarea.
 */
public class TaskService {

    private final Map<Long, Task> tasks = new LinkedHashMap<>();
    private long nextId = 1;

    /** Crea una tarea no completada con un id nuevo. Rechaza títulos nulos o en blanco. */
    public Task create(String title) {
        validateTitle(title);
        Task task = new Task(nextId++, title, false);
        tasks.put(task.id(), task);
        return task;
    }

    /** Busca una tarea por id; devuelve vacío si no existe. */
    public Optional<Task> findById(long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    /** Devuelve una copia inmutable de todas las tareas, en orden de creación. */
    public List<Task> findAll() {
        return List.copyOf(tasks.values());
    }

    /**
     * Reemplaza título y estado de una tarea existente, conservando su id.
     *
     * @throws TaskNotFoundException si el id no existe
     * @throws IllegalArgumentException si el título es nulo o está en blanco
     */
    public Task update(long id, String newTitle, boolean completed) {
        validateTitle(newTitle);
        if (!tasks.containsKey(id)) {
            throw new TaskNotFoundException(id);
        }
        Task updated = new Task(id, newTitle, completed);
        tasks.put(id, updated); // put sobre una clave existente no altera el orden
        return updated;
    }

    /**
     * Elimina una tarea existente.
     *
     * @throws TaskNotFoundException si el id no existe
     */
    public void delete(long id) {
        if (tasks.remove(id) == null) {
            throw new TaskNotFoundException(id);
        }
    }

    /** Regla de dominio: el título no puede ser nulo ni estar en blanco. */
    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("El título no puede ser nulo ni estar en blanco");
        }
    }
}
