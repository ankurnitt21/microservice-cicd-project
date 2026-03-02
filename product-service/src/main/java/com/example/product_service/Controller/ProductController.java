package com.example.product_service.Controller;

import com.example.product_service.exception.ProductNotFoundException;
import com.example.product_service.model.Product;
import com.example.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @Tag groups all endpoints in this controller under "Products" in Swagger UI
@Tag(name = "Products", description = "Product catalog management")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    // SLF4J logger — named after this class, so logger name = com.example.product_service.Controller.ProductController
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Get all products", description = "Returns a list of all products in the catalog")
    @ApiResponse(responseCode = "200", description = "List of products returned successfully")
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Request received: GET /api/products");
        List<Product> products = productService.getAllProducts();
        log.info("Returning {} product(s)", products.size());
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get product by ID", description = "Returns a single product by its unique ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found",
            content = @Content(schema = @Schema(implementation = Product.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @Parameter(description = "Unique ID of the product", example = "1", required = true)
            @PathVariable Long id) {

        log.info("Request received: GET /api/products/{}", id);
        Product product = productService.getProductById(id);

        if (product == null) {
            log.warn("Product not found: id={}", id);
            throw new ProductNotFoundException(id);
        }

        log.info("Found product: id={}, name={}", product.getId(), product.getName());
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "Create a new product", description = "Adds a new product to the catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully",
            content = @Content(schema = @Schema(implementation = Product.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — check errors in response",
            content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody @Valid Product product) {

        log.info("Request received: POST /api/products — name={}, price={}", product.getName(), product.getPrice());
        Product createdProduct = productService.createProduct(product);
        log.info("Product created: id={}, name={}", createdProduct.getId(), createdProduct.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @Operation(summary = "Update product by ID", description = "Replaces all fields of an existing product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(hidden = true)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "Unique ID of the product to update", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid Product product) {

        log.info("Request received: PUT /api/products/{}", id);

        if (productService.getProductById(id) == null) {
            log.warn("Update failed — product not found: id={}", id);
            throw new ProductNotFoundException(id);
        }

        Product updatedProduct = productService.updateProduct(id, product);
        log.info("Product updated: id={}, name={}", updatedProduct.getId(), updatedProduct.getName());
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Delete product by ID", description = "Removes a product from the catalog")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found",
            content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Unique ID of the product to delete", example = "1")
            @PathVariable Long id) {

        log.info("Request received: DELETE /api/products/{}", id);

        if (productService.getProductById(id) == null) {
            log.warn("Delete failed — product not found: id={}", id);
            throw new ProductNotFoundException(id);
        }

        productService.deleteProduct(id);
        log.info("Product deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
