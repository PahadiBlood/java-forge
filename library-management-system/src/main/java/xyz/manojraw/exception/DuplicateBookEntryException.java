package xyz.manojraw.exception;

public class DuplicateBookEntryException extends RuntimeException {
    public DuplicateBookEntryException(String message) {
        super(String.format("Book with same id, title or isbn already exists : %s", message));
    }
}
