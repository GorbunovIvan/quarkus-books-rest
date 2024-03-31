package org.example.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.example.model.Book;
import org.example.repository.BookRepository;

import java.util.List;

@ApplicationScoped
public class BookService {

    @Inject
    BookRepository bookRepository;

    public List<Book> getAll() {
        return bookRepository.findAll()
                .list();
    }

    public Book getById(long id) {
        return bookRepository.findById(id);
    }

    public Book getByTitle(String title) {
        return bookRepository.find("title", title)
                .firstResultOptional()
                .orElse(null);
    }

    @Transactional
    public Book create(Book book) {
        return bookRepository.create(book);
    }

    public Book update(long id, Book book) {
        return bookRepository.update(id, book);
    }

    @Transactional
    public void deleteById(long id) {
        bookRepository.deleteById(id);
    }
}
