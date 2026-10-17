package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.HomeSection;
import com.mshop.web.api.dto.ProductResponse;
import com.mshop.web.api.dto.ProductReviewSummary;
import com.mshop.web.api.dto.ProductWrapper;
import com.mshop.web.api.modal.Product;
import com.mshop.web.api.modal.Review;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl {

    private final DataCenter dataCenter;
    private static List<Product> products;
    private static List<Review> reviews;

    @PostConstruct
    void init() {
        // the problem with this data read and initialization is if data is too heavy
        // it will delay whole application startup

        // copyonwritearraylist can be ignored as we are not updating product list
//        products = new CopyOnWriteArrayList<>(dataCenter.readProducts());
//        reviews = new CopyOnWriteArrayList<>(dataCenter.readReviews());

        var productsAsync = dataCenter.readProductsAsync;
        var reviewsAsync = dataCenter.readReviewsAsync;
        try {
            products = productsAsync.get();
            reviews = reviewsAsync.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> findAllCategories() {
        return products
                .stream()
                .map(Product::getCategory)
                .distinct()
                .toList();
    }

    public List<HomeSection> findHomeSections(int size) {
        System.out.println("Total size asked " + size);
        var products = ProductServiceImpl.products.stream().collect(Collectors.groupingBy(Product::getCategory));

        //optimize it with teeing collector
        return products
                .entrySet()
                .stream()
                .map(e -> {
                    var allProducts = e.getValue();
                    long allProductsCount = allProducts.size();
                    var limitedProducts = allProducts.stream()
                            .map(p -> new ProductResponse(
                                    p.getId(),
                                    p.getName(),
                                    p.getCategory(),
                                    p.getDescription(),
                                    p.getPrice(),
                                    p.getImageUrl()
                            ))
                            .limit(size)
                            .toList();

                    return new HomeSection(e.getKey(), allProductsCount, limitedProducts);
                }).toList();
    }

    public ProductWrapper findAllProducts(String category, int page, int size) {
        if (page < 0) page = 0;
//        var productsByCategory = products
//                .stream()
//                .filter(p -> (category == null || category.isBlank() || p.getCategory().equalsIgnoreCase(category)))
//                .toList();

        final Set<String> productIds = new HashSet<>();
        List<Product> productsByCategory;
        if (category == null || category.isBlank()) {
            productsByCategory = products;
        } else {
            productsByCategory = new ArrayList<>();
            products.forEach(p -> {
                if (p.getCategory().equalsIgnoreCase(category)) {
                    productsByCategory.add(p);
                    productIds.add(p.getId());
                }
            });
        }

        long totalItems = productsByCategory.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // TODO:  Read reviews average and total based on category products
        var res = reviews
                .stream()
                .filter(r -> productIds.contains(r.getProductId()))
                .collect(Collectors.summarizingDouble(Review::getRating));

        var productByCategoryPageSize = productsByCategory
                .stream()
                .skip(((long) page * size))
                .limit(size)
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getCategory(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getImageUrl()
                ))
                .toList();

        return new ProductWrapper(
                productByCategoryPageSize, page, size, totalPages, totalItems,
                new ProductReviewSummary(res.getAverage(), res.getCount())
        );
    }

    public ProductResponse findProductById(String id) {
        return products
                .stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getName(),
                        p.getCategory(),
                        p.getDescription(),
                        p.getPrice(),
                        p.getImageUrl()
                ))
                .orElseThrow(() -> new NoSuchElementException(String.format("Product with id %s not found", id)));
    }
}