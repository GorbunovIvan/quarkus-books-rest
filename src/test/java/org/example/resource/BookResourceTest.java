package org.example.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.example.model.Author;
import org.example.model.Book;
import org.example.service.BookService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
class BookResourceTest {

    @InjectSpy
    BookService bookService;

    @Inject
    ObjectMapper objectMapper;

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

        books.forEach(bookService::create);
        assertFalse(bookService.getAll().isEmpty());

        Mockito.clearInvocations(bookService);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        books.forEach(book -> bookService.deleteById(book.getId()));
        for (var book : bookService.getAll()) {
            bookService.deleteById(book.getId());
        }
    }

    @Test
    void testGetAll() throws JsonProcessingException {

        var jsonExpected = objectMapper.writeValueAsString(books);

        when()
                .get("/books")
        .then()
                .statusCode(200)
                .body(equalTo(jsonExpected));

        verify(bookService, times(1)).getAll();
    }

    @Test
    void testGetById() throws JsonProcessingException {

        for (var book : books) {

            var jsonExpected = objectMapper.writeValueAsString(book);

            when()
                    .get("/books/" + book.getId())
            .then()
                    .statusCode(200)
                    .body(equalTo(jsonExpected));

            verify(bookService, times(1)).getById(book.getId());
        }

        verify(bookService, times(books.size())).getById(anyLong());
    }

    @Test
    void testGetByIdNotFound() {

        var id = -1L;

        when()
                .get("/books/" + id)
        .then()
                .body(Matchers.emptyString());

        verify(bookService, times(1)).getById(id);
    }

    @Test
    void testGetByTitle() throws JsonProcessingException {

        for (var book : books) {

            var jsonExpected = objectMapper.writeValueAsString(book);

            when()
                    .get("/books/title/" + book.getTitle())
           .then()
                    .statusCode(200)
                    .body(equalTo(jsonExpected));

            verify(bookService, times(1)).getByTitle(book.getTitle());
        }

        verify(bookService, times(books.size())).getByTitle(anyString());
    }

    @Test
    void testGetByTitleNotFound() {

        var title = "-";

        when()
                .get("/books/title/" + title)
        .then()
                .body(Matchers.emptyString());

        verify(bookService, times(1)).getByTitle(title);
    }

    @Test
    void testCreate() throws JsonProcessingException {

        var book = new Book();
        book.setTitle("new book");
        book.setYear(1234);
        book.setAuthors(Set.of(
                new Author(null, "new author 1", null),
                new Author(null, "new author 2", null)
        ));

        var jsonBook = objectMapper.writeValueAsString(book);

        var jsonResponse =
                given()
                        .contentType("application/json")
                        .body(jsonBook)
                .when()
                        .post("/books")
                .then()
                        .statusCode(200)
                        .extract()
                        .asPrettyString();

        assertFalse(jsonResponse.isEmpty());

        Book bookCreated = objectMapper.readValue(jsonResponse, Book.class);

        assertNotNull(bookCreated);
        assertNotNull(bookCreated.getId());
        assertEquals(book, bookCreated);

        verify(bookService, times(1)).create(book);
    }

    @Test
    void testUpdate() throws JsonProcessingException {

        for (var bookPersisted : books) {

            var id = bookPersisted.getId();

            var book = new Book();
            book.setTitle(bookPersisted.getTitle() + " updated");
            book.setYear(bookPersisted.getYear() + 99);

            var jsonBook = objectMapper.writeValueAsString(book);

            var jsonResponse =
                    given()
                            .contentType("application/json")
                            .body(jsonBook)
                    .when()
                            .patch("/books/" + id)
                    .then()
                            .statusCode(200)
                            .extract()
                            .asPrettyString();

            assertFalse(jsonResponse.isEmpty());

            Book bookUpdated = objectMapper.readValue(jsonResponse, Book.class);

            assertEquals(id, bookUpdated.getId());
            assertEquals(book, bookUpdated);

            verify(bookService, times(1)).update(id, book);
        }

        verify(bookService, times(books.size())).update(anyLong(), any(Book.class));
    }

    @Test
    void testUpdateNotFound() throws JsonProcessingException {

        var id = -1L;
        var book = new Book();

        var jsonBook = objectMapper.writeValueAsString(book);

        given()
                .contentType("application/json")
                .body(jsonBook)
        .when()
                .patch("/books/" + id)
        .then()
                .statusCode(500);

        verify(bookService, times(1)).update(id, book);
    }

    @Test
    void testDeleteById() {

        assertFalse(bookService.getAll().isEmpty());

        for (var book : books) {

            var id = book.getId();

            when()
                    .delete("/books/" + id)
            .then()
                    .body(Matchers.emptyString());

            verify(bookService, times(1)).deleteById(id);
        }

        verify(bookService, times(books.size())).deleteById(anyLong());
        assertTrue(bookService.getAll().isEmpty());
    }
}