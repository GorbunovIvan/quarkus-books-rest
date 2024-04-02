package org.example.service;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.example.model.Author;
import org.example.model.Book;
import org.example.repository.BookRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
class BookServiceTest {

    @Inject
    BookService bookService;

    @InjectSpy
    BookRepository bookRepository;

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

        books.forEach(bookRepository::create);
        assertFalse(bookRepository.findAll().list().isEmpty());

        Mockito.clearInvocations(bookRepository);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        bookRepository.deleteAll();
    }

    @Test
    void testGetAll() {
        var booksFound = bookService.getAll();
        assertEquals(new HashSet<>(books), new HashSet<>(booksFound));
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    void testGetById() {
        for (var book : books) {
            var bookFound = bookService.getById(book.getId());
            assertEquals(book, bookFound);
            verify(bookRepository, times(1)).findById(book.getId());
        }
        verify(bookRepository, times(books.size())).findById(anyLong());
    }

    @Test
    void testGetByIdNotFound() {
        var id = -1L;
        var bookFound = bookService.getById(id);
        assertNull(bookFound);
        verify(bookRepository, times(1)).findById(id);
    }

    @Test
    void testGetByTitle() {
        for (var book : books) {
            var bookFound = bookService.getByTitle(book.getTitle());
            assertEquals(book, bookFound);
        }
        verify(bookRepository, times(books.size())).find(anyString(), any(Object.class));
    }

    @Test
    void testGetByTitleNotFound() {
        var title = "-";
        var bookFound = bookService.getByTitle(title);
        assertNull(bookFound);
        verify(bookRepository, times(1)).find(anyString(), any(Object.class));
    }

    @Test
    void testCreate() {

        var book = new Book();
        book.setTitle("new book");
        book.setYear(1234);
        book.setAuthors(Set.of(
                    new Author(null, "new author 1", null),
                    new Author(null, "new author 2", null)
                ));

        var bookCreated = bookService.create(book);
        assertNotNull(bookCreated);
        assertNotNull(bookCreated.getId());
        assertEquals(book, bookCreated);

        verify(bookRepository, times(1)).create(book);
    }

    @Test
    void testUpdate() {

        for (var bookPersisted : books) {

            var id = bookPersisted.getId();

            var book = new Book();
            book.setTitle(bookPersisted.getTitle() + " updated");
            book.setYear(bookPersisted.getYear() + 99);

            var bookUpdated = bookService.update(id, book);
            assertEquals(id, bookUpdated.getId());
            assertEquals(book, bookUpdated);
            verify(bookRepository, times(1)).update(id, book);
        }

        verify(bookRepository, times(books.size())).update(anyLong(), any(Book.class));
    }

    @Test
    void testUpdateNotFound() {
        var id = -1L;
        var book = new Book();
        assertThrows(RuntimeException.class, () -> bookService.update(id, book));
        verify(bookRepository, times(1)).update(id, book);
    }

    @Test
    void testDeleteById() {

        assertFalse(bookRepository.findAll().list().isEmpty());

        for (var book : books) {

            var id = book.getId();

            bookService.deleteById(id);
            verify(bookRepository, times(1)).deleteById(id);
        }

        verify(bookRepository, times(books.size())).deleteById(anyLong());
        assertTrue(bookRepository.findAll().list().isEmpty());
    }
}