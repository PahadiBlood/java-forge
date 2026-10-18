package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.*;
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
        if (size < 1) size = 15;
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

    public ProductSearchWrapper searchProducts(int page, int size, String sort, String search) {
        if (page < 0) page = 0;
        if (size < 1) size = 15;

        search = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        var searchProducts = getSearchProducts(sort, search);
        var productsResponse = searchProducts
                .stream()
                .skip((long) page * size)
                .limit(size)
                .map(p ->
                        new ProductResponse(
                                p.getId(),
                                p.getName(),
                                p.getCategory(),
                                p.getDescription(),
                                p.getPrice(),
                                p.getImageUrl()
                        )
                )
                .toList();

        long totalItems = searchProducts.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        return new ProductSearchWrapper(productsResponse, page, size, totalPages, totalItems);
    }

    private static List<Product> getSearchProducts(String sort, String search) {
        var searchChain = products
                .stream()
                .filter(product -> !search.isBlank() && (
                        product.getName().toLowerCase(Locale.ROOT).contains(search)
                                || product.getCategory().toLowerCase(Locale.ROOT).contains(search)
                                || product.getDescription().toLowerCase(Locale.ROOT).contains(search)
                ));

        if (sort != null && !sort.isBlank() && !"relevance".equalsIgnoreCase(sort)) {
            Comparator<Product> sortFunction = switch (sort.toLowerCase(Locale.ROOT)) {
                case "name-asc" -> (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());
                case "price-asc" -> Comparator.comparingDouble(Product::getPrice);
                case "price-desc" -> Comparator.comparingDouble(Product::getPrice).reversed();
                default -> null;
            };

            if (sortFunction != null) {
                searchChain = searchChain.sorted(sortFunction);
            }
        } else {
            searchChain = searchChain
                    .sorted(Comparator.comparingInt((Product product) -> relevance(product, search))
                            .thenComparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
        }

        return searchChain.toList();
    }

    private static int relevance(Product product, String search) {
        String name = product.getName().toLowerCase(Locale.ROOT);
        String category = product.getCategory().toLowerCase(Locale.ROOT);
        if (name.equals(search)) return 0;
        if (name.startsWith(search)) return 1;
        if (name.contains(search)) return 2;
        if (category.contains(search)) return 3;
        return 4;
    }
}
