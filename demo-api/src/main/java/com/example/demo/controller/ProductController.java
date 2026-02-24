package com.example.demo.controller;

import com.example.demo.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST controller for Product CRUD operations.
 * Uses in-memory storage for demo purposes.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ProductController() {
        products.put(1L, new Product(1L, "Laptop", "MacBook Pro M3", 2499.99));
        products.put(2L, new Product(2L, "Mouse", "Logitech MX Master 3", 99.99));
        products.put(3L, new Product(3L, "Keyboard", "Keychron K2", 79.99));
        idGenerator.set(4);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Getting all products");
        return ResponseEntity.ok(new ArrayList<>(products.values()));
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        log.info("Creating product: {}", product.getName());
        long id = idGenerator.getAndIncrement();
        product.setId(id);
        products.put(id, product);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }
}
