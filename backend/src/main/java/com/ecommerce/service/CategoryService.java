package com.ecommerce.service;

import com.ecommerce.dto.CategoryDto;
import com.ecommerce.entity.Category;
import com.ecommerce.repository.CategoryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    // Metrics
    private final Counter categoryCreatedCounter;
    private final Counter categoryUpdatedCounter;
    private final Counter categoryViewedCounter;
    
    @Autowired
    public CategoryService(MeterRegistry meterRegistry) {
        this.categoryCreatedCounter = Counter.builder("categories.created")
            .description("Total number of categories created")
            .tag("service", "product-service")
            .register(meterRegistry);
        
        this.categoryUpdatedCounter = Counter.builder("categories.updated")
            .description("Total number of categories updated")
            .tag("service", "product-service")
            .register(meterRegistry);
        
        this.categoryViewedCounter = Counter.builder("categories.viewed")
            .description("Total number of category views")
            .tag("service", "product-service")
            .register(meterRegistry);
    }
    
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public Optional<CategoryDto> getCategoryById(Long id) {
        Optional<CategoryDto> result = categoryRepository.findById(id)
                .map(this::convertToDto);
        result.ifPresent(dto -> categoryViewedCounter.increment());
        return result;
    }
    
    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new RuntimeException("Category with name '" + categoryDto.getName() + "' already exists");
        }
        
        Category category = Category.builder()
                .name(categoryDto.getName())
                .description(categoryDto.getDescription())
                .build();
        
        Category savedCategory = categoryRepository.save(category);
        categoryCreatedCounter.increment();
        return convertToDto(savedCategory);
    }
    
    public Optional<CategoryDto> updateCategory(Long id, CategoryDto categoryDto) {
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    if (categoryDto.getName() != null && !categoryDto.getName().equals(existingCategory.getName())) {
                        if (categoryRepository.existsByName(categoryDto.getName())) {
                            throw new RuntimeException("Category with name '" + categoryDto.getName() + "' already exists");
                        }
                        existingCategory.setName(categoryDto.getName());
                    }
                    if (categoryDto.getDescription() != null) {
                        existingCategory.setDescription(categoryDto.getDescription());
                    }
                    
                    Category updatedCategory = categoryRepository.save(existingCategory);
                    categoryUpdatedCounter.increment();
                    return convertToDto(updatedCategory);
                });
    }
    
    public boolean deleteCategory(Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
