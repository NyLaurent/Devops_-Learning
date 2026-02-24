package com.ecommerce.service;

import com.ecommerce.dto.ProductDto;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    // Metrics
    private final Counter productCreatedCounter;
    private final Counter productUpdatedCounter;
    private final Counter productViewedCounter;
    private final Timer productQueryTimer;
    private final MeterRegistry meterRegistry;
    
    @Autowired
    public ProductService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.productCreatedCounter = Counter.builder("products.created")
            .description("Total number of products created")
            .tag("service", "product-service")
            .register(meterRegistry);
        
        this.productUpdatedCounter = Counter.builder("products.updated")
            .description("Total number of products updated")
            .tag("service", "product-service")
            .register(meterRegistry);
        
        this.productViewedCounter = Counter.builder("products.viewed")
            .description("Total number of product views")
            .tag("service", "product-service")
            .register(meterRegistry);
        
        this.productQueryTimer = Timer.builder("products.query.duration")
            .description("Product query execution time")
            .tag("service", "product-service")
            .register(meterRegistry);
    }
    
    public List<ProductDto> getAllProducts() {
        return productRepository.findByIsActiveTrue()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(this::convertToDto);
    }
    
    public Optional<ProductDto> getProductById(Long id) {
        Sample sample = Timer.start(meterRegistry);
        try {
            Optional<ProductDto> result = productRepository.findById(id)
                    .filter(Product::getIsActive)
                    .map(this::convertToDto);
            result.ifPresent(dto -> productViewedCounter.increment());
            return result;
        } finally {
            sample.stop(productQueryTimer);
        }
    }
    
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Page<ProductDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable)
                .map(this::convertToDto);
    }
    
    public List<ProductDto> searchProducts(String searchTerm) {
        return productRepository.searchProducts(searchTerm)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Page<ProductDto> searchProducts(String searchTerm, Pageable pageable) {
        return productRepository.searchProducts(searchTerm, pageable)
                .map(this::convertToDto);
    }
    
    public List<ProductDto> searchProductsByCategory(Long categoryId, String searchTerm) {
        return productRepository.searchProductsByCategory(categoryId, searchTerm)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Page<ProductDto> searchProductsByCategory(Long categoryId, String searchTerm, Pageable pageable) {
        return productRepository.searchProductsByCategory(categoryId, searchTerm, pageable)
                .map(this::convertToDto);
    }
    
    public ProductDto createProduct(ProductDto productDto) {
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDto.getCategoryId()));
        
        Product product = Product.builder()
                .name(productDto.getName())
                .description(productDto.getDescription())
                .price(productDto.getPrice())
                .stockQuantity(productDto.getStockQuantity())
                .imageUrl(productDto.getImageUrl())
                .isActive(productDto.getIsActive() != null ? productDto.getIsActive() : true)
                .category(category)
                .build();
        
        Product savedProduct = productRepository.save(product);
        productCreatedCounter.increment();
        return convertToDto(savedProduct);
    }
    
    public Optional<ProductDto> updateProduct(Long id, ProductDto productDto) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    if (productDto.getName() != null) {
                        existingProduct.setName(productDto.getName());
                    }
                    if (productDto.getDescription() != null) {
                        existingProduct.setDescription(productDto.getDescription());
                    }
                    if (productDto.getPrice() != null) {
                        existingProduct.setPrice(productDto.getPrice());
                    }
                    if (productDto.getStockQuantity() != null) {
                        existingProduct.setStockQuantity(productDto.getStockQuantity());
                    }
                    if (productDto.getImageUrl() != null) {
                        existingProduct.setImageUrl(productDto.getImageUrl());
                    }
                    if (productDto.getIsActive() != null) {
                        existingProduct.setIsActive(productDto.getIsActive());
                    }
                    if (productDto.getCategoryId() != null) {
                        Category category = categoryRepository.findById(productDto.getCategoryId())
                                .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDto.getCategoryId()));
                        existingProduct.setCategory(category);
                    }
                    
                    Product updatedProduct = productRepository.save(existingProduct);
                    productUpdatedCounter.increment();
                    return convertToDto(updatedProduct);
                });
    }
    
    public boolean deleteProduct(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setIsActive(false);
                    productRepository.save(product);
                    return true;
                })
                .orElse(false);
    }
    
    private ProductDto convertToDto(Product product) {
        return ProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .build();
    }
}
