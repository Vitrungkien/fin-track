# Excel Import Feature for Transactions

## Overview
This feature allows users to import multiple transactions at once from an Excel file, making it easier to bulk-upload transaction data.

## Features Implemented

### 1. Template Download
- **Endpoint**: `GET /api/transactions/template`
- **Description**: Downloads an Excel template file with sample data
- **Template Format**:
  - Column 1: Date (YYYY-MM-DD)
  - Column 2: Type (INCOME/EXPENSE)
  - Column 3: Category Name
  - Column 4: Amount
  - Column 5: Note (optional)
- **Sample Data**: Includes 3 sample rows to demonstrate the format

### 2. Excel Import
- **Endpoint**: `POST /api/transactions/import`
- **Description**: Imports transactions from an uploaded Excel file
- **Validation**:
  - File must be .xlsx or .xls format
  - Date must be in valid format (supports multiple formats)
  - Type must be INCOME or EXPENSE
  - Category must exist for the user
  - Category type must match transaction type
  - Amount must be a positive number

### 3. Import Results
The import process returns detailed results:
- Total rows processed
- Number of successful imports
- Number of failed imports
- Detailed error messages for each failed row

## User Interface

### Buttons Added
1. **Download Template** - Downloads the Excel template
2. **Import Excel** - Opens the import modal

### Import Modal
- File upload input with validation
- Format instructions
- Progress indicator during import
- Results display with:
  - Success/warning/error status
  - Summary statistics
  - Detailed error list (if any)

## Technical Implementation

### Backend Components

#### 1. ImportResult DTO
```java
com.finance.tracker.dto.response.ImportResult
```
- Holds import statistics and results
- Contains list of errors and imported transactions

#### 2. TransactionService Methods
- `importFromExcel(MultipartFile file)` - Processes Excel import
- `generateTemplate()` - Creates Excel template
- Helper methods for parsing and validation

#### 3. TransactionController Endpoints
- `/api/transactions/import` - POST endpoint for import
- `/api/transactions/template` - GET endpoint for template download

#### 4. CategoryRepository Enhancement
- Added `findByUserAndName()` method for category lookup during import

### Frontend Components

#### 1. UI Updates (transactions.html)
- Added import and template download buttons
- Added import modal with three sections:
  - Upload section
  - Progress section
  - Results section

#### 2. JavaScript Functions (transactions.js)
- `downloadTemplate()` - Triggers template download
- `openImportModal()` - Opens import modal
- `importExcel()` - Handles file upload and import
- `displayImportResult()` - Shows import results

## Date Format Support
The import supports multiple date formats:
- ISO formats: `2026-02-11`, `2026-02-11T10:30:00`
- Standard formats: `2026-02-11 10:30:00`, `2026-02-11 10:30`
- Alternative formats: `11/02/2026`, `11-02-2026`

## Error Handling
- Row-level error tracking
- Continues processing even if some rows fail
- Provides specific error messages for each failure
- Transaction rollback for individual failed rows

## Usage Instructions

### For Users:
1. Click "Download Template" to get the Excel template
2. Fill in the template with your transaction data
3. Click "Import Excel" to open the import dialog
4. Select your filled Excel file
5. Click "Import" to process the file
6. Review the import results
7. Successfully imported transactions will appear in the transaction list

### Template Guidelines:
- Use the exact category names as they appear in your categories
- Ensure category type matches transaction type
- Use YYYY-MM-DD format for dates (recommended)
- Amounts should be positive numbers
- Notes are optional

## Dependencies
- Apache POI (already included in pom.xml version 5.2.5)
- Spring Boot Web (for file upload)
- Existing transaction and category services
