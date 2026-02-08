package com.finance.tracker.service;

import com.finance.tracker.dto.response.ChartDataResponse;
import com.finance.tracker.dto.response.DashboardSummaryResponse;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.entity.TransactionType;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final TransactionRepository transactionRepository;
        private final UserRepository userRepository;

        private User getCurrentUser() {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();
                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        }

        @Transactional(readOnly = true)
        public DashboardSummaryResponse getSummary(Integer month, Integer year) {
                User user = getCurrentUser();
                LocalDate startDate;
                LocalDate endDate;

                if (month != null && year != null) {
                        startDate = LocalDate.of(year, month, 1);
                        endDate = startDate.plusMonths(1).minusDays(1);
                } else {
                        // Default to current month
                        LocalDate now = LocalDate.now();
                        startDate = now.withDayOfMonth(1);
                        endDate = now.withDayOfMonth(now.lengthOfMonth());
                }

                List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                                user.getId(), startDate, endDate);

                BigDecimal totalIncome = transactions.stream()
                                .filter(t -> t.getType() == TransactionType.INCOME)
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpense = transactions.stream()
                                .filter(t -> t.getType() == TransactionType.EXPENSE)
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return DashboardSummaryResponse.builder()
                                .month(YearMonth.from(startDate).toString())
                                .totalIncome(totalIncome)
                                .totalExpense(totalExpense)
                                .balance(totalIncome.subtract(totalExpense))
                                .transactionCount((long) transactions.size())
                                .build();
        }

        @Transactional(readOnly = true)
        public ChartDataResponse getCategoryChartData(Integer month, Integer year) {
                User user = getCurrentUser();
                // Similar logic for dates, logic duplicate could be refactored
                LocalDate startDate;
                LocalDate endDate;

                if (month != null && year != null) {
                        startDate = LocalDate.of(year, month, 1);
                        endDate = startDate.plusMonths(1).minusDays(1);
                } else {
                        LocalDate now = LocalDate.now();
                        startDate = now.withDayOfMonth(1);
                        endDate = now.withDayOfMonth(now.lengthOfMonth());
                }

                List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                                user.getId(), startDate, endDate);

                // Group by Category (Only Expenses usually makes sense for pie chart)
                Map<String, BigDecimal> categoryTotals = transactions.stream()
                                .filter(t -> t.getType() == TransactionType.EXPENSE)
                                .collect(Collectors.groupingBy(
                                                t -> t.getCategory().getName() + "|" + t.getCategory().getColor(),
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount,
                                                                BigDecimal::add)));

                List<String> labels = categoryTotals.keySet().stream()
                                .map(k -> k.split("\\|")[0])
                                .collect(Collectors.toList());

                List<String> colors = categoryTotals.keySet().stream()
                                .map(k -> k.split("\\|")[1])
                                .collect(Collectors.toList());

                List<BigDecimal> data = categoryTotals.values().stream()
                                .collect(Collectors.toList());

                return ChartDataResponse.builder()
                                .labels(labels)
                                .data(data)
                                .colors(colors)
                                .build();
        }

        @Transactional(readOnly = true)
        public ChartDataResponse getDailyChartData(Integer month, Integer year) {
                User user = getCurrentUser();
                LocalDate startDate;
                LocalDate endDate;

                if (month != null && year != null) {
                        startDate = LocalDate.of(year, month, 1);
                        endDate = startDate.plusMonths(1).minusDays(1);
                } else {
                        LocalDate now = LocalDate.now();
                        startDate = now.withDayOfMonth(1);
                        endDate = now.withDayOfMonth(now.lengthOfMonth());
                }

                List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                                user.getId(), startDate, endDate);

                Map<Integer, BigDecimal> dailyTotals = transactions.stream()
                                .filter(t -> t.getType() == TransactionType.EXPENSE)
                                .collect(Collectors.groupingBy(
                                                t -> t.getTransactionDate().getDayOfMonth(),
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount,
                                                                BigDecimal::add)));

                // Create a list for all days
                List<String> labels = startDate.datesUntil(endDate.plusDays(1))
                                .map(d -> String.valueOf(d.getDayOfMonth()))
                                .collect(Collectors.toList());

                List<BigDecimal> data = startDate.datesUntil(endDate.plusDays(1))
                                .map(d -> dailyTotals.getOrDefault(d.getDayOfMonth(), BigDecimal.ZERO))
                                .collect(Collectors.toList());

                return ChartDataResponse.builder()
                                .labels(labels)
                                .data(data)
                                .colors(null) // Not needed for line/bar chart usually, handled by frontend
                                .build();
        }

        @Transactional(readOnly = true)
        public List<com.finance.tracker.dto.response.CategoryExpenseSummary> getCategoryExpenseSummary(Integer month,
                        Integer year) {
                User user = getCurrentUser();
                LocalDate startDate;
                LocalDate endDate;

                if (month != null && year != null) {
                        startDate = LocalDate.of(year, month, 1);
                        endDate = startDate.plusMonths(1).minusDays(1);
                } else {
                        LocalDate now = LocalDate.now();
                        startDate = now.withDayOfMonth(1);
                        endDate = now.withDayOfMonth(now.lengthOfMonth());
                }

                List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                                user.getId(), startDate, endDate);

                BigDecimal totalExpense = transactions.stream()
                                .filter(t -> t.getType() == TransactionType.EXPENSE)
                                .map(Transaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<com.finance.tracker.entity.Category, BigDecimal> categoryTotals = transactions.stream()
                                .filter(t -> t.getType() == TransactionType.EXPENSE)
                                .collect(Collectors.groupingBy(
                                                Transaction::getCategory,
                                                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount,
                                                                BigDecimal::add)));

                return categoryTotals.entrySet().stream()
                                .map(entry -> {
                                        BigDecimal amount = entry.getValue();
                                        double percentage = totalExpense.compareTo(BigDecimal.ZERO) > 0
                                                        ? amount.divide(totalExpense, 4, java.math.RoundingMode.HALF_UP)
                                                                        .multiply(new BigDecimal("100")).doubleValue()
                                                        : 0.0;

                                        return com.finance.tracker.dto.response.CategoryExpenseSummary.builder()
                                                        .categoryId(entry.getKey().getId())
                                                        .categoryName(entry.getKey().getName())
                                                        .color(entry.getKey().getColor())
                                                        .icon(entry.getKey().getIcon())
                                                        .totalAmount(amount)
                                                        .percentage(percentage)
                                                        .build();
                                })
                                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                                .collect(Collectors.toList());
        }
}
