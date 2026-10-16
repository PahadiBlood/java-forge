# mShop

Spring Boot 3.5 · Java 21 · Thymeleaf · JSON files as data (no DB).

## Run
```
mvn spring-boot:run      # or run MshopApplication from your IDE
```
Open http://localhost:8080

The controllers are empty stubs (they return `null`), so pages show a
"could not load" notice until you implement the endpoints.

## Data
`src/main/resources/data/products.json` — 200 products, 5 categories (40 each)
`src/main/resources/data/reviews.json`  — reviews linked via `productId`

## Pages (`PageController`)
| URL | Template |
|---|---|
| `/` | home |
| `/category/{name}` | category |
| `/product/{id}` | product |

Pages are plain Thymeleaf shells; JS (`static/js`) fetches everything from the API.

## API the frontend expects
Paging is 0-based. A "page" response is:
`{ "items": [...], "page": 0, "size": 12, "totalPages": 4, "totalItems": 40 }`

| Endpoint | Response |
|---|---|
| `GET /api/categories` | `["Electronics", "Footwear", ...]` |
| `GET /api/home/sections?size=10` | `[{ "category": "...", "totalItems": 40, "items": [product, ...] }]` (home shows as many rows as you return) |
| `GET /api/products?category=&page=&size=` | page of products |
| `GET /api/products/{id}` | a single product |
| `GET /api/products/{id}/reviews?page=&size=` | page of reviews **plus** `"summary": { "average": 4.2, "count": 9 }` |

Product: `id, name, category, description, price, imageUrl`
Review: `id, productId, author, rating, comment, createdAt`
