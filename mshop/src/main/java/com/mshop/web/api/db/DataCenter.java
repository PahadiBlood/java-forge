package com.mshop.web.api.db;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mshop.web.api.modal.Order;
import com.mshop.web.api.modal.Product;
import com.mshop.web.api.modal.Review;
import com.mshop.web.api.modal.User;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Component
public final class DataCenter {
    private static final int LIMIT = 10;
    private final ExecutorService service = Executors.newFixedThreadPool(2);
    private final ObjectMapper mapper;

    @Getter
    private final List<Product> products = new CopyOnWriteArrayList<>();
    @Getter
    private final List<Review> reviews = new CopyOnWriteArrayList<>();
    @Getter
    private final List<Order> orders;
    @Getter
    private final List<User> users;

    @PostConstruct
    void init() {
        try {
            var productsAsync = CompletableFuture.supplyAsync(() -> {
                try {
                    return readFirstProducts();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, service).thenAccept(products::addAll);

            var reviewsAsync = CompletableFuture.supplyAsync(() -> {
                try {
                    return readFirstReviews();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, service).thenAccept(reviews::addAll);

            CompletableFuture.allOf(productsAsync, reviewsAsync).join();
            loadRestData();
        } catch (Exception e) {
            service.shutdown();
            throw new RuntimeException(e);
        }
    }

    // Reads only the first `limit` objects, then stops
    private List<Product> readFirstProducts() throws IOException {
        var products = new ArrayList<Product>();
        try (JsonParser parser = mapper.getFactory().createParser(new ClassPathResource("data/products.json").getInputStream())) {
            parser.nextToken();
            while (parser.nextToken() == JsonToken.START_OBJECT && products.size() < LIMIT) {
                products.add(mapper.readValue(parser, Product.class));
            }
        }
        return products;
    }

    // Reads only the rest objects, then stops
    private List<Product> readRestProducts() throws IOException {
        var products = new ArrayList<Product>();
        try (JsonParser parser = mapper.getFactory().createParser(new ClassPathResource("data/products.json").getInputStream())) {
            parser.nextToken();
            int i = 0;
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                if (i++ < LIMIT) {
                    parser.skipChildren(); // jump over this object without mapping
                } else {
                    products.add(mapper.readValue(parser, Product.class));
                }
            }
        }
        return products;
    }

    // Reads only the first `limit` objects, then stops
    private List<Review> readFirstReviews() throws IOException {
        var reviews = new ArrayList<Review>();
        try (JsonParser parser = mapper.getFactory().createParser(new ClassPathResource("data/reviews.json").getInputStream())) {
            parser.nextToken();
            while (parser.nextToken() == JsonToken.START_OBJECT && reviews.size() < LIMIT) {
                reviews.add(mapper.readValue(parser, Review.class));
            }
        }
        return reviews;
    }

    // Reads only the first `limit` objects, then stops
    private List<Review> readRestReviews() throws IOException {
        var reviews = new ArrayList<Review>();
        try (JsonParser parser = mapper.getFactory().createParser(new ClassPathResource("data/reviews.json").getInputStream())) {
            parser.nextToken();
            int i = 0;
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                if (i++ < LIMIT) {
                    parser.skipChildren();
                } else {
                    reviews.add(mapper.readValue(parser, Review.class));
                }
            }
        }
        return reviews;
    }

    private List<User> readUsers() throws IOException {
        var users = new ArrayList<User>();
        try (JsonParser parser = mapper.getFactory().createParser(new ClassPathResource("data/users.json").getInputStream())) {
            parser.nextToken();
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                users.add(mapper.readValue(parser, User.class));
            }
        }
        return users;
    }

    private List<Order> readOrders() throws IOException {
        var orders = new ArrayList<Order>();
        try (JsonParser parser = mapper.getFactory().createParser(new ClassPathResource("data/orders.json").getInputStream())) {
            parser.nextToken();
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                orders.add(mapper.readValue(parser, Order.class));
            }
        }
        return orders;
    }

    public void loadRestData() {
        var productsAsync = CompletableFuture.supplyAsync(() -> {
            try {
                return readRestProducts();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, service).thenAccept(products::addAll);

        var reviewsAsync = CompletableFuture.supplyAsync(() -> {
            try {
                return readRestReviews();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, service).thenAccept(reviews::addAll);

        var usersAsync = CompletableFuture.supplyAsync(() -> {
            try {
                return readUsers();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, service).thenAccept(users::addAll);

        var ordersAsync = CompletableFuture.supplyAsync(() -> {
            try {
                return readOrders();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, service).thenAccept(orders::addAll);

        CompletableFuture.allOf(productsAsync, reviewsAsync, usersAsync, ordersAsync)
                .whenComplete((result, ex) -> {
                    service.shutdown();
                    if (ex != null) {
                        ex.printStackTrace();
                    }
                });
    }
}
