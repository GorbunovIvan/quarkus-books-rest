package org.example.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.example.model.Author;
import org.example.model.Book;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@ApplicationScoped
public class BookRepository implements PanacheRepository<Book> {

    @PersistenceContext
    EntityManager entityManager;
    @Inject
    AuthorRepository authorRepository;

    @Transactional
    public Book create(Book book) {
        persistAuthors(book);
        entityManager.persist(book);
        return book;
    }

    @Transactional
    public Book update(long id, Book book) {

        var bookInDB = entityManager.find(Book.class, id);
        if (bookInDB == null) {
            throw new RuntimeException(String.format("Book with id '%d' doesn't exist", id));
        }

        setValueIfNotEmpty(book.getTitle(), bookInDB::setTitle);
        setValueIfNotEmpty(book.getYear(), bookInDB::setYear);

        persistAuthors(book);
        setValueIfNotEmpty(book.getAuthors(), bookInDB::setAuthors);

        return bookInDB;
    }

    private <T> void setValueIfNotEmpty(T value, Consumer<T> setter) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return;
            }
        }
        setter.accept(value);
    }

    private void persistAuthors(Book book) {
        var authorsPersisted = persistAuthors(book.getAuthors());
        book.setAuthors(authorsPersisted);
    }

    private Set<Author> persistAuthors(Set<Author> authors) {

        if (authors.isEmpty()) {
            return authors;
        }

        var authorsPersisted = new HashSet<Author>();
        var authorsNew = new HashSet<Author>();

        for (var author : authors) {
            var authorFound = authorRepository.find("name", author.getName())
                    .firstResultOptional()
                    .orElse(null);
            if (authorFound != null) {
                authorsPersisted.add(authorFound);
            } else {
                authorsNew.add(author);
            }
        }

        authorRepository.persist(authorsNew);
        authorsPersisted.addAll(authorsNew);

        return authorsPersisted;
    }
}
