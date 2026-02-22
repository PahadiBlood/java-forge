package xyz.manojraw.book;

import org.junit.jupiter.api.Test;
import xyz.manojraw.exception.BookNotAvailableException;
import xyz.manojraw.exception.BookNotFoundException;
import xyz.manojraw.model.Book;
import xyz.manojraw.model.Library;

import static org.junit.jupiter.api.Assertions.*;

public class BookBorrowTest {

    // 1. Borrowing a book should make it unavailable
    @Test
    void borrowBookMakesUnavailable() {
        Library library = createLibraryWithOneBook();

        library.borrowBook("1234");

        assertFalse(library.findByIsbn("1234").isAvailable());
    }

    // 2. Borrowing already borrowed book throws exception
    @Test
    void borrowingAlreadyBorrowedBookThrowsException() {
        Library library = createLibraryWithOneBook();
        library.borrowBook("1234");

        assertThrowsExactly(BookNotAvailableException.class,
                () -> library.borrowBook("1234"));
    }

    // 3. Returning borrowed book makes it available
    @Test
    void returnBorrowedBookMakesAvailable() {
        Library library = createLibraryWithOneBook();
        library.borrowBook("1234");

        library.returnBook("1234");

        assertTrue(library.findByIsbn("1234").isAvailable());
    }

    // 4. Returning non-borrowed book throws IllegalStateException
    @Test
    void returnNonBorrowedBookThrowsException() {
        Library library = createLibraryWithOneBook();

        assertThrowsExactly(IllegalStateException.class,
                () -> library.returnBook("1234"));
    }

    // 5. Borrowing unknown ISBN throws BookNotFoundException
    @Test
    void borrowUnknownIsbnThrowsException() {
        Library library = createLibraryWithOneBook();

        assertThrowsExactly(BookNotFoundException.class,
                () -> library.borrowBook("9999"));
    }

    // 6. Returning unknown ISBN throws BookNotFoundException
    @Test
    void returnUnknownIsbnThrowsException() {
        Library library = createLibraryWithOneBook();

        assertThrowsExactly(BookNotFoundException.class,
                () -> library.returnBook("9999"));
    }

    private Library createLibraryWithOneBook() {
        Library library = new Library();
        library.addBook(new Book(1L, "Test", "Manoj", "1234"));
        return library;
    }
}