# Home sections: one row per category

`GET /api/home/sections?size=10` feeds the home page. `home.js` expects a JSON array. Each element is one row.

```json
{ "category": "Electronics", "totalItems": 40, "items": [ /* at most size products */ ] }
```

`category` is the heading and the "View all" link. `totalItems` is how many products are in that category, shown as "View all (40)". `items` is the cards in the row. The page sends `size=10` and renders every card you return, so `items` should hold at most `size` products.

The code is `findHomeSections` in `ProductServiceImpl`. Products are already in memory. `DataCenter.readProducts()` read `data/products.json` once at startup.

`findAllCategories` is the nav. It returns the category names as a JSON array of strings, in the order they first appear in that product list.

## Which shape should you keep?

Keep the method that is there.

1. Group the products by category once.
2. `totalItems` is `list.size()`.
3. `items` is a second stream on that same list: map to `ProductResponse`, then `limit(size)`.

`Collectors.teeing` can build the count and the list from one stream. It is the wrong tool for this row. The comment in `findHomeSections` asks for it. The sections below say why the current two steps stay.

| What is wrong | What to do |
| --- | --- |
| `groupingBy` uses a `HashMap`, so row order can change | Pass `LinkedHashMap::new` so rows follow first-seen category order |
| A teeing collector looks like one pass for the count and the cards | Keep `list.size()` and `limit(size)` |
| `PRODUCTS` is a static `CopyOnWriteArrayList` filled in `@PostConstruct` | Keep an instance field. The list from `readProducts()` is already unmodifiable |
| `System.out.println` runs on every home request | Delete that line |

## 1. Group, then count, then take a prefix

```java
var products = PRODUCTS.stream().collect(Collectors.groupingBy(Product::getCategory));

return products.entrySet().stream().map(e -> {
    var allProducts = e.getValue();
    long allProductsCount = allProducts.size();
    var limitedProducts = allProducts.stream()
            .map(p -> new ProductResponse(...))
            .limit(size)
            .toList();
    return new HomeSection(e.getKey(), allProductsCount, limitedProducts);
}).toList();
```

**Issue:** This looks like two walks per category. The comment says to collapse them with `teeing`.

**What this walk actually does:** `groupingBy` without a map factory stores each category in an `ArrayList`. `size()` on that list does not walk the products. `limit(size)` stops the mapping after `size` products. For 40 products and `size` 10, the DTO mapping touches 10 products. The other 30 stay in the list and only contribute to the count.

Order inside `items` is the order of `products.json`. `groupingBy` keeps encounter order in each list.

## 2. Why teeing does not make this cheaper

`Collectors.teeing` takes two collectors and a function that merges their results. Every product is offered to both collectors.

```java
allProducts.stream().collect(Collectors.teeing(
        Collectors.counting(),
        Collectors.mapping(p -> new ProductResponse(...), Collectors.toList()),
        (count, items) -> new HomeSection(category, count, items.stream().limit(size).toList())
));
```

**Issue:** The count needs every product. The card list needs the first `size` products. Teeing cannot stop one side early and keep going on the other. Both sides see all 40 products. The `limit` above runs only after every product has already been mapped into a list.

A teeing collector earns its place when both answers need the whole stream: a sum and a count, or a min and a max. Here one answer is a prefix. `list.size()` plus `limit(size)` already does less work.

**Fix:** Leave the count and the prefix as the two lines in section 1.

## 3. Row order

**Issue:** `Collectors.groupingBy(Product::getCategory)` uses a `HashMap`. Home rows are not in the same order as the nav from `findAllCategories`. A later run can print the rows in a different order. The nav uses `distinct()`, which keeps the first time each name appears in the product list.

**Fix:** Supply a linked map so the first-seen category stays first.

```java
PRODUCTS.stream().collect(Collectors.groupingBy(
        Product::getCategory,
        LinkedHashMap::new,
        Collectors.toList()));
```

That order matches `findAllCategories` on the same list.

## 4. The product cache

**Issue:** `PRODUCTS` is `static`. `@PostConstruct` assigns it. Every `ProductServiceImpl` in the JVM shares that one field. A test that builds a second service overwrites it. `CopyOnWriteArrayList` copies the array on each write so readers do not lock. After `init` nothing writes. `readProducts()` already returns `Collections.unmodifiableList(...)`.

**Fix:** Store the list in an instance field. Assign it from `dataCenter.readProducts()`. The unmodifiable list is the cache. `CopyOnWriteArrayList` fits a list that readers and writers share. This one is filled once and then only read.
