package cl.ufro.tareas;

/**
 * Se lanza cuando se intenta actualizar o eliminar una tarea cuyo id no existe.
 */
public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(long id) {
        super("No existe una tarea con id " + id);
    }
}
