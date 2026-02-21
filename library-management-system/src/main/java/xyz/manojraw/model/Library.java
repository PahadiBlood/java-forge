package xyz.manojraw.model;

import xyz.manojraw.exception.BookNotFoundException;
import xyz.manojraw.exception.DuplicateBookEntryException;

import java.util.ArrayList;
import java.util.List;

public class Library {
    private final List<Book> books = new ArrayList<>();

    public void addBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException();
        }
        if (books.stream().anyMatch(b -> isBookExists(b, book))) {
            throw new DuplicateBookEntryException(book.toString());
        }
        books.add(book);
    }

    public List<Book> getAllBooks() {
        return List.copyOf(books);
    }

    public Book findByIsbn(String isbn) {
        return books.stream().filter(b -> b.getIsbn().equals(isbn)).findFirst().orElseThrow(BookNotFoundException::new);
    }

    public List<Book> getAllByAuthor(String author) {
        return books.stream().filter(b -> b.getAuthor().equals(author)).toList();
    }

    private boolean isBookExists(Book e, Book n) {
        return e.getId().equals(n.getId()) || e.getTitle().equals(n.getTitle()) || e.getIsbn().equals(n.getIsbn());
    }

    public int countTotalBooks() {
        return books.size();
    }
}
