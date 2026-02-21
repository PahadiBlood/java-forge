package xyz.manojraw.book;

import org.junit.jupiter.api.*;
import xyz.manojraw.exception.BookNotFoundException;
import xyz.manojraw.exception.DuplicateBookEntryException;
import xyz.manojraw.model.Book;
import xyz.manojraw.model.Library;

import static org.junit.jupiter.api.Assertions.*;

/*
    1. Adding a valid book should increase total count by 1
    2. Adding null should throw IllegalArgumentException
    3. Adding a duplicate ISBN should throw IllegalArgumentException
    4. Finding by a valid ISBN should return the correct book
    5. Finding by an invalid ISBN should throw BookNotFoundException
    6. Finding by author should return only that author's books
    7. Finding by an unknown author should return an empty list
*/


public class LibraryTest {
    Library library;

    @BeforeEach
    void init() {
        library = new Library();
        library.addBook(new Book(1L, "Test", "Manoj", "1234"));
    }

    @Test
    void bookCountIncreaseTest() {
        int existingSize = library.countTotalBooks();

        var book = new Book(3L, "Test3", "Manoj", "123456");
        library.addBook(book);

        int newSize = library.countTotalBooks();
        assertTrue(newSize > existingSize);
    }

    @Test
    void addBookNullTest() {
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addBook(null));
    }

    @Test
    void addBookIsbnDuplicateTest() {
        var book = new Book(2L, "Test2", "Manoj", "1234");
        assertThrowsExactly(DuplicateBookEntryException.class, () -> library.addBook(book));
    }

    @Test
    void findBookByMatchingIsbnTest() {
        var book = library.findByIsbn("1234");
        assertNotNull(book);
        assertEquals("1234", book.getIsbn());
    }

    @Test
    void findBookByWrongIsbnTest() {
        assertThrowsExactly(BookNotFoundException.class, () -> library.findByIsbn("1223"));
    }

    @Test
    void getAllBooksByMatchingAuthorOnlyTest() {
        var authorBooks = library.getAllByAuthor("Manoj");
        assertTrue(authorBooks.stream()
                .allMatch(b -> "Manoj".equals(b.getAuthor()))
        );
    }

    @Test
    void getAuthorEmptyBooksTest() {
        assertTrue(library.getAllByAuthor("1223").isEmpty());
    }

    @Test
    void listImmutabilityTest() {
        var book = new Book(2L, "Test2", "Manoj", "12345");
        var books = library.getAllBooks();
        assertThrows(UnsupportedOperationException.class, () -> books.add(book));
    }

    @AfterAll
    static void postTest() {
        System.out.println("All tests completed");
    }
}
