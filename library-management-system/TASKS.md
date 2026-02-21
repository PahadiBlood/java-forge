# Tasks: library-management-system

> Complete each task in order. Every task builds on the previous one.
> Write your implementation first, then write tests before moving on.

---

## Task 1 &nbsp;·&nbsp; Book Model &nbsp;`Easy`

### Problem

Create the foundation of the system- a `Book` class that represents a single book in the library. When a book is created it must always be available by default without being explicitly told so.

### Requirements

- Create a `Book` class with fields: `id`, `title`, `author`, `isbn`, `isAvailable`
- `isAvailable` must default to `true` - it must not be a constructor parameter
- Provide getters and setters for all fields
- `toString()` must include both title and author

### Tests to Pass

```
1. A newly created book must be available by default
2. Setting availability to false must reflect when retrieved
3. toString() must contain the book title
4. toString() must contain the book author
5. getIsbn() must return the value passed in the constructor
```

---

## Task 2 &nbsp;·&nbsp; Library - Add & Search &nbsp;`Easy-Medium`

### Problem

Create a `Library` class to store books. The library must reject invalid or duplicate entries and must be able to search books by ISBN or by author. It should never return `null` - use exceptions for failures.

### Requirements

- `addBook(Book book)` - adds a book to the library
- `findByIsbn(String isbn)` - returns the matching book
- `findByAuthor(String author)` - returns a list of matching books
- `getAllBooks()` - returns an unmodifiable list of all books
- Create a custom `BookNotFoundException` exception class
- Use streams for `findByAuthor`

### Rules

```
addBook(null)                   → throws IllegalArgumentException
addBook(duplicateIsbn)          → throws IllegalArgumentException
findByIsbn(nonExistentIsbn)     → throws BookNotFoundException
findByAuthor(unknownAuthor)     → returns empty list, never throws
getAllBooks()                    → returned list must not be modifiable
```

### Tests to Pass

```
1. Adding a valid book should increase total count by 1
2. Adding null should throw IllegalArgumentException
3. Adding a duplicate ISBN should throw IllegalArgumentException
4. Finding by a valid ISBN should return the correct book
5. Finding by an invalid ISBN should throw BookNotFoundException
6. Finding by author should return only that author's books
7. Finding by an unknown author should return an empty list
```

---

## Task 3 &nbsp;·&nbsp; Borrow & Return &nbsp;`Medium`

### Problem

Extend `Library` to support borrowing and returning books. A book cannot be borrowed twice without being returned in between. A book that was never borrowed cannot be returned.

### Requirements

- `borrowBook(String isbn)` - marks the book as unavailable
- `returnBook(String isbn)` - marks the book as available
- `getAvailableBooks()` - returns all currently available books
- `getBorrowedBooks()` - returns all currently borrowed books
- Create a custom `BookNotAvailableException` exception class
- Use streams for both getter methods

### Rules

```
borrowBook(nonExistentIsbn)     → throws BookNotFoundException
borrowBook(alreadyBorrowed)     → throws BookNotAvailableException
returnBook(nonExistentIsbn)     → throws BookNotFoundException
returnBook(notBorrowed)         → throws IllegalStateException
```

### Tests to Pass

```
1. Borrowing a book should make it unavailable
2. Borrowing an already borrowed book should throw BookNotAvailableException
3. Returning a borrowed book should make it available again
4. Returning a book that was never borrowed should throw IllegalStateException
5. Available count should decrease by 1 after borrowing
6. Borrowed count should increase by 1 after borrowing
7. Borrowing a non-existent ISBN should throw BookNotFoundException
```

---

## Task 4 &nbsp;·&nbsp; BorrowingService & History &nbsp;`Medium`

### Problem

`Library` is getting too large. Move the borrowing logic into a dedicated `BorrowingService` class. This service must also keep a chronological log of every borrow and return action that occurred.

### Requirements

- Create `BorrowingService` that takes a `Library` through its constructor
- Move `borrowBook` and `returnBook` into this service
- `getBorrowingHistory()` - returns a chronological, unmodifiable list of log entries
- `getTotalBorrowCount()` - returns how many times books have been borrowed

### History Format

```
"BORROWED: Clean Code by Robert Martin"
"RETURNED: Clean Code by Robert Martin"
```

### Rules

```
getTotalBorrowCount()   → counts borrows only, not returns
getBorrowingHistory()   → must be unmodifiable
getBorrowingHistory()   → must be in chronological order
```

### Tests to Pass

```
1. Borrowing a book should add a BORROWED entry to history
2. Returning a book should add a RETURNED entry to history
3. Each history entry must contain the book title and author
4. Total borrow count must only count borrows, not returns
5. History must be in correct chronological order
6. Borrowing 3 books should result in a history size of 3
```

---

## Task 5 &nbsp;·&nbsp; Parameterized Tests &nbsp;`Medium-Hard`

### Problem

No new features. Your job is to go back and strengthen your existing tests using parameterized testing - running the same test logic across multiple inputs without duplicating test methods.

### Requirements

- No changes to application code - tests only
- Use `@ParameterizedTest`, `@ValueSource`, `@NullAndEmptySource`, `@CsvSource`

### Tests to Write

```
1. null, "", "   ", "\t" passed to findByIsbn must all throw
2. Known author with 2 books  → size 2
   Known author with 1 book   → size 1
   Unknown author             → size 0
3. Borrow 1 book → history size 1
   Borrow 2 books → history size 2
   Borrow 3 books → history size 3
```

### Dependency Required

Add this to your `pom.xml` to enable `@ParameterizedTest`:


---

## Checklist

```
 Task 1  →  Book model with correct default availability
 Task 2  →  Library with add, search, and proper exceptions
 Task 3  →  Borrow and return with all edge cases handled
 Task 4  →  BorrowingService with chronological history
 Task 5  →  Parameterized tests across all layers
```

---