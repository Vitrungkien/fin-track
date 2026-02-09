package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.entity.Category;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.entity.TransactionType;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long categoryId, TransactionType type,
            Integer month, Integer year, Pageable pageable) {
        User user = getCurrentUser();
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (month != null && year != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            startDate = start.atStartOfDay();
            endDate = start.plusMonths(1).minusDays(1).atTime(23, 59, 59);
        }

        Page<Transaction> transactions = transactionRepository.findWithFilters(
                user.getId(), categoryId, type, startDate, endDate, pageable);

        return transactions.map(this::mapToResponse);
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        User user = getCurrentUser();
        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(c -> c.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Category not found or access denied"));

        if (category.getType() != request.getType()) {
            throw new IllegalArgumentException("Category type does not match transaction type");
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .category(category)
                .amount(request.getAmount())
                .type(request.getType())
                .transactionDate(request.getTransactionDate())
                .note(request.getNote())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest request) {
        Transaction transaction = getTransactionById(id);

        Category category = categoryRepository.findById(request.getCategoryId())
                .filter(c -> c.getUser().getId().equals(transaction.getUser().getId()))
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (category.getType() != request.getType()) {
            throw new IllegalArgumentException("Category type does not match transaction type");
        }

        transaction.setCategory(category);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setNote(request.getNote());

        return mapToResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = getTransactionById(id);
        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id) {
        return mapToResponse(getTransactionById(id));
    }

    private Transaction getTransactionById(Long id) {
        User user = getCurrentUser();
        return transactionRepository.findById(id)
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found or access denied"));
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .categoryId(transaction.getCategory().getId())
                .categoryName(transaction.getCategory().getName())
                .categoryColor(transaction.getCategory().getColor())
                .categoryIcon(transaction.getCategory().getIcon())
                .transactionDate(transaction.getTransactionDate())
                .note(transaction.getNote())
                .build();
    }
}
