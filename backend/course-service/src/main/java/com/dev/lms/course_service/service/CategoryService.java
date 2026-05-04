package com.dev.lms.course_service.service;

import com.dev.lms.course_service.dto.CategoryDto;
import com.dev.lms.course_service.dto.CreateCategoryRequest;
import com.dev.lms.course_service.dto.SubCategoryDto;
import com.dev.lms.course_service.dto.UpdateCategoryRequest;
import com.dev.lms.course_service.entity.Category;
import com.dev.lms.course_service.exception.BusinessException;
import com.dev.lms.course_service.exception.ResourceNotFoundException;
import com.dev.lms.course_service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(UUID id) {
        return toDto(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id)));
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new BusinessException("Category '" + request.name() + "' already exists");
        }
        Category saved = categoryRepository.save(Category.builder().name(request.name()).build());
        return toDto(saved);
    }

    @Transactional
    public CategoryDto update(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new BusinessException("Category '" + request.name() + "' already exists");
        }
        category.setName(request.name());
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(
                c.getId(),
                c.getName(),
                c.getSubCategories().stream()
                        .map(s -> new SubCategoryDto(s.getId(), s.getName(), c.getId(), c.getName()))
                        .toList()
        );
    }
}
