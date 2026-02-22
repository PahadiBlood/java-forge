package xyz.manojraw.exception;

public class BookNotAvailableException extends RuntimeException {
    public BookNotAvailableException() {
        super("Book not available");
    }

    public BookNotAvailableException(String message) {
        super(message);
    }
}
