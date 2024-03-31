package org.example.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.example.model.Book;
import org.example.service.BookService;

import java.util.List;

@Path("/books")
public class BookResource {

    @Inject
    BookService booksService;

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Book> getAll() {
        return booksService.getAll();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book getById(long id) {
        return booksService.getById(id);
    }

    @GET
    @Path("/title/{title}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book getByTitle(String title) {
        return booksService.getByTitle(title);
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Book create(Book book) {
        return booksService.create(book);
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Book update(long id, Book book) {
        return booksService.update(id, book);
    }

    @DELETE
    @Path("/{id}")
    public void deleteById(long id) {
        booksService.deleteById(id);
    }
}
