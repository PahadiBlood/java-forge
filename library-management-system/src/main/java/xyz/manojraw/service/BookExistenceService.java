package xyz.manojraw.service;

import xyz.manojraw.model.Book;

@FunctionalInterface
public interface BookExistenceService {
    boolean isBookExists(Book existingBook, Book newBook);
}
