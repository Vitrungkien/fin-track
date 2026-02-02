package com.finance.tracker.service;

import com.finance.tracker.dto.request.CategoryRequest;
import com.finance.tracker.dto.response.CategoryResponse;
import com.finance.tracker.entity.Category;
import com.finance.tracker.entity.TransactionType;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(TransactionType type) {
        User user = getCurrentUser();
        List<Category> categories = (type == null)
                ? categoryRepository.findByUserId(user.getId())
                : categoryRepository.findByUserIdAndType(user.getId(), type);

        return categories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        User user = getCurrentUser();

        if (categoryRepository.existsByUserIdAndNameAndType(user.getId(), request.getName(), request.getType())) {
            throw new IllegalArgumentException("Category with this name already exists for this type");
        }

        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .color(request.getColor())
                .type(request.getType())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = getCategoryById(id);

        // Check uniqueness if name or type changed
        if (!category.getName().equals(request.getName()) || category.getType() != request.getType()) {
            if (categoryRepository.existsByUserIdAndNameAndType(category.getUser().getId(), request.getName(),
                    request.getType())) {
                throw new IllegalArgumentException("Category with this name already exists");
            }
        }

        category.setName(request.getName());
        category.setColor(request.getColor());
        category.setType(request.getType());

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        // TODO: Check for existing transactions before deletion
        categoryRepository.delete(category);
    }

    private Category getCategoryById(Long id) {
        User user = getCurrentUser();
        return categoryRepository.findById(id)
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Category not found or access denied"));
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .type(category.getType())
                .build();
    }
}
