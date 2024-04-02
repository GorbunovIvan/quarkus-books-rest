package org.example.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.example.model.Author;
import org.example.model.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BookRepositoryTest {

    @Inject
    BookRepository bookRepository;

    @PersistenceContext
    EntityManager entityManager;

    private List<Book> books;

    @BeforeEach
    void setUp() {

        var authors = List.of(
                new Author(null, "test author 1", new ArrayList<>()),
                new Author(null, "test author 2", new ArrayList<>()),
                new Author(null, "test author 3", new ArrayList<>()),
                new Author(null, "test author 4", new ArrayList<>())
        );

        books = List.of(
                new Book(null, "test book 1", 1111, new HashSet<>()),
                new Book(null, "test book 2", 2222, new HashSet<>(Set.of(authors.getFirst()))),
                new Book(null, "test book 3", 3333, new HashSet<>(Set.of(authors.get(1), authors.get(2)))),
                new Book(null, "test book 4", 4444, new HashSet<>(Set.of(authors.get(0), authors.get(1)))),
                new Book(null, "test book 5", 5555, new HashSet<>(Set.of(authors.get(2), authors.get(3))))
        );
    }

    @AfterEach
    @Transactional
    void tearDown() {
        entityManager.createQuery("DELETE FROM Book").executeUpdate();
        entityManager.createQuery("DELETE FROM Author").executeUpdate();
    }

    @Test
    void testCreate() {

        for (var book : books) {
            var bookPersisted = bookRepository.create(book);
            assertNotNull(bookPersisted);
            assertNotNull(bookPersisted.getId());
            assertEquals(book, bookPersisted);
            assertEquals(entityManager.find(Book.class, bookPersisted.getId()), bookPersisted);
        }

        // Checking if authors are persisted correctly
        // (there shouldn't be different authors with the same name)
        var authorsIdsNumber = books.stream()
                .map(Book::getAuthors)
                .flatMap(Set::stream)
                .map(Author::getId)
                .distinct()
                .count();

        var authorsNamesNumber = books.stream()
                .map(Book::getAuthors)
                .flatMap(Set::stream)
                .map(Author::getName)
                .distinct()
                .count();

        assertEquals(authorsNamesNumber, authorsIdsNumber);
    }

    @Test
    @Transactional
    void testUpdate() {

        // First we need to persist books to the database
        for (var book : books) {
            entityManager.persist(book);
        }

        for (var bookPersisted : books) {

            var id = bookPersisted.getId();

            var book = new Book();
            book.setTitle(bookPersisted.getTitle() + " updated");
            book.setYear(bookPersisted.getYear() + 99);
            book.setAuthors(bookPersisted.getAuthors().stream()
                    .map(a -> new Author(null, a.getName(), Collections.emptyList()))
                    .collect(Collectors.toSet()));

            if (book.getAuthors().isEmpty()) {
                book.setAuthors(Set.of(new Author(null, "test author 1", Collections.emptyList())));
            } else {
                book.getAuthors().remove(book.getAuthors().iterator().next());
            }

            var bookUpdated = bookRepository.update(id, book);
            assertNotNull(bookUpdated);
            assertEquals(id, bookUpdated.getId());
            assertEquals(book, bookUpdated);
            assertEquals(entityManager.find(Book.class, id), bookUpdated);
        }
    }

    @Test
    void testUpdateIdNotFound() {
        var id = -1L;
        var book = new Book(null, "test book -1", 9999, Collections.emptySet());
        assertThrows(RuntimeException.class, () -> bookRepository.update(id, book));
    }
}