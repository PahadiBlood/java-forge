// ===== CarService practice tasks =====

// Task 1: findAll()
//   Return all cars (from FileService or an in-memory list).

// Task 2: findById(int id)
//   Return a single car. Use stream().filter().findFirst() and return Optional<Car>.

// Task 3: create(Car car)
//   Add a new car to the list. Validate that the ID doesn't already exist.

// Task 4: update(int id, Car updatedCar)
//   Find the car by ID and replace its fields. Handle the "not found" case.

// Task 5: delete(int id)
//   Remove the car by ID. Return true/false depending on whether it existed.

// Task 6: search(String brand, double minPrice, double maxPrice)
//   Filter using streams. Make the filters optional (skip a filter if the param is null).

// Task 7: findAllPaginated(int page, int size)
//   Return only that page's slice using skip((page - 1) * size) and limit(size).
//   Bonus: also return the total count and total pages.

// Task 8 (async): getAllCarsAsync() and getAllPeopleAsync()
//   Wrap both in CompletableFuture.supplyAsync(..., executor).
//   Pass the ExecutorService in as a parameter, don't create it inside the service.

// Task 9 (merge): getCarsWithOwners()
//   Use thenCombine to merge the cars future and the people future.
//   Match each car to its owner (or each person to the cars they like).
//   Print the result with thenAccept; don't call get() in the middle.

// Task 10 (errors): findByIdAsync(int id)
//   Throw an exception if the car doesn't exist.
//   Handle it with exceptionally() or handle() and return a fallback value.

// Remember: shut down the executor at the end, and keep main alive with join()
// if you're using callbacks.