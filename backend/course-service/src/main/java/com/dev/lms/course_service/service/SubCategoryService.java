package com.dev.lms.course_service.service;

import com.dev.lms.course_service.dto.CreateSubCategoryRequest;
import com.dev.lms.course_service.dto.SubCategoryDto;
import com.dev.lms.course_service.dto.UpdateSubCategoryRequest;
import com.dev.lms.course_service.entity.Category;
import com.dev.lms.course_service.entity.SubCategory;
import com.dev.lms.course_service.exception.BusinessException;
import com.dev.lms.course_service.exception.ResourceNotFoundException;
import com.dev.lms.course_service.repository.CategoryRepository;
import com.dev.lms.course_service.repository.SubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubCategoryService {

    private final SubCategoryRepository subCategoryRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public SubCategoryDto create(CreateSubCategoryRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        if (subCategoryRepository.existsByNameAndCategoryId(request.name(), request.categoryId())) {
            throw new BusinessException("Subcategory with name '" + request.name() + "' already exists in this category");
        }

        SubCategory subCategory = SubCategory.builder()
                .name(request.name())
                .category(category)
                .build();

        SubCategory saved = subCategoryRepository.save(subCategory);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SubCategoryDto getById(UUID id) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory", id));
        return toResponse(subCategory);
    }

    @Transactional(readOnly = true)
    public List<SubCategoryDto> getAll() {
        return subCategoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubCategoryDto> getByCategoryId(UUID categoryId) {
        return subCategoryRepository.findByCategoryId(categoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SubCategoryDto update(UUID id, UpdateSubCategoryRequest request) {
        SubCategory subCategory = subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubCategory", id));

        if (subCategoryRepository.existsByNameAndCategoryId(request.name(), subCategory.getCategory().getId())
                && !subCategory.getName().equals(request.name())) {
            throw new BusinessException("Subcategory with name '" + request.name() + "' already exists in this category");
        }

        subCategory.setName(request.name());
        SubCategory saved = subCategoryRepository.save(subCategory);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!subCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("SubCategory", id);
        }
        subCategoryRepository.deleteById(id);
    }

    private SubCategoryDto toResponse(SubCategory subCategory) {
        return new SubCategoryDto(
                subCategory.getId(),
                subCategory.getName(),
                subCategory.getCategory().getId(),
                subCategory.getCategory().getName()
        );
    }
}
