package cl.ufro.tareas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Pruebas basadas en propiedades (jqwik) para el CRUD de {@link TaskService}.
 * Cada propiedad expresa una regla del dominio que debe cumplirse para
 * cualquier entrada generada. Cada una crea su propio servicio para partir limpia.
 */
class TaskPropertiesTest {

    // ---------------------------------------------------------------- Generadores

    /** Títulos de 1 a 40 caracteres (a-z, A-Z, 0-9, espacio, ñ, á, é) que nunca están en blanco. */
    @Provide
    Arbitrary<String> validTitles() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars(' ', 'ñ', 'á', 'é')
                .ofMinLength(1)
                .ofMaxLength(40)
                .filter(s -> !s.isBlank());
    }

    /** Títulos inválidos: vacío o compuesto solo por espacios/tabs. */
    @Provide
    Arbitrary<String> blankTitles() {
        return Arbitraries.strings()
                .withChars(' ', '\t')
                .ofMinLength(0)
                .ofMaxLength(20);
    }

    /** Listas de 1 a 10 títulos válidos. */
    @Provide
    Arbitrary<List<String>> titleLists() {
        return validTitles().list().ofMinSize(1).ofMaxSize(10);
    }

    // ---------------------------------------------------------------- Create

    // Tipo: ida y vuelta (round-trip). Lo creado se lee igual y parte sin completar.
    @Property
    void createdTaskCanBeReadBackAndStartsIncomplete(@ForAll("validTitles") String title) {
        TaskService service = new TaskService();

        Task created = service.create(title);

        assertThat(created.title()).isEqualTo(title);
        assertThat(created.completed()).isFalse();
        assertThat(service.findById(created.id())).contains(created);
    }

    // Tipo: estructural / invariante. Los ids son únicos y cada create suma exactamente 1 tarea.
    @Property
    void createAssignsUniqueIdsAndAddsExactlyOne(@ForAll("titleLists") List<String> titles) {
        TaskService service = new TaskService();
        Set<Long> ids = new HashSet<>();

        for (String title : titles) {
            int before = service.findAll().size();
            Task created = service.create(title);
            assertThat(ids.add(created.id())).as("id repetido: %d", created.id()).isTrue();
            assertThat(service.findAll()).hasSize(before + 1);
        }
        assertThat(ids).hasSize(titles.size());
    }

    // Tipo: de negocio. Un título en blanco se rechaza y no cambia el estado.
    @Property
    void createRejectsBlankTitleWithoutChangingState(
            @ForAll("blankTitles") String blank, @ForAll("titleLists") List<String> existing) {
        TaskService service = new TaskService();
        existing.forEach(service::create);
        List<Task> before = service.findAll();

        assertThatThrownBy(() -> service.create(blank)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(IllegalArgumentException.class);

        assertThat(service.findAll()).isEqualTo(before);
    }

    // ---------------------------------------------------------------- Read

    // Tipo: estructural. findAll devuelve todo lo creado, en orden, sin perder ni duplicar.
    @Property
    void findAllReturnsEverythingCreatedInOrder(@ForAll("titleLists") List<String> titles) {
        TaskService service = new TaskService();
        List<Task> created = new ArrayList<>();
        titles.forEach(t -> created.add(service.create(t)));

        assertThat(service.findAll()).containsExactlyElementsOf(created);
    }

    // Tipo: idempotencia. Un id inexistente da vacío y leer no modifica el estado.
    @Property
    void readingUnknownIdIsEmptyAndDoesNotChangeState(
            @ForAll("titleLists") List<String> titles, @ForAll long randomId) {
        TaskService service = new TaskService();
        titles.forEach(service::create);
        List<Task> before = service.findAll();
        long unknownId = before.size() + 1 + Math.abs(randomId % 1_000_000); // siempre fuera del rango asignado

        assertThat(service.findById(unknownId)).isEmpty();
        service.findAll();
        before.forEach(t -> service.findById(t.id()));

        assertThat(service.findAll()).isEqualTo(before);
    }

    // ---------------------------------------------------------------- Update

    // Tipo: de negocio. Cambia solo la tarea objetivo, conserva el id y no toca las demás.
    @Property
    void updateChangesOnlyTargetTaskAndKeepsItsId(
            @ForAll("titleLists") List<String> titles,
            @ForAll("validTitles") String newTitle,
            @ForAll boolean completed,
            @ForAll int index) {
        TaskService service = new TaskService();
        titles.forEach(service::create);
        List<Task> before = service.findAll();
        int target = Math.floorMod(index, before.size());
        long targetId = before.get(target).id();

        Task updated = service.update(targetId, newTitle, completed);

        assertThat(updated).isEqualTo(new Task(targetId, newTitle, completed));
        List<Task> after = service.findAll();
        assertThat(after).hasSameSizeAs(before);
        for (int i = 0; i < before.size(); i++) {
            if (i == target) {
                assertThat(after.get(i)).isEqualTo(updated);
            } else {
                assertThat(after.get(i)).isEqualTo(before.get(i));
            }
        }
    }

    // Tipo: idempotencia. Aplicar el mismo update dos veces equivale a aplicarlo una.
    @Property
    void updateIsIdempotent(
            @ForAll("titleLists") List<String> titles,
            @ForAll("validTitles") String newTitle,
            @ForAll boolean completed) {
        TaskService service = new TaskService();
        titles.forEach(service::create);
        long id = service.findAll().get(0).id();

        service.update(id, newTitle, completed);
        List<Task> afterOnce = service.findAll();
        service.update(id, newTitle, completed);

        assertThat(service.findAll()).isEqualTo(afterOnce);
    }

    // Tipo: de negocio. Actualizar un id inexistente lanza TaskNotFoundException.
    @Property
    void updateOfUnknownIdThrowsNotFound(
            @ForAll("titleLists") List<String> titles,
            @ForAll("validTitles") String newTitle,
            @ForAll boolean completed) {
        TaskService service = new TaskService();
        titles.forEach(service::create);
        List<Task> before = service.findAll();
        long unknownId = before.size() + 1;

        assertThatThrownBy(() -> service.update(unknownId, newTitle, completed))
                .isInstanceOf(TaskNotFoundException.class);
        assertThat(service.findAll()).isEqualTo(before);
    }

    // ---------------------------------------------------------------- Delete

    // Tipo: estructural. La tarea desaparece, hay una menos y el resto queda intacto y en orden.
    @Property
    void deleteRemovesOnlyTargetTask(@ForAll("titleLists") List<String> titles, @ForAll int index) {
        TaskService service = new TaskService();
        titles.forEach(service::create);
        List<Task> before = service.findAll();
        int target = Math.floorMod(index, before.size());
        Task removed = before.get(target);

        service.delete(removed.id());

        List<Task> expected = new ArrayList<>(before);
        expected.remove(target);
        assertThat(service.findById(removed.id())).isEmpty();
        assertThat(service.findAll()).hasSize(before.size() - 1).containsExactlyElementsOf(expected);
    }

    // Tipo: idempotencia (en sentido de error). Eliminar dos veces falla la segunda.
    @Property
    void deletingTwiceFailsTheSecondTime(@ForAll("validTitles") String title) {
        TaskService service = new TaskService();
        long id = service.create(title).id();

        service.delete(id);

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(TaskNotFoundException.class);
    }

    // Tipo: de negocio. Los ids no se reutilizan tras eliminar.
    @Property
    void idsAreNeverReusedAfterDelete(
            @ForAll("titleLists") List<String> titles, @ForAll("validTitles") String newTitle) {
        TaskService service = new TaskService();
        Set<Long> usedIds = new HashSet<>();
        titles.forEach(t -> usedIds.add(service.create(t).id()));
        service.findAll().forEach(t -> service.delete(t.id())); // se elimina todo, incluso el último id

        Task fresh = service.create(newTitle);

        assertThat(usedIds).doesNotContain(fresh.id());
    }
}
