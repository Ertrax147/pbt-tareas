package cl.ufro.tareas;

/**
 * Entidad de dominio: una tarea con identificador, título y estado.
 * Es un record, por lo tanto inmutable; para "modificar" se crea una nueva instancia.
 */
public record Task(long id, String title, boolean completed) {
}
