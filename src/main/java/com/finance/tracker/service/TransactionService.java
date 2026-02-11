package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransactionRequest;
import com.finance.tracker.dto.response.ImportResult;
import com.finance.tracker.dto.response.TransactionResponse;
import com.finance.tracker.entity.Category;
import com.finance.tracker.entity.Transaction;
import com.finance.tracker.entity.TransactionType;
import com.finance.tracker.entity.User;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

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
            Integer month, Integer year, String keyword, Pageable pageable) {
        User user = getCurrentUser();
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (month != null && year != null) {
            LocalDate start = LocalDate.of(year, month, 1);
            startDate = start.atStartOfDay();
            endDate = start.plusMonths(1).minusDays(1).atTime(23, 59, 59);
        }

        Page<Transaction> transactions = transactionRepository.findWithFilters(
                user.getId(), categoryId, type, startDate, endDate, keyword, pageable);

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

    /**
     * Import transactions from Excel file
     * Expected columns: Date, Type, Category, Amount, Note
     */
    @Transactional
    public ImportResult importFromExcel(MultipartFile file) throws IOException {
        User user = getCurrentUser();
        ImportResult result = ImportResult.builder()
                .totalRows(0)
                .successCount(0)
                .errorCount(0)
                .errors(new ArrayList<>())
                .importedTransactions(new ArrayList<>())
                .build();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int rowNum = 0;

            for (Row row : sheet) {
                rowNum++;
                // Skip header row
                if (rowNum == 1)
                    continue;

                // Skip empty rows
                if (isRowEmpty(row))
                    continue;

                result.setTotalRows(result.getTotalRows() + 1);

                try {
                    Transaction transaction = parseRowToTransaction(row, user, rowNum);
                    Transaction saved = transactionRepository.save(transaction);
                    result.getImportedTransactions().add(mapToResponse(saved));
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception e) {
                    result.setErrorCount(result.getErrorCount() + 1);
                    result.getErrors().add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * Generate Excel template for transaction import
     */
    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = { "Date (YYYY-MM-DD)", "Type (INCOME/EXPENSE)", "Category Name", "Amount", "Note" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256); // 20 characters wide
            }

            // Add sample data rows
            addSampleRow(sheet, 1, "2026-02-11", "EXPENSE", "Food & Dining", "150000", "Lunch with team");
            addSampleRow(sheet, 2, "2026-02-10", "INCOME", "Salary", "15000000", "Monthly salary");
            addSampleRow(sheet, 3, "2026-02-09", "EXPENSE", "Transportation", "50000", "Taxi to office");

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void addSampleRow(Sheet sheet, int rowNum, String date, String type, String category, String amount,
            String note) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(date);
        row.createCell(1).setCellValue(type);
        row.createCell(2).setCellValue(category);
        row.createCell(3).setCellValue(amount);
        row.createCell(4).setCellValue(note);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null)
            return true;

        for (int i = 0; i < 5; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private Transaction parseRowToTransaction(Row row, User user, int rowNum) {
        try {
            // Parse date (column 0)
            String dateStr = getCellValueAsString(row.getCell(0));
            if (dateStr == null || dateStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Date is required");
            }
            LocalDateTime transactionDate = parseDate(dateStr);

            // Parse type (column 1)
            String typeStr = getCellValueAsString(row.getCell(1));
            if (typeStr == null || typeStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Type is required");
            }
            TransactionType type;
            try {
                type = TransactionType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid type. Must be INCOME or EXPENSE");
            }

            // Parse category (column 2)
            String categoryName = getCellValueAsString(row.getCell(2));
            if (categoryName == null || categoryName.trim().isEmpty()) {
                throw new IllegalArgumentException("Category name is required");
            }

            Category category = categoryRepository.findByUserAndName(user, categoryName.trim())
                    .orElseThrow(() -> new IllegalArgumentException("Category '" + categoryName + "' not found"));

            if (category.getType() != type) {
                throw new IllegalArgumentException(
                        "Category '" + categoryName + "' type does not match transaction type");
            }

            // Parse amount (column 3)
            String amountStr = getCellValueAsString(row.getCell(3));
            if (amountStr == null || amountStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Amount is required");
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(amountStr.trim().replace(",", ""));
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Amount must be greater than 0");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid amount format");
            }

            // Parse note (column 4) - optional
            String note = getCellValueAsString(row.getCell(4));

            return Transaction.builder()
                    .user(user)
                    .category(category)
                    .amount(amount)
                    .type(type)
                    .transactionDate(transactionDate)
                    .note(note != null ? note.trim() : "")
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                    return LocalDateTime.parse(dateStr.trim(), formatter);
                } else if (formatter == DateTimeFormatter.ISO_LOCAL_DATE ||
                        formatter.toString().contains("yyyy-MM-dd")) {
                    return LocalDate.parse(dateStr.trim(), formatter).atStartOfDay();
                } else {
                    return LocalDate.parse(dateStr.trim(), formatter).atStartOfDay();
                }
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new IllegalArgumentException("Invalid date format. Expected: YYYY-MM-DD or DD/MM/YYYY");
    }
}
