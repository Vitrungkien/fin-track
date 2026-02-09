package com.finance.tracker.service;

import com.finance.tracker.dto.response.CategoryExpenseStats;
import com.finance.tracker.dto.response.MonthlyReportResponse;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.entity.TransactionType;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(Integer month, Integer year) {
        User user = getCurrentUser();
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month != null && year != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            startDate = start.atStartOfDay();
            endDate = start.plusMonths(1).minusDays(1).atTime(LocalTime.MAX);
        } else {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1).atStartOfDay();
            endDate = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);
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

        Map<String, BigDecimal> expenseByCategory = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        List<CategoryExpenseStats> topCategories = expenseByCategory.entrySet().stream()
                .map(entry -> {
                    double percentage = 0.0;
                    if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                        percentage = entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP).doubleValue() * 100;
                    }
                    return CategoryExpenseStats.builder()
                            .categoryName(entry.getKey())
                            .amount(entry.getValue())
                            .percentage(percentage)
                            .build();
                })
                .sorted(Comparator.comparing(CategoryExpenseStats::getAmount).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return MonthlyReportResponse.builder()
                .month(YearMonth.from(startDate).toString())
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .topExpenseCategories(topCategories)
                .build();
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportToExcel(Integer month, Integer year) throws IOException {
        User user = getCurrentUser();
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month != null && year != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            startDate = start.atStartOfDay();
            endDate = start.plusMonths(1).minusDays(1).atTime(LocalTime.MAX);
        } else {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1).atStartOfDay();
            endDate = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);
        }

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                user.getId(), startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");

            // Header Request
            Row headerRow = sheet.createRow(0);
            String[] columns = { "Date", "Type", "Category", "Amount", "Note" };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            int rowIdx = 1;
            for (Transaction transaction : transactions) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(transaction.getTransactionDate().toString());
                row.createCell(1).setCellValue(transaction.getType().toString());
                row.createCell(2).setCellValue(transaction.getCategory().getName());
                row.createCell(3).setCellValue(transaction.getAmount().doubleValue());
                row.createCell(4).setCellValue(transaction.getNote());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Transactional(readOnly = true)
    public ByteArrayInputStream exportToCsv(Integer month, Integer year) throws IOException {
        User user = getCurrentUser();
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month != null && year != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            startDate = start.atStartOfDay();
            endDate = start.plusMonths(1).minusDays(1).atTime(LocalTime.MAX);
        } else {
            LocalDate now = LocalDate.now();
            startDate = now.withDayOfMonth(1).atStartOfDay();
            endDate = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);
        }

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                user.getId(), startDate, endDate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder csv = new StringBuilder();
        csv.append("Date,Type,Category,Amount,Note\n");

        for (Transaction t : transactions) {
            csv.append(String.format("%s,%s,%s,%.2f,\"%s\"\n",
                    t.getTransactionDate(),
                    t.getType(),
                    t.getCategory().getName(),
                    t.getAmount(),
                    t.getNote() != null ? t.getNote().replace("\"", "\"\"") : ""));
        }

        out.write(csv.toString().getBytes());
        return new ByteArrayInputStream(out.toByteArray());
    }
}
