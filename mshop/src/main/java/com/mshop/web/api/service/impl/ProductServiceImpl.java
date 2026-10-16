package com.mshop.web.api.service.impl;

import com.mshop.web.api.db.DataCenter;
import com.mshop.web.api.dto.HomeSection;
import com.mshop.web.api.dto.ProductResponse;
import com.mshop.web.api.modal.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl {

    private final DataCenter dataCenter;
    private static List<Product> PRODUCTS;

    @PostConstruct
    void init() {
        PRODUCTS = new CopyOnWriteArrayList<>(dataCenter.readProducts());
    }

    public List<String> findAllCategories() {
        return PRODUCTS
                .stream()
                .map(Product::getCategory)
                .distinct()
                .toList();
    }

    public List<HomeSection> findHomeSections(int size) {
        System.out.println("Total size asked " + size);
        var products = PRODUCTS.stream().collect(Collectors.groupingBy(Product::getCategory));

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
}
