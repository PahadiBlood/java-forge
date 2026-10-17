# mShop

mShop is a small shop you open in a browser. It has a home page, a page for one category, and a page for one product.

There is no database. Products and reviews live in JSON files. Spring Boot reads those files. The browser asks for JSON. Java sends JSON back. The page script draws the cards.

The Java version is 21. The app is Spring Boot 3.5. The HTML shells use Thymeleaf. The browser uses plain JavaScript. Jackson comes with `spring-boot-starter-web`. Lombok is on the compile classpath.

## Run

From the `mshop` folder:

```
mvn spring-boot:run
```

You can also run `MshopApplication` from the IDE.

Open http://localhost:8888

The port is 8888. `application.yml` sets `server.port` to 8888.

Two API calls return data today. Those are the category names and the home rows. The product list, the single product, and the reviews still return `null`. The page shows "could not load" for a call that returns `null`.

## How a page loads

The server does not build the product cards. It sends an empty HTML page. The script in the browser fills that page.

1. You open `/`, `/category/Electronics`, or `/product/p-1001`.
2. `PageController` picks a Thymeleaf template. The template is a shell. It has a nav, a content area, and a script tag.
3. The script calls `/api/...`.
4. `getJSON` in `common.js` reads the body. An empty body or a failed status shows an error on the page.
5. The script writes the HTML for the links, the cards, and the reviews.

Every page loads the nav from `GET /api/categories`.

## Where the code lives

| Path | What it does |
| --- | --- |
| `PageController` | Returns the HTML shell for `/`, `/category/{name}`, and `/product/{id}` |
| `static/js` | Calls the API and draws the page |
| `data/products.json` | 200 products. 5 categories. 40 products in each category |
| `data/reviews.json` | Reviews. Each review points at a product with `productId` |
| `DataCenter` | Reads the JSON files with Jackson |
| `Product` and `Review` | The objects Jackson builds from those files |
| `ProductServiceImpl` | Holds the product list. Builds the category names and the home rows |
| `ProductApiController` | The product HTTP API |
| `ReviewApiController` | The review HTTP API. This method still returns `null` |
| `dto` | The JSON shapes the browser reads |

The home-row note is next to the service: [01_home_sections.md](src/main/java/com/mshop/web/api/service/impl/notes/01_home_sections.md).

## Pages

| URL | Template | What the script asks for |
| --- | --- | --- |
| `/` | `home.html` | `GET /api/home/sections?size=10` |
| `/category/{name}` | `category.html` | `GET /api/products?category={name}&page=&size=12` |
| `/product/{id}` | `product.html` | `GET /api/products/{id}` and `GET /api/products/{id}/reviews?page=0&size=5` |

`{name}` and `{id}` come from the URL. The category page reads `Electronics` from `/category/Electronics`. The product page reads `p-1001` from `/product/p-1001`.

## The data

A product looks like this:

```json
{
  "id": "p-1001",
  "name": "Orbit USB-C Hub",
  "category": "Electronics",
  "description": "Orbit USB-C Hub. Reliable performance and modern design.",
  "price": 7269,
  "imageUrl": "/images/placeholder.svg"
}
```

A card uses `id`, `name`, `category`, `price`, and `imageUrl`. `id` is the link to `/product/p-1001`. `category` is the small tag. The price is shown with a rupee sign. A missing `imageUrl` uses `/images/placeholder.svg`.

The product page also prints `description`.

A review looks like this:

```json
{
  "id": "r-5001",
  "productId": "p-1001",
  "author": "Arjun",
  "rating": 3,
  "comment": "Average quality, okay for the price.",
  "createdAt": "2025-12-30T15:40:00"
}
```

The review list prints `author`, `rating`, `comment`, and `createdAt`. `rating` becomes stars. `createdAt` becomes a date such as "30 Dec 2025".

## What the browser expects

Page numbers in the API start at 0. The first page is `page=0`.

The category link in the address bar starts at 1. `category.js` subtracts 1 before it calls the API. Page 2 in the address bar is `page=1` in the API.

A page of results has this shape:

```json
{
  "items": [],
  "page": 0,
  "size": 12,
  "totalPages": 4,
  "totalItems": 40
}
```

`items` is the rows for this page. `totalItems` is the full count. `totalPages` builds the page buttons. `common.js` hides the buttons when `totalPages` is 0 or 1.

### `GET /api/categories`

This call returns a JSON array of names. Each name is a string. There is no wrapper and no count.

```json
["Electronics", "Footwear"]
```

`common.js` turns each string into a nav link. The link goes to `/category/Electronics`.

`findAllCategories` builds this list. It reads the category off each product and keeps the first time that name appears.

This call works.

### `GET /api/home/sections?size=10`

This call returns a JSON array. One object is one row on the home page.

```json
[
  {
    "category": "Electronics",
    "totalItems": 40,
    "items": []
  }
]
```

`category` is the heading and the "View all" link. `totalItems` is the full count for that category. The page prints it as "View all (40)". `items` is the cards in that row. The page asks for `size=10` and draws every card in `items`, so return at most 10 products per row.

Return one object per category. An empty array shows "No products yet."

`findHomeSections` builds this list. It groups the products by category. `totalItems` is the size of that group. `items` is the first `size` products, mapped to `id`, `name`, `category`, `description`, `price`, and `imageUrl`.

This call works. The longer note is [01_home_sections.md](src/main/java/com/mshop/web/api/service/impl/notes/01_home_sections.md).

### `GET /api/products?category=&page=&size=`

This call returns one page of products. The category page calls it with `size=12`.

`category` is optional. With a category, `items` holds products from that category only. `totalItems` is the count for that category. Without a category, the page is the full catalog.

`category.js` reads `items`, `totalItems`, `page`, and `totalPages`. A bare array does not work. The script looks for `data.items`.

This method still returns `null`. The category page shows "could not load" until it returns the page object.

### `GET /api/products/{id}`

This call returns one product object. The same fields as a product in the JSON file.

`product.js` reads `name`, `imageUrl`, `category`, `price`, and `description` on the root object. A list, or a wrapper around the product, does not work. The script reads `p.name` on the body itself.

This method still returns `null`.

### `GET /api/products/{id}/reviews?page=&size=`

This call returns one page of reviews, plus a summary. The product page calls it with `size=5`.

```json
{
  "items": [],
  "page": 0,
  "size": 5,
  "totalPages": 2,
  "totalItems": 9,
  "summary": { "average": 4.2, "count": 9 }
}
```

`items` holds reviews for that product id. `summary.average` is the mean star rating. `summary.count` is how many reviews went into that mean. The page prints the big number from `average` and the line "9 reviews" from `count`. A count of 0 shows "No reviews yet."

`product.js` reads `summary`, `items`, `page`, and `totalPages`. The items are reviews. A list of products does not work here.

This method still returns `null`.
