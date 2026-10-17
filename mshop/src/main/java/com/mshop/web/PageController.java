package com.mshop.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    // home page
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // full category page
    @GetMapping("/category/{name}")
    public String category(@PathVariable String name) {
        return "category";
    }

    @GetMapping("/search")
    public String search() {
        return "search";
    }

    // product details page
    @GetMapping("/product/{id}")
    public String product(@PathVariable String id) {
        return "product";
    }
}
