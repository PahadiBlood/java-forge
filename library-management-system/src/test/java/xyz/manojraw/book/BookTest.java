package xyz.manojraw.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.manojraw.model.Book;

import static org.junit.jupiter.api.Assertions.*;

/*
    1. A newly created book must be available by default
    2. Setting availability to false must reflect when retrieved
    3. toString() must contain the book title
    4. toString() must contain the book author
    5. getIsbn() must return the value passed in the constructor
*/

public class BookTest {

    Book book;

    @BeforeEach
    void init() {
        book = new Book(1L, "Test", "Manoj", "1234");
    }

    @Test
    public void createBookTest() {
        assertNotNull(book);
    }

    @Test
    public void isAvailableTest() {
        book = new Book(1L, "Test", "Manoj", "1234", false);
        assertFalse(book.isAvailable());
    }

    @Test
    public void toStringTest() {
        assertTrue(book.toString().contains("title"));
        assertTrue(book.toString().contains("author"));
    }

    @Test
    public void isbnTest() {
        assertEquals("1234", book.getIsbn());
    }
}
