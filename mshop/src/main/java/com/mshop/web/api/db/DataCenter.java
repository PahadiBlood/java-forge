package com.mshop.web.api.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mshop.web.api.modal.Product;
import com.mshop.web.api.modal.Review;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DataCenter {

    private final ExecutorService service = Executors.newFixedThreadPool(2);

    private final ObjectMapper objectMapper;
    public final CompletableFuture<List<Product>> readProductsAsync;
    public final CompletableFuture<List<Review>> readReviewsAsync;

    public DataCenter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.readProductsAsync = CompletableFuture.supplyAsync(this::readProducts, service);
        this.readReviewsAsync = CompletableFuture.supplyAsync(this::readReviews, service);
    }

    public List<Product> readProducts() {
        try (InputStream in = new ClassPathResource("data/products.json").getInputStream()) {
            List<Product> products = objectMapper.readValue(in, new TypeReference<>() {
            });
            return Collections.unmodifiableList(products);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Review> readReviews() {
        try (InputStream in = new ClassPathResource("data/reviews.json").getInputStream()) {
            List<Review> products = objectMapper.readValue(in, new TypeReference<>() {
            });
            return Collections.unmodifiableList(products);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
