package com.example.product_service.service;

import java.util.List;
import java.util.Map;

import com.example.product_service.model.Product;
import java.util.HashMap;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    // SLF4J logger — logger name = com.example.product_service.service.ProductService
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final Map<Long, Product> productStore = new HashMap<>();
    private long idCounter = 1;


    public List<Product> getAllProducts() {
        log.debug("Fetching all products from store — current count: {}", productStore.size());
        return new ArrayList<>(productStore.values());
    }

    public Product getProductById(Long id) {
        log.debug("Looking up product by id={}", id);
        Product product = productStore.get(id);
        if (product == null) {
            log.debug("Cache miss — no product found for id={}", id);
        }
        return product;
    }

    public Product createProduct(Product product) {
        product.setId(idCounter++);
        productStore.put(product.getId(), product);
        log.debug("Saved product to store: id={}, name={}, price={}", product.getId(), product.getName(), product.getPrice());
        return product;
    }

    public Product updateProduct(Long id, Product product) {
        if (!productStore.containsKey(id)) {
            log.debug("Update skipped — product not in store: id={}", id);
            return null;
        }
        product.setId(id);  // ensure id is set from path variable, not from body
        productStore.put(id, product);
        log.debug("Updated product in store: id={}, name={}", id, product.getName());
        return product;
    }

    public void deleteProduct(Long id) {
        productStore.remove(id);
        log.debug("Removed product from store: id={}", id);
    }

}
